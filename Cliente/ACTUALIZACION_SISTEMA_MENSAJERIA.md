# 📡 Actualización del Sistema de Mensajería - Alineado con API del Servidor

**Fecha:** 17 de Octubre de 2025  
**Versión:** 2.0.0  
**Estado:** ✅ Completado

---

## 📋 Resumen de Cambios

Se ha actualizado completamente el sistema de mensajería del cliente para alinearlo con la API oficial del servidor. Ahora el sistema maneja correctamente tanto las respuestas a peticiones (pull) como las notificaciones push del servidor.

---

## 🔄 Archivos Modificados

### 1. **DTOMensaje.java** - DTO Completo de Mensajes
**Ubicación:** `Infraestructura/DTO/src/main/java/dto/vistaContactoChat/DTOMensaje.java`

**Cambios:**
- ✅ Agregados todos los campos que envía el servidor según la API:
  - `mensajeId` / `id` - UUID o ID numérico del mensaje
  - `remitenteId` / `destinatarioId` - UUIDs de usuarios
  - `remitenteNombre` / `destinatarioNombre` - Nombres completos
  - `contenido` - Texto del mensaje
  - `tipo` - "TEXTO", "IMAGEN", "AUDIO", "ARCHIVO", "VIDEO"
  - `fileId` / `fileName` - Para archivos adjuntos
  - `fechaEnvio` - Timestamp ISO 8601
  - `estado` - "ENVIADO", "ENTREGADO", "LEIDO"
  - `esMio` - Campo calculado para UI

- ✅ Métodos de utilidad agregados:
  - `getAutorConFecha()` - Formato "Autor - HH:MM" para compatibilidad
  - `tieneArchivo()` - Verifica si tiene archivo adjunto
  - `esTexto()`, `esAudio()`, `esImagen()`, `esArchivo()` - Verificación de tipo
  - `extraerHora()` - Extrae hora del timestamp ISO 8601

**Ejemplo de uso:**
```java
DTOMensaje mensaje = ... // del servidor
System.out.println(mensaje.getRemitenteNombre()); // "Juan Pérez"
System.out.println(mensaje.getTipo()); // "TEXTO"
System.out.println(mensaje.getAutorConFecha()); // "Juan Pérez - 10:35"
if (mensaje.tieneArchivo()) {
    descargarArchivo(mensaje.getFileId());
}
```

---

### 2. **DTOEnviarMensaje.java** - Actualizado con Tipos Correctos
**Ubicación:** `Infraestructura/DTO/src/main/java/dto/comunicacion/peticion/mensaje/DTOEnviarMensaje.java`

**Cambios:**
- ✅ Tipos actualizados para coincidir con la API: "TEXTO", "AUDIO", "IMAGEN", "ARCHIVO"
- ✅ Agregado campo `fileName` para todos los tipos de archivo
- ✅ Nuevos métodos de fábrica:
  - `deTexto()` - Para mensajes de texto
  - `deAudio()` - Para mensajes de audio (con fileName)
  - `deImagen()` - Para mensajes con imagen (nuevo)
  - `deArchivo()` - Para mensajes con archivo (nuevo)

**Ejemplo de uso:**
```java
// Mensaje de texto
DTOEnviarMensaje texto = DTOEnviarMensaje.deTexto(userId, contactoId, "Hola");

// Mensaje de audio
DTOEnviarMensaje audio = DTOEnviarMensaje.deAudio(userId, contactoId, fileId, "nota_voz.mp3");

// Mensaje con imagen
DTOEnviarMensaje imagen = DTOEnviarMensaje.deImagen(userId, contactoId, "Mira esto", fileId, "foto.jpg");

// Mensaje con archivo
DTOEnviarMensaje archivo = DTOEnviarMensaje.deArchivo(userId, contactoId, "Te envío el documento", fileId, "doc.pdf");
```

---

### 3. **DTOSolicitarHistorial.java** - Nuevo DTO
**Ubicación:** `Infraestructura/DTO/src/main/java/dto/comunicacion/peticion/mensaje/DTOSolicitarHistorial.java`

**Descripción:**
- DTO para la petición de historial con userId y contactoId
- Actualmente no se usa porque el servidor acepta solo el contactoId
- Se mantiene para futuras implementaciones

**Estructura:**
```java
{
  "userId": "user-uuid-123",
  "contactoId": "contact-uuid-456"
}
```

---

### 4. **DTOHistorialMensajes.java** - Nuevo DTO de Respuesta
**Ubicación:** `Infraestructura/DTO/src/main/java/dto/comunicacion/respuesta/DTOHistorialMensajes.java`

**Descripción:**
- DTO para parsear la respuesta completa del historial del servidor
- Incluye metadatos adicionales además de los mensajes

**Campos:**
- `mensajes` - Lista de DTOMensaje
- `tieneMas` - Indica si hay más mensajes disponibles
- `totalMensajes` - Total de mensajes en la conversación
- `contactoId` - UUID del contacto
- `contactoNombre` - Nombre del contacto

**Ejemplo de respuesta del servidor:**
```json
{
  "action": "solicitarHistorialPrivado",
  "status": "success",
  "data": {
    "mensajes": [...],
    "tieneMas": false,
    "totalMensajes": 15,
    "contactoId": "uuid-123",
    "contactoNombre": "María López"
  }
}
```

---

### 5. **GestionMensajesImpl.java** - Actualizado Completamente
**Ubicación:** `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/GestionMensajesImpl.java`

**Cambios Principales:**

#### A. Manejadores Registrados Correctamente
```java
// Respuestas a peticiones (PULL)
gestorRespuesta.registrarManejador("enviarMensajePrivado", this::manejarRespuestaEnvioMensaje);
gestorRespuesta.registrarManejador("solicitarHistorialPrivado", this::manejarHistorial);

// Notificaciones push del servidor (PUSH)
gestorRespuesta.registrarManejador("nuevoMensajeDirecto", this::manejarNuevoMensajePush);
```

#### B. Solicitar Historial
```java
@Override
public void solicitarHistorial(String contactoId) {
    String userId = gestorSesionUsuario.getUserId();
    // El servidor acepta solo el contactoId (obtiene userId de la sesión)
    DTORequest peticion = new DTORequest("solicitarHistorialPrivado", contactoId);
    enviadorPeticiones.enviar(peticion);
}
```

#### C. Envío de Mensajes con Tipos Correctos
- `enviarMensajeTexto()` - Tipo "TEXTO"
- `enviarMensajeAudio()` - Tipo "AUDIO" con fileName
- `enviarMensajeImagen()` - Tipo "IMAGEN" con fileName (nuevo)
- `enviarMensajeArchivo()` - Tipo "ARCHIVO" con fileName (nuevo)

#### D. Manejo de Respuesta de Envío
```java
private void manejarRespuestaEnvioMensaje(DTOResponse r) {
    // Confirma que el mensaje fue enviado exitosamente
    DTOMensaje mensaje = gson.fromJson(gson.toJson(r.getData()), DTOMensaje.class);
    mensaje.setEsMio(true);
    notificarObservadores("MENSAJE_ENVIADO_EXITOSO", mensaje);
}
```

#### E. Manejo de Notificaciones Push (NUEVO)
```java
private void manejarNuevoMensajePush(DTOResponse r) {
    // Se ejecuta cuando OTRO usuario nos envía un mensaje
    DTOMensaje mensaje = gson.fromJson(gson.toJson(r.getData()), DTOMensaje.class);
    
    // Determinar si es mío o del otro
    String myUserId = gestorSesionUsuario.getUserId();
    boolean esMio = mensaje.getRemitenteId().equals(myUserId);
    mensaje.setEsMio(esMio);
    
    notificarObservadores("NUEVO_MENSAJE_PRIVADO", mensaje);
}
```

#### F. Manejo de Historial Mejorado
```java
private void manejarHistorial(DTOResponse r) {
    try {
        // Intentar parsear estructura completa
        DTOHistorialMensajes historialCompleto = gson.fromJson(...);
        List<DTOMensaje> mensajes = historialCompleto.getMensajes();
        
        // Marcar cada mensaje como "mío" o "del otro"
        String myUserId = gestorSesionUsuario.getUserId();
        for (DTOMensaje msg : mensajes) {
            msg.setEsMio(msg.getRemitenteId().equals(myUserId));
        }
        
        notificarObservadores("HISTORIAL_MENSAJES", mensajes);
    } catch (Exception e) {
        // Fallback: intentar parsear como array directo
        ...
    }
}
```

---

## 🔔 Eventos de Notificación

El sistema ahora emite los siguientes eventos a los observadores:

### Eventos de Mensajes
| Evento | Cuándo se emite | Datos |
|--------|-----------------|-------|
| `MENSAJE_ENVIADO_EXITOSO` | Confirmación del servidor tras enviar | DTOMensaje |
| `ERROR_ENVIO_MENSAJE` | Error al enviar mensaje | String (mensaje error) |
| `NUEVO_MENSAJE_PRIVADO` | Notificación push de nuevo mensaje | DTOMensaje |
| `HISTORIAL_MENSAJES` | Respuesta de historial | List<DTOMensaje> |
| `ERROR_HISTORIAL` | Error al obtener historial | String (mensaje error) |

### Ejemplo de Observador
```java
@Override
public void actualizar(String tipo, Object datos) {
    switch (tipo) {
        case "NUEVO_MENSAJE_PRIVADO":
            DTOMensaje mensaje = (DTOMensaje) datos;
            if (esChatAbierto(mensaje.getRemitenteId())) {
                agregarMensajeAVista(mensaje);
            } else {
                mostrarNotificacion(mensaje);
            }
            break;
            
        case "MENSAJE_ENVIADO_EXITOSO":
            DTOMensaje miMensaje = (DTOMensaje) datos;
            agregarMensajeAVista(miMensaje);
            break;
            
        case "HISTORIAL_MENSAJES":
            List<DTOMensaje> mensajes = (List<DTOMensaje>) datos;
            cargarHistorialEnVista(mensajes);
            break;
    }
}
```

---

## 🔄 Flujos de Comunicación

### 1. Envío de Mensaje de Texto
```
Cliente                    Servidor
   |                          |
   |-- enviarMensajePrivado ->|
   |    {tipo: "TEXTO"}       |
   |                          |
   |<- respuesta confirmación-|
   |   MENSAJE_ENVIADO_EXITOSO|
   |                          |
                              |
                         Destinatario
                              |
                    <- nuevoMensajeDirecto (push)
                       NUEVO_MENSAJE_PRIVADO
```

### 2. Envío de Mensaje con Archivo
```
Cliente                    Servidor
   |                          |
   |-- startFileUpload ------>|
   |<- transferId, fileId ----|
   |                          |
   |-- uploadFileChunk ------>| (repetir N veces)
   |<- confirmación ----------|
   |                          |
   |-- endFileUpload -------->|
   |<- confirmación ----------|
   |                          |
   |-- enviarMensajePrivado ->|
   |    {tipo: "IMAGEN",      |
   |     fileId: "...",       |
   |     fileName: "..."}     |
   |                          |
   |<- respuesta confirmación-|
```

### 3. Solicitar Historial
```
Cliente                    Servidor
   |                          |
   |-- solicitarHistorialPrivado ->|
   |    {contactoId: "..."}   |
   |                          |
   |<- respuesta con historial|
   |   {mensajes: [...],      |
   |    tieneMas: false,      |
   |    totalMensajes: 15}    |
   |                          |
   HISTORIAL_MENSAJES (event)
```

---

## 📊 Comparación Antes vs Después

| Aspecto | Antes | Después |
|---------|-------|---------|
| **Tipos de mensaje** | "texto", "audio" | "TEXTO", "AUDIO", "IMAGEN", "ARCHIVO" |
| **Campos en DTOMensaje** | 3 campos básicos | 14 campos completos |
| **Notificaciones push** | No manejadas | ✅ Manejadas correctamente |
| **Confirmación de envío** | No diferenciada | ✅ Evento separado |
| **Historial** | Payload simple | Payload con userId y contactoId |
| **Metadatos de historial** | Solo mensajes | Mensajes + paginación + info contacto |
| **Archivos adjuntos** | Solo audio | Audio, imagen, archivo genérico |
| **Nombres en campos** | No disponibles | Remitente y destinatario |

---

## ✅ Ventajas de la Actualización

1. **🔄 Compatibilidad Total con Servidor**
   - Todos los campos del servidor son manejados
   - No se pierden datos en la comunicación

2. **🔔 Notificaciones Push Funcionales**
   - Los mensajes llegan en tiempo real
   - Se diferencian de las respuestas normales

3. **📎 Soporte Completo de Archivos**
   - Imágenes, audios, archivos genéricos
   - Metadata completa (nombre, tipo, fileId)

4. **👤 Información Completa de Usuarios**
   - Nombres de remitente y destinatario
   - IDs para operaciones

5. **📊 Mejor Manejo de Errores**
   - Eventos separados para errores
   - Logs detallados para debugging

6. **🎯 Código Más Mantenible**
   - DTOs claros y bien documentados
   - Separación clara entre push y pull

---

## 🚀 Próximos Pasos Recomendados

1. **Actualizar la UI del Chat**
   - Adaptar la vista para mostrar diferentes tipos de mensajes
   - Implementar badges para mensajes no leídos
   - Mostrar indicadores de estado (enviado, entregado, leído)

2. **Integrar con Sistema de Archivos**
   - Conectar `enviarMensajeImagen()` con el sistema de subida
   - Implementar descarga de archivos adjuntos
   - Vista previa de imágenes

3. **Mejorar Experiencia de Usuario**
   - Notificaciones de escritorio para mensajes nuevos
   - Sonidos de notificación
   - Contador de mensajes no leídos

4. **Implementar Paginación**
   - Usar el campo `tieneMas` del historial
   - Cargar más mensajes al hacer scroll

5. **Estados de Mensaje**
   - Implementar marcado como "leído"
   - Mostrar checks dobles en la UI

---

## 📝 Notas Técnicas

- El servidor puede enviar `id` (numérico) o `mensajeId` (string), el DTO maneja ambos
- El historial puede venir como array directo o estructura completa, hay fallback
- El campo `esMio` se calcula en el cliente comparando con el userId de la sesión
- Los timestamps están en formato ISO 8601: "2025-10-17T10:35:00"
- El servidor valida automáticamente el userId desde la sesión

---

## 🐛 Debugging

### Logs del Sistema

El sistema ahora tiene logs detallados:

```
🔧 [GestionMensajes]: Inicializando gestor de mensajes...
✅ [GestionMensajes]: Gestor inicializado con manejadores registrados
   → Respuestas: enviarMensajePrivado, solicitarHistorialPrivado
   → Push: nuevoMensajeDirecto

📤 [GestionMensajes]: Enviando mensaje de TEXTO
   → Remitente: user-uuid-123
   → Destinatario: user-uuid-456
   → Contenido: Hola

📥 [GestionMensajes]: Recibida RESPUESTA de envío de mensaje - Status: success
✅ [GestionMensajes]: Mensaje confirmado por servidor

🔔 [GestionMensajes]: Recibida NOTIFICACIÓN PUSH de nuevo mensaje - Status: success
✅ [GestionMensajes]: Nuevo mensaje recibido
   → De: Juan Pérez (user-uuid-456)
   → Tipo: TEXTO
   → Contenido: Hola a ti también
```

---

**Documentación creada por:** Sistema de Chat Unillanos  
**Última actualización:** 17 de Octubre de 2025

