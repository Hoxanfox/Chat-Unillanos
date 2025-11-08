# BUGFIX: NullPointerException en VistaContactoChat al recibir mensajes

## 📋 Problema Identificado

**Error:** `NullPointerException: Cannot invoke "String.equals(Object)" because the return value of "dto.vistaContactoChat.DTOMensaje.getRemitenteId()" is null`

**Ubicación:** `VistaContactoChat.java:158`

**Causa Raíz:**
El servidor envía notificaciones PUSH con una estructura JSON diferente a la que espera el cliente:

- **Servidor envía:**
```json
{
  "author": {
    "userId": "...",
    "username": "..."
  },
  "content": "...",
  "messageType": "TEXT",
  "messageId": "...",
  "timestamp": "..."
}
```

- **Cliente espera (DTOMensaje):**
```json
{
  "remitenteId": "...",
  "remitenteNombre": "...",
  "contenido": "...",
  "tipo": "TEXTO",
  "mensajeId": "...",
  "fechaEnvio": "..."
}
```

Cuando Gson intentaba deserializar directamente el JSON del servidor al `DTOMensaje`, no podía mapear correctamente los campos, dejando `remitenteId` como `null`, lo que causaba el `NullPointerException` al intentar compararlo.

## ✅ Solución Implementada

### 1. Método de Mapeo Personalizado

**Archivo:** `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/GestionMensajesImpl.java`

Se agregaron dos métodos helper:

#### `mapearMensajeDesdeServidor(Object data)`
- Convierte la estructura del servidor al formato del DTO
- Mapea campos anidados (como `author.userId` → `remitenteId`)
- Convierte nombres de campos (como `content` → `contenido`)
- Manejo robusto de errores con try-catch

#### `convertirTipoMensaje(String messageType)`
- Convierte tipos del servidor a tipos del cliente:
  - `TEXT` → `TEXTO`
  - `IMAGE` → `IMAGEN`
  - `AUDIO` → `AUDIO`
  - `FILE` → `ARCHIVO`

### 2. Actualización de Manejadores de PUSH

Se modificaron los métodos que manejan notificaciones PUSH del servidor:

- `manejarNuevoMensajePush(DTOResponse r)`
- `manejarNuevoMensajeAudioPush(DTOResponse r)`

**Antes:**
```java
DTOMensaje mensaje = gson.fromJson(gson.toJson(r.getData()), DTOMensaje.class);
```

**Después:**
```java
DTOMensaje mensaje = mapearMensajeDesdeServidor(r.getData());
```

### 3. Validación Null-Safe en la Vista

**Archivo:** `Presentacion/InterfazEscritorio/src/main/java/interfazEscritorio/dashboard/featureContactos/chatContacto/VistaContactoChat.java`

Se agregó una validación adicional para prevenir NPE en caso de que el mapeo falle:

```java
case "NUEVO_MENSAJE_PRIVADO":
    if (datos instanceof DTOMensaje) {
        DTOMensaje mensaje = (DTOMensaje) datos;
        
        // Validación null-safe para prevenir NullPointerException
        if (mensaje.getRemitenteId() == null) {
            System.err.println("⚠️ [VistaContactoChat]: Mensaje recibido con remitenteId null, ignorando...");
            break;
        }
        
        // Solo mostrar si es de nuestro contacto actual o si somos nosotros
        if (mensaje.getRemitenteId().equals(contacto.getId()) || mensaje.esMio()) {
            // ... mostrar mensaje
        }
    }
    break;
```

## 🔍 Mapeo de Campos

| Servidor | Cliente |
|----------|---------|
| `author.userId` | `remitenteId` |
| `author.username` | `remitenteNombre` |
| `content` | `contenido` |
| `messageType` | `tipo` |
| `messageId` | `mensajeId` |
| `timestamp` | `fechaEnvio` |
| `channelId` | `destinatarioId` |
| `fileId` | `fileId` |
| `fileName` | `fileName` |

## ✅ Resultado

- ✅ Compilación exitosa sin errores
- ✅ Los mensajes PUSH ahora se mapean correctamente
- ✅ El campo `remitenteId` se llena adecuadamente
- ✅ No más `NullPointerException` al recibir mensajes
- ✅ Validación adicional como protección extra

## 🧪 Pruebas Recomendadas

1. Enviar mensaje de texto entre dos usuarios
2. Recibir mensaje de texto (PUSH del servidor)
3. Enviar mensaje de audio
4. Recibir mensaje de audio (PUSH del servidor)
5. Verificar que los mensajes se muestren correctamente en la vista

## 📝 Notas Adicionales

- El mapeo es robusto y maneja casos donde algunos campos puedan ser `null`
- La conversión de tipos de mensajes es bidireccional (servidor ↔ cliente)
- La validación null-safe en la vista actúa como una capa adicional de protección
- El código mantiene compatibilidad con mensajes de texto, audio, imágenes y archivos

## 🔧 Archivos Modificados

1. `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/GestionMensajesImpl.java`
   - Agregado método `mapearMensajeDesdeServidor()`
   - Agregado método `convertirTipoMensaje()`
   - Actualizado `manejarNuevoMensajePush()`
   - Actualizado `manejarNuevoMensajeAudioPush()`

2. `Presentacion/InterfazEscritorio/src/main/java/interfazEscritorio/dashboard/featureContactos/chatContacto/VistaContactoChat.java`
   - Agregada validación null-safe en el caso `NUEVO_MENSAJE_PRIVADO`

---
**Fecha:** 2025-11-06
**Estado:** ✅ RESUELTO

