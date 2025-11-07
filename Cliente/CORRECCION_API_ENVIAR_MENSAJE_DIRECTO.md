# 🔧 Corrección de API - Mensajes Directos Privados

## 📋 Resumen
Se corrigió la implementación del cliente para alinearse correctamente con la API del servidor para el envío de mensajes directos (texto y audio) y la solicitud de historial privado, basándose en la documentación oficial.

## 🎯 Cambios Realizados

### 1. ✅ Corrección del Nombre de Acción (Texto)
**Archivo:** `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/GestionMensajesImpl.java`

**Problema:** El cliente enviaba la acción como `"enviarMensajeDirecto"` (camelCase)
**Solución:** Cambiado a `"enviarmensajedirecto"` (todo en minúsculas)

```java
// ANTES:
DTORequest peticion = new DTORequest("enviarMensajeDirecto", payload);

// DESPUÉS:
DTORequest peticion = new DTORequest("enviarmensajedirecto", payload);
```

**Razón:** Según la documentación del servidor, la acción debe ser `"enviarmensajedirecto"` sin camelCase.

---

### 2. ✅ Corrección del Nombre de Acción (Audio)
**Archivo:** `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/GestionMensajesImpl.java`

**Problema:** El cliente enviaba la acción como `"enviarMensajeDirectoAudio"` (camelCase)
**Solución:** Cambiado a `"enviarmensajedirectoaudio"` (todo en minúsculas)

```java
// ANTES:
DTORequest peticion = new DTORequest("enviarMensajeDirectoAudio", payload);

// DESPUÉS:
DTORequest peticion = new DTORequest("enviarmensajedirectoaudio", payload);
```

**Razón:** Según la documentación del servidor, la acción debe ser `"enviarmensajedirectoaudio"` sin camelCase.

---

### 3. ✅ Corrección del Nombre de Acción (Historial)
**Archivo:** `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/GestionMensajesImpl.java`

**Problema:** El cliente enviaba la acción como `"solicitarHistorialPrivado"` (camelCase)
**Solución:** Cambiado a `"solicitarhistorialprivado"` (todo en minúsculas)

```java
// ANTES:
DTORequest peticion = new DTORequest("solicitarHistorialPrivado", payload);

// DESPUÉS:
DTORequest peticion = new DTORequest("solicitarhistorialprivado", payload);
```

**Razón:** Según la documentación del servidor, la acción debe ser `"solicitarhistorialprivado"` sin camelCase.

---

### 4. ✅ Corrección de la Estructura del Payload de Audio
**Archivo:** `Infraestructura/DTO/src/main/java/dto/comunicacion/peticion/mensaje/DTOEnviarMensaje.java`

**Problema:** El método `deAudio()` usaba campos separados (`fileId`, `fileName`) pero el servidor espera la ruta en el campo `contenido`
**Solución:** Modificado el método `deAudio()` para usar el campo `contenido` con la ruta del archivo

```java
// ANTES:
public static DTOEnviarMensaje deAudio(..., String audioFileId, String fileName) {
    return new DTOEnviarMensaje(..., "AUDIO", null, audioFileId, fileName);
}

// DESPUÉS:
public static DTOEnviarMensaje deAudio(..., String audioFilePath) {
    return new DTOEnviarMensaje(..., "audio", audioFilePath, null, null);
}
```

**Razón:** Según la documentación de `enviarmensajedirectoaudio`, el servidor espera:
- El campo `"contenido"` con la ruta/URL del archivo (NO Base64)
- El campo `"tipo"` como `"audio"` (minúsculas)

---

### 5. ✅ Actualización del Método enviarMensajeAudio
**Archivo:** `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/GestionMensajesImpl.java`

**Cambios:**
- Usa `DTOEnviarMensaje.deAudio()` en lugar de `DTOEnviarMensajeAudio`
- Envía la acción correcta: `"enviarmensajedirectoaudio"`
- Pasa la ruta del archivo en el campo `contenido`

```java
// Crear payload con la ruta del archivo en 'contenido'
DTOEnviarMensaje payload = DTOEnviarMensaje.deAudio(
    peerRemitenteId,
    peerDestinoId,
    remitenteId,
    destinatarioId,
    audioFileId // Ruta/URL del archivo, NO Base64
);

// Enviar con la acción correcta
DTORequest peticion = new DTORequest("enviarmensajedirectoaudio", payload);
```

---

## 📡 Estructura de las Peticiones (Ahora Correcta)

### Para Mensaje de Texto
```json
{
  "action": "enviarmensajedirecto",
  "payload": {
    "remitenteId": "uuid-del-usuario-que-envia",
    "destinatarioId": "uuid-del-usuario-que-recibe",
    "contenido": "Este es el contenido del mensaje",
    "tipo": "TEXTO",
    "peerDestinoId": "peer-id-del-destinatario",
    "peerRemitenteId": "peer-id-del-remitente"
  }
}
```

### Para Mensaje de Audio
```json
{
  "action": "enviarmensajedirectoaudio",
  "payload": {
    "remitenteId": "uuid-del-usuario-que-envia",
    "destinatarioId": "uuid-del-usuario-que-recibe",
    "contenido": "ruta/al/archivo/guardado.webm",
    "tipo": "audio",
    "peerDestinoId": "peer-id-del-destinatario",
    "peerRemitenteId": "peer-id-del-remitente"
  }
}
```

### Para Solicitar Historial Privado
```json
{
  "action": "solicitarhistorialprivado",
  "payload": {
    "remitenteId": "uuid-del-usuario-que-pide-el-historial",
    "destinatarioId": "uuid-del-otro-usuario-en-el-chat",
    "peerRemitenteId": "peer-id-del-que-pide",
    "peerDestinatarioId": "peer-id-del-otro-usuario"
  }
}
```

⚠️ **IMPORTANTE:** 
- **Para mensajes de audio:** El campo `contenido` debe contener la **ruta/URL** del archivo, **NO datos Base64**
- **Para mensajes de audio:** El cliente debe **subir el archivo primero** a un servidor de archivos
- **Para historial:** Solo después de subir el archivo, se envía el mensaje con la ruta
- Los campos `peerRemitenteId` y `peerDestinatarioId` son **opcionales** (pueden ser `null`)

---

## 🔄 Flujo Completo del Servidor (Según Documentación)

### Para Mensajes de Texto (`enviarmensajedirecto`)
1. **Validar Payload:** Verifica que `remitenteId`, `destinatarioId` y `contenido` no estén vacíos
2. **Verificación de Seguridad:** Compara el `remitenteId` del payload con el `userId` de la sesión
3. **Obtener o Crear Canal Directo:** Llama a `chatFachada.crearCanalDirecto(remitenteId, destinatarioId)`
4. **Preparar Mensaje:** Crea un `SendMessageRequestDto` con el ID del canal
5. **Guardar y Enviar:** Llama a `chatFachada.enviarMensajeTexto`
6. **Confirmar al Remitente:** Envía respuesta JSON exitosa
7. **Notificación Push:** Si el destinatario está online, envía notificación

### Para Mensajes de Audio (`enviarmensajedirectoaudio`)
1. **Validar Payload:** Verifica que `remitenteId`, `destinatarioId` y `contenido` (ruta del audio) no estén vacíos
2. **Validar IDs:** Comprueba que sean UUIDs válidos
3. **Verificación de Seguridad:** Compara el `remitenteId` con el `userId` de la sesión
4. **Obtener o Crear Canal Directo:** Llama a `chatFachada.crearCanalDirecto(remitenteId, destinatarioId)`
5. **Preparar Mensaje:** Asigna `audioFilePath = contenido` y crea `SendMessageRequestDto` con tipo "AUDIO"
6. **Guardar y Enviar:** Llama a `chatFachada.enviarMensajeAudio(sendAudioDto, remitenteId)`
7. **Confirmar al Remitente:** Envía respuesta JSON exitosa
8. **Notificación Push:** Si el destinatario está online, envía notificación

### Para Solicitar Historial (`solicitarhistorialprivado`)
1. **Validar Payload:** Verifica que `remitenteId` y `destinatarioId` no estén vacíos
2. **Obtener o Crear Canal:** Busca o crea el canal privado entre los dos usuarios
3. **Recuperar Mensajes:** Obtiene todos los mensajes del canal de la base de datos
4. **Mapear Respuesta:** Convierte los mensajes a la estructura esperada por el cliente
5. **Enviar Historial:** Devuelve el array de mensajes al solicitante

---

## ✅ Respuestas del Servidor

### Respuesta Exitosa (Texto)
```json
{
  "action": "enviarMensajeDirecto",
  "status": true,
  "message": "Mensaje enviado",
  "data": {
    "mensajeId": "uuid-del-nuevo-mensaje-creado",
    "fechaEnvio": "2025-11-06T22:10:01.123Z"
  }
}
```

### Respuesta Exitosa (Audio)
```json
{
  "action": "enviarMensajeDirectoAudio",
  "status": true,
  "message": "Mensaje de audio enviado",
  "data": {
    "mensajeId": "uuid-del-nuevo-mensaje-de-audio",
    "fechaEnvio": "2025-11-06T22:15:01.456Z"
  }
}
```

### Respuesta Exitosa (Historial)
```json
{
  "action": "solicitarHistorialPrivado",
  "status": true,
  "message": "Historial privado obtenido exitosamente",
  "data": [
    {
      "mensajeId": "uuid-del-mensaje-1",
      "remitenteId": "uuid-del-usuario-que-pide-el-historial",
      "destinatarioId": "uuid-del-otro-usuario-en-el-chat",
      "peerRemitenteId": "peer-id-del-autor-del-mensaje-1",
      "peerDestinoId": null,
      "tipo": "texto",
      "contenido": "Hola, este es el primer mensaje",
      "fechaEnvio": "2025-11-06T20:01:00Z"
    },
    {
      "mensajeId": "uuid-del-mensaje-2",
      "remitenteId": "uuid-del-otro-usuario-en-el-chat",
      "destinatarioId": "uuid-del-usuario-que-pide-el-historial",
      "peerRemitenteId": "peer-id-del-autor-del-mensaje-2",
      "peerDestinoId": null,
      "tipo": "audio",
      "contenido": "ruta/al/audio/respuesta.webm",
      "fechaEnvio": "2025-11-06T20:01:30Z"
    }
  ]
}
```

### Respuesta de Error - Destinatario no encontrado
```json
{
  "action": "enviarMensajeDirecto",
  "status": false,
  "message": "Destinatario no encontrado o desconectado",
  "data": null
}
```

### Respuesta de Error - Validación (Audio)
```json
{
  "action": "enviarMensajeDirectoAudio",
  "status": false,
  "message": "Datos de mensaje inválidos",
  "data": {
    "campo": "contenido",
    "error": "El enlace del archivo de audio es requerido"
  }
}
```

### Respuesta de Error - Seguridad (Historial)
```json
{
  "action": "solicitarHistorialPrivado",
  "status": false,
  "message": "Error al obtener el historial: Usuario no autorizado",
  "data": null
}
```

---

## 🔔 Notificación Push al Destinatario

### Push de Mensaje de Texto
Cuando el servidor recibe un mensaje de texto, si el destinatario está **online**, envía:

```json
{
  "action": "nuevoMensajeDirecto",
  "status": true,
  "message": "Nuevo mensaje recibido",
  "data": {
    "messageId": "uuid-mensaje",
    "timestamp": "2025-11-06T22:10:01.123Z",
    "author": {
      "userId": "uuid-remitente",
      "username": "NombreRemitente"
    },
    "content": "Contenido del mensaje",
    "messageType": "TEXT",
    "channelId": "uuid-canal-privado"
  }
}
```

### Push de Mensaje de Audio
Cuando el servidor recibe un mensaje de audio, si el destinatario está **online**, envía:

```json
{
  "action": "nuevoMensajeDirectoAudio",
  "status": true,
  "message": "Nuevo mensaje de audio recibido",
  "data": {
    "messageId": "uuid-mensaje",
    "timestamp": "2025-11-06T22:15:01.456Z",
    "author": {
      "userId": "uuid-remitente",
      "username": "NombreRemitente"
    },
    "content": "ruta/al/archivo/guardado.webm",
    "messageType": "AUDIO",
    "channelId": "uuid-canal-privado"
  }
}
```

**Nota:** Si el destinatario está **offline**, no se envía notificación push. El usuario verá los mensajes cuando inicie sesión y solicite el historial con `solicitarhistorialprivado`.

---

## 🧪 Pruebas Recomendadas

1. **Enviar mensaje de texto:** Verificar que la acción sea `"enviarmensajedirecto"`
2. **Enviar mensaje de audio:** 
   - Verificar que la acción sea `"enviarmensajedirectoaudio"`
   - Verificar que el campo `contenido` contenga la ruta del archivo
   - El archivo debe estar subido ANTES de enviar el mensaje
3. **Solicitar historial:**
   - Verificar que la acción sea `"solicitarhistorialprivado"`
   - Verificar que se reciba el array completo de mensajes
   - Verificar que el campo `tipo` de cada mensaje sea "texto" o "audio" (minúsculas)
4. **Recepción de push:** Verificar que los mensajes entrantes se manejen correctamente
5. **Manejo de errores:** Probar con destinatario offline, datos inválidos, etc.

---

## 📝 Notas Importantes

- ✅ Los campos `peerDestinoId` y `peerRemitenteId` son **opcionales** (pueden ser `null`)
- ✅ El servidor ignora el campo `fechaEnvio` del cliente y usa su propia fecha
- ✅ El servidor se encarga de la lógica "get-or-create" del canal privado
- ✅ La verificación de seguridad impide que un usuario envíe mensajes en nombre de otro
- ✅ El manejador de respuestas está registrado como `"enviarMensajeDirecto"`, `"enviarMensajeDirectoAudio"` y `"solicitarHistorialPrivado"` (con mayúsculas) pero las acciones de envío deben ser en minúsculas
- ⚠️ **Para audio:** El campo `contenido` debe ser una **ruta/URL**, **NO Base64**
- ⚠️ **Para audio:** El cliente debe subir el archivo primero y luego enviar el mensaje con la ruta
- ⚠️ **Para historial:** No hay notificación push, es una petición-respuesta simple
- ⚠️ **Para historial:** El servidor transforma "TEXT" en "texto" y "AUDIO" en "audio" en la respuesta

---

## 🔄 Diferencias Clave entre las Acciones

| Aspecto | Mensaje de Texto | Mensaje de Audio | Solicitar Historial |
|---------|------------------|------------------|---------------------|
| **Acción** | `enviarmensajedirecto` | `enviarmensajedirectoaudio` | `solicitarhistorialprivado` |
| **Campo contenido** | Texto del mensaje | Ruta/URL del archivo | N/A |
| **Campo tipo** | `"TEXTO"` | `"audio"` | N/A (viene en respuesta) |
| **Formato de contenido** | String normal | Ruta/URL (NO Base64) | N/A |
| **Prerequisito** | Ninguno | Archivo debe estar subido | Ninguno |
| **Respuesta** | Confirmación con ID | Confirmación con ID | Array de mensajes |
| **Push** | Sí (si destinatario online) | Sí (si destinatario online) | No hay push |

---

## 📅 Fecha de Corrección
7 de noviembre de 2025

## 👤 Estado
✅ **Completado y Alineado con la API del Servidor**

---

## 📌 Resumen de Todas las Correcciones

### Acciones Corregidas (3)
1. ✅ `enviarmensajedirecto` (antes: `enviarMensajeDirecto`)
2. ✅ `enviarmensajedirectoaudio` (antes: `enviarMensajeDirectoAudio`)
3. ✅ `solicitarhistorialprivado` (antes: `solicitarHistorialPrivado`)

### Cambios en DTOs (1)
1. ✅ `DTOEnviarMensaje.deAudio()` ahora usa el campo `contenido` con la ruta del archivo

### Archivos Modificados (2)
1. ✅ `GestionMensajesImpl.java` - 3 cambios de nombres de acciones
2. ✅ `DTOEnviarMensaje.java` - 1 cambio en el método `deAudio()`

### Resultado Final
✅ **Todas las acciones de mensajes directos están alineadas con la API del servidor**
