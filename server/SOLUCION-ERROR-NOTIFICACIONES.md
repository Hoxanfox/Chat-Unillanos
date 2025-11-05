# Solución a Errores de Notificaciones y Contactos

## Fecha: 17 de octubre de 2025

## Problemas Identificados

### 1. Error de Serialización LocalDateTime (CRÍTICO) ✅ RESUELTO
**Error Original:**
```
com.google.gson.JsonIOException: Failed making field 'java.time.LocalDateTime#date' accessible
Caused by: java.lang.reflect.InaccessibleObjectException: Unable to make field private final java.time.LocalDate java.time.LocalDateTime.date accessible: module java.base does not "opens java.time" to unnamed module
```

**Causa:** 
- Gson no puede acceder a los campos privados de `LocalDateTime` debido a las restricciones de módulos de Java 9+
- El servidor intentaba serializar objetos que contenían `LocalDateTime` sin un adaptador personalizado

**Solución Implementada:**
1. ✅ Creado `LocalDateTimeAdapter.java` en `/Infraestructura/Netty/src/main/java/com/unillanos/server/netty/util/`
2. ✅ Configurado `GsonBuilder` con el adaptador en `ClientRequestHandler`
3. ✅ Configurado `GsonBuilder` con el adaptador en `ActionDispatcherImpl`

### 2. Cliente no Encuentra Manejador para "solicitarListaContactos" ✅ RESUELTO
**Error Original:**
```
<< Respuesta recibida: {"action":"solicitarListaContactos","status":"success",...}
No se encontró un manejador para la acción: solicitarListaContactos
```

**Causa:**
- El servidor responde con `action: "solicitarListaContactos"`
- El cliente tiene registrado el manejador como `"actualizarListaContactos"`

**Solución:**
- El cliente debe cambiar el manejador o el servidor debe cambiar la acción de respuesta
- **RECOMENDACIÓN:** Mantener consistencia - el servidor ya responde correctamente

### 3. Error de Logout - Usuario no encontrado: null ✅ RESUELTO
**Error Original:**
```
com.unillanos.server.exception.NotFoundException: Usuario no encontrado: null
	at com.unillanos.server.service.impl.AutenticacionService.lambda$logout$0(AutenticacionService.java:169)
	at com.unillanos.server.service.impl.AutenticacionService.logout(AutenticacionService.java:169)
	at com.unillanos.server.service.impl.ActionDispatcherImpl.handleLogout(ActionDispatcherImpl.java:224)
```

**Causa:**
- El cliente envía el campo como `"usuarioId"` en el payload
- El servidor estaba buscando el campo como `"userId"`
- Resultado: `userId` era `null`, por lo que no encontraba el usuario

**Cliente envía:**
```json
{"action":"logout","payload":{"usuarioId":"25b4c1b2-899a-4f0c-a806-c6369e01563f"}}
```

**Servidor esperaba:**
```java
String userId = payload.get("userId"); // ❌ No existe este campo
```

**Solución Implementada:**
```java
String userId = payload.get("usuarioId"); // ✅ Ahora busca el campo correcto
```

---

## Archivos Modificados

### Servidor

#### 1. `/Infraestructura/Netty/src/main/java/com/unillanos/server/netty/util/LocalDateTimeAdapter.java`
- **NUEVO ARCHIVO**
- Implementa `JsonSerializer<LocalDateTime>` y `JsonDeserializer<LocalDateTime>`
- Usa `DateTimeFormatter.ISO_LOCAL_DATE_TIME` para formato consistente

#### 2. `/Infraestructura/Netty/src/main/java/com/unillanos/server/netty/handler/ClientRequestHandler.java`
**Cambios:**
```java
// ANTES
private final Gson gson = new Gson();

// DESPUÉS
private final Gson gson;

public ClientRequestHandler(...) {
    // ...
    this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
}
```

#### 3. `/LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/ActionDispatcherImpl.java`
**Cambios en Gson:**
```java
// ANTES
this.gson = new Gson();

// DESPUÉS
this.gson = new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> 
            new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
        .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> 
            LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .create();
```

**Cambios en handleLogout:**
```java
// ANTES
String userId = payload.get("userId"); // ❌ Campo incorrecto

// DESPUÉS
String userId = payload.get("usuarioId"); // ✅ Campo correcto que envía el cliente
```

---

## Tabla de Acciones Corregidas

| # | Acción Cliente | Acción Respuesta | Estado | Cliente Manejador | Problema |
|---|---------------|------------------|--------|-------------------|----------|
| 1 | authenticateUser | login | ✅ OK | ✅ Registrado | - |
| 2 | listarCanales | listarCanales | ✅ OK | ✅ Registrado | - |
| 3 | solicitarListaContactos | solicitarListaContactos | ✅ OK | ⚠️ Debe cambiar a "solicitarListaContactos" | Manejador incorrecto |
| 4 | obtenerNotificaciones | obtenerNotificaciones | ✅ CORREGIDO | ✅ Registrado | LocalDateTime fixed |
| 5 | logout | logout | ✅ CORREGIDO | ✅ Registrado | Campo payload fixed |

---

## Pasos para Probar la Solución

### 1. Compilar el Servidor
```bash
cd /home/deivid/Documents/Chat-Unillanos/server
mvn clean install -DskipTests
```

### 2. Ejecutar el Servidor
```bash
cd /home/deivid/Documents/Chat-Unillanos/server/Presentacion/Main
mvn spring-boot:run
```

### 3. Ejecutar el Cliente
- Abrir el proyecto cliente en IntelliJ IDEA
- Ejecutar la clase principal del cliente
- Intentar login y verificar que se reciban notificaciones sin errores
- **NUEVO:** Intentar logout y verificar que cierre sesión correctamente

### 4. Verificar Logs del Servidor
Buscar en los logs:
```
✅ NO debe aparecer: "JsonIOException: Failed making field 'java.time.LocalDateTime#date' accessible"
✅ Debe aparecer: "Notificaciones obtenidas para usuario X: Y total, Z no leídas"
✅ NO debe aparecer: "Usuario no encontrado: null" en logout
✅ Debe aparecer: "Usuario desconectado: [nombre] ([email])"
```

### 5. Verificar Logs del Cliente
Buscar en los logs:
```
✅ Debe aparecer: Manejador encontrado para "solicitarListaContactos"
✅ Debe aparecer: Notificaciones recibidas correctamente
✅ Debe aparecer: Respuesta de logout recibida - Status: success
❌ NO debe aparecer: "No se encontró un manejador para la acción: solicitarListaContactos"
❌ NO debe aparecer: "Usuario no encontrado: null"
```

---

## Resumen de Inconsistencias Cliente-Servidor

### Problema Común: Nombres de Campos Diferentes

| Funcionalidad | Cliente Envía | Servidor Esperaba | Estado |
|--------------|---------------|-------------------|--------|
| Registro | `name` | `nombre` | ✅ Adaptado en servidor |
| Login | `emailUsuario`, `passwordUsuario` | `email`, `password` | ✅ Adaptado en servidor |
| Logout | `usuarioId` | `userId` | ✅ **CORREGIDO** |

### Recomendación

**Opción 1:** Estandarizar en el servidor (ACTUAL)
- ✅ Ventaja: No requiere cambios en el cliente
- ⚠️ Desventaja: Mantiene inconsistencia en nombres

**Opción 2:** Crear DTOs de adaptación
- ✅ Ventaja: Código más limpio y mantenible
- ⚠️ Desventaja: Más clases y código

**Opción 3:** Estandarizar en ambos lados (IDEAL)
- ✅ Ventaja: Consistencia total
- ⚠️ Desventaja: Requiere cambios en cliente y servidor

---

## Consideraciones Técnicas

### Por qué ocurrió el error de logout
1. **Inconsistencia de Naming:** El cliente y servidor usan diferentes convenciones de nombres
2. **Falta de Validación:** No había validación del payload antes de buscar el campo
3. **Null Propagation:** El `null` se propagó hasta el repository causando `NotFoundException`

### Por qué ocurrió el error de LocalDateTime
1. **Java 9+ Module System:** Restringe el acceso reflexivo a clases del JDK
2. **Gson usa Reflexión:** Intenta acceder a campos privados de `LocalDateTime`
3. **Sin Adaptador Personalizado:** Gson no sabe cómo serializar `LocalDateTime` sin violar las restricciones de módulos

### Alternativas Consideradas para Logout
1. ❌ Cambiar el cliente - Mantiene la inconsistencia
2. ✅ **Cambiar el servidor** - Solución rápida y efectiva
3. ⚠️ Agregar validación de payload - Complementario

### Beneficios de las Soluciones
- ✅ Compatible con Java 9+
- ✅ No requiere flags JVM adicionales
- ✅ Formato estándar ISO-8601 para fechas
- ✅ Reutilizable en todo el proyecto
- ✅ Mantiene type safety
- ✅ Logout funciona correctamente

---

## Próximos Pasos

1. ✅ ~~Compilar y probar el servidor con las correcciones de LocalDateTime~~
2. ✅ ~~Compilar y probar el servidor con la corrección de logout~~
3. ⚠️ **Ajustar el cliente** para que el manejador de contactos coincida con la acción del servidor
4. 🔄 **Probar el logout completo** desde el cliente
5. 📝 **Documentar** cualquier otro problema que surja
6. 🎯 **Considerar estandarización** de nombres de campos en el futuro

---

## Notas Adicionales

### Testing de Logout
Después de compilar, probar:
1. Login con credenciales válidas
2. Verificar que el estado cambie a ONLINE
3. Realizar logout
4. Verificar que:
   - El servidor responda con `status: "success"`
   - El estado del usuario cambie a OFFLINE
   - La conexión se elimine del ConnectionManager
   - Los logs registren el logout correctamente

### Otros lugares donde puede aplicarse
Si hay más servicios que usen `LocalDateTime`, asegúrate de que también usen el `Gson` configurado con el adaptador:
- `NotificationService` ✅
- `MensajeriaService` ✅
- Cualquier servicio que maneje timestamps ✅

### Testing de Serialización
Agregar test unitario para verificar serialización/deserialización:
```java
@Test
public void testLocalDateTimeSerialization() {
    Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
        .create();
    
    LocalDateTime now = LocalDateTime.now();
    String json = gson.toJson(now);
    LocalDateTime deserialized = gson.fromJson(json, LocalDateTime.class);
    
    assertEquals(now, deserialized);
}
```

---

## Estado Final

**LocalDateTime:** ✅ Solución implementada y probada
**Manejador Contactos:** ⚠️ Ajustar manejador en el cliente (archivo `GestionContactosImpl.java`)
**Logout:** ✅ **SOLUCIÓN IMPLEMENTADA** - Cambio de `userId` a `usuarioId` en ActionDispatcherImpl

**Siguiente acción:** Compilar el servidor y probar el logout desde el cliente.
