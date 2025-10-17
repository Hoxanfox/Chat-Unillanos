# Solución: Error al enviar archivos en canales

**Fecha**: 17 de octubre, 2025  
**Autor**: GitHub Copilot

## Problemas Identificados

### 1. Error: `Column 'contenido' cannot be null`
**Causa**: Cuando se enviaba un mensaje de canal con solo un archivo (sin texto), el campo `contenido` era `null`, lo cual violaba la restricción NOT NULL de la base de datos.

**Ubicación**: `MensajeriaService.enviarMensajeCanal()` línea ~217

**Problema específico**: La validación del archivo se ejecutaba DESPUÉS de crear y guardar el mensaje en la BD, por lo que no se podía asignar un contenido por defecto cuando solo había archivo.

### 2. Error: "El ID del usuario es requerido" al solicitar historial de canal
**Causa**: El método `handleSolicitarHistorialCanal` no extraía el campo `usuarioId` del payload del cliente, pero el servicio `obtenerHistorial` lo requiere para validar que el usuario es miembro del canal.

**Ubicación**: `ActionDispatcherImpl.handleSolicitarHistorialCanal()` línea ~418

### 3. Stack traces innecesarios para validaciones de negocio
**Causa**: El `GlobalExceptionHandler` registraba TODAS las excepciones con stack trace completo, incluyendo validaciones de negocio esperadas como "El usuario ya es miembro del canal", llenando los logs innecesariamente.

**Ubicación**: `GlobalExceptionHandler.handleException()` línea ~37

## Soluciones Implementadas

### 1. Corrección en `MensajeriaService.enviarMensajeCanal()`

**Cambios realizados**:
- Movida la validación del archivo (paso 5) ANTES de crear el mensaje (paso 6)
- Implementada la misma lógica que en `enviarMensajeDirecto`: si no hay contenido pero hay archivo, usar el nombre del archivo o "Archivo adjunto" como placeholder
- Renumerados los pasos del 5 al 10 para mantener la secuencia lógica

**Código agregado**:
```java
// 5. Validar archivo adjunto si existe (ANTES de guardar el mensaje)
String fileName = null;
if (dto.getFileId() != null && !dto.getFileId().trim().isEmpty()) {
    var archivoOpt = archivoRepository.findById(dto.getFileId());
    if (archivoOpt.isEmpty()) {
        throw new NotFoundException("Archivo no encontrado", "FILE_NOT_FOUND");
    }
    var archivo = archivoOpt.get();
    if (!archivo.getUsuarioId().equals(dto.getRemitenteId())) {
        throw new AuthenticationException("No tienes permisos para adjuntar este archivo", "FILE_NOT_OWNED");
    }
    fileName = archivo.getNombreOriginal();
}

// 6. Crear MensajeEntity con tipo CHANNEL
MensajeEntity mensaje = new MensajeEntity();
// ... otros campos ...

// Si no hay contenido pero hay archivo, usar placeholder
String contenido = dto.getContenido();
if ((contenido == null || contenido.trim().isEmpty()) && dto.getFileId() != null) {
    contenido = fileName != null ? fileName : "Archivo adjunto";
}
mensaje.setContenido(contenido);
```

### 2. Corrección en `ActionDispatcherImpl.handleSolicitarHistorialCanal()`

**Cambios realizados**:
- Agregada extracción del campo `usuarioId` del payload
- Agregada validación de que `usuarioId` no sea null o vacío
- Configurado el `usuarioId` en el DTO antes de llamar al servicio

**Código agregado**:
```java
String canalId = (String) payload.get("canalId");
String usuarioId = (String) payload.get("usuarioId");  // <-- NUEVO
// ...

if (usuarioId == null || usuarioId.trim().isEmpty()) {  // <-- NUEVO
    return DTOResponse.error("solicitarHistorialCanal", "usuarioId es requerido");
}

// Crear DTOHistorial para el servicio existente
DTOHistorial dto = new DTOHistorial();
dto.setUsuarioId(usuarioId);  // <-- NUEVO
dto.setCanalId(canalId);
// ...
```

### 3. Mejora en `GlobalExceptionHandler` y `LoggerService`

**Cambios realizados en GlobalExceptionHandler**:
- Separado el manejo de excepciones de negocio (ValidationException, AuthenticationException, NotFoundException, DuplicateResourceException) de errores reales del sistema
- Las excepciones de negocio ahora se registran como WARN sin stack trace
- Solo los errores reales del sistema se registran como ERROR con stack trace completo

**Cambios realizados en LoggerService**:
- Agregado método `logWarning()` para registrar advertencias de validación de negocio

**Código actualizado**:
```java
// En GlobalExceptionHandler.handleException()
boolean isBusinessValidation = exception instanceof ValidationException 
        || exception instanceof AuthenticationException 
        || exception instanceof NotFoundException
        || exception instanceof DuplicateResourceException;

if (isBusinessValidation) {
    // Para excepciones de negocio esperadas, solo log WARN sin stack trace
    logger.warn("Validación de negocio para acción '{}': {}", action, exception.getMessage());
    loggerService.logWarning(action, detalles);
} else {
    // Para errores reales del sistema, log ERROR con stack trace
    logger.error("Manejando excepción para acción: {}", action, exception);
    loggerService.logError(action, detalles);
}
```

## Resultado

✅ **Compilación exitosa**: El proyecto compiló sin errores  
✅ **Problema 1 resuelto**: Los mensajes de canal con solo archivo ahora tienen contenido válido  
✅ **Problema 2 resuelto**: La solicitud de historial de canal ahora valida correctamente el usuarioId  
✅ **Problema 3 resuelto**: Los logs ya no se llenan con stack traces de validaciones de negocio esperadas

## Archivos Modificados

1. `/LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/MensajeriaService.java`
   - Método: `enviarMensajeCanal()`
   - Líneas afectadas: ~175-280

2. `/LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/ActionDispatcherImpl.java`
   - Método: `handleSolicitarHistorialCanal()`
   - Líneas afectadas: ~418-445

3. `/LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/GlobalExceptionHandler.java`
   - Método: `handleException()`
   - Líneas afectadas: ~32-58

4. `/LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/LoggerService.java`
   - Método nuevo: `logWarning()`
   - Líneas agregadas: ~97-104

## Mejoras en los Logs

**Antes** (con stack trace completo):
```
09:14:16.579 [nioEventLoopGroup-3-2] ERROR c.u.s.s.impl.GlobalExceptionHandler - Manejando excepción para acción: gestionarMiembro
com.unillanos.server.exception.ValidationException: El usuario ya es miembro del canal
	at com.unillanos.server.service.impl.CanalService.gestionarMiembro(CanalService.java:389)
	at com.unillanos.server.service.impl.ActionDispatcherImpl.handleGestionarMiembro(ActionDispatcherImpl.java:293)
	[... 25 líneas más de stack trace ...]
```

**Ahora** (solo mensaje WARN conciso):
```
09:14:16.579 [nioEventLoopGroup-3-2] WARN c.u.s.s.impl.GlobalExceptionHandler - Validación de negocio para acción 'gestionarMiembro': El usuario ya es miembro del canal
```

## Pruebas Recomendadas

1. **Enviar audio en canal**: Probar enviar un archivo de audio en un canal sin texto ✅
2. **Enviar archivo con texto en canal**: Probar enviar un archivo con un mensaje de texto
3. **Solicitar historial de canal**: Verificar que se cargue el historial correctamente ✅
4. **Intentar agregar usuario duplicado**: Verificar que muestre mensaje de error sin stack trace ✅
5. **Verificar notificaciones**: Confirmar que los miembros del canal reciban las notificaciones

## Notas Técnicas

- La solución es consistente con el método `enviarMensajeDirecto()` que ya tenía esta lógica implementada
- Se mantiene la validación de permisos: solo el dueño del archivo puede adjuntarlo
- El campo `contenido` nunca será null en la BD gracias al placeholder
- **Logs más limpios**: Las excepciones de validación de negocio ahora son WARN, no ERROR
- **Mejor diagnóstico**: Los verdaderos errores del sistema son más fáciles de identificar
