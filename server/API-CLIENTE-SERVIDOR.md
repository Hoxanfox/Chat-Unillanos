# 📡 API Cliente-Servidor - Chat Unillanos

**Versión:** 1.0.0  
**Fecha:** 17 de Octubre de 2025  
**Protocolo:** JSON sobre TCP (Netty)

---

## 📋 Índice

1. [Historial de Chat Privado](#1-historial-de-chat-privado)
2. [Historial de Canal](#2-historial-de-canal)
3. [Enviar Mensaje de Texto](#3-enviar-mensaje-de-texto)
4. [Enviar Mensaje con Imagen](#4-enviar-mensaje-con-imagen)
5. [Enviar Mensaje de Audio](#5-enviar-mensaje-de-audio)
6. [Enviar Mensaje con Archivo](#6-enviar-mensaje-con-archivo)
7. [Descargar Archivo/Audio/Imagen](#7-descargar-archivoaudioimagen)
8. [Notificaciones Push](#8-notificaciones-push)
9. [Tipos de Mensaje Soportados](#9-tipos-de-mensaje-soportados)
10. [Estados de Mensaje](#10-estados-de-mensaje)

---

## 1. 📖 Historial de Chat Privado

### Solicitar Historial

**Cliente → Servidor**
```json
{
  "action": "solicitarHistorialPrivado",
  "payload": {
    "contactoId": "f8e3d4a1-5b7c-4e9d-8a2f-1c3b5d7e9f0a"
  }
}
```

**Campos:**
- `action`: `"solicitarHistorialPrivado"`
- `payload.contactoId`: UUID del contacto con quien tiene la conversación (String) - **REQUERIDO**
- `payload.usuarioId`: UUID del usuario autenticado (String) - **OPCIONAL** (el servidor lo obtiene automáticamente de la sesión si no se envía)

**Nota:** El servidor obtiene automáticamente el `usuarioId` de la sesión del usuario autenticado. Si se envía en el payload, el servidor valida que coincida con la sesión actual para seguridad adicional.

---

### Respuesta del Servidor

**Servidor → Cliente**
```json
{
  "action": "solicitarHistorialPrivado",
  "status": "success",
  "message": "Historial obtenido: 15 mensajes",
  "data": {
    "mensajes": [
      {
        "mensajeId": "msg-uuid-1",
        "remitenteId": "user-uuid-1",
        "destinatarioId": "user-uuid-2",
        "remitenteNombre": "Juan Pérez",
        "destinatarioNombre": "María López",
        "contenido": "Hola, ¿cómo estás?",
        "tipo": "TEXTO",
        "fileId": null,
        "fechaEnvio": "2025-10-17T10:30:45",
        "fileName": null
      },
      {
        "mensajeId": "msg-uuid-2",
        "remitenteId": "user-uuid-2",
        "destinatarioId": "user-uuid-1",
        "remitenteNombre": "María López",
        "destinatarioNombre": "Juan Pérez",
        "contenido": "Mira esta foto",
        "tipo": "IMAGEN",
        "fileId": "file-uuid-123",
        "fechaEnvio": "2025-10-17T10:31:20",
        "fileName": "foto.jpg"
      },
      {
        "mensajeId": "msg-uuid-3",
        "remitenteId": "user-uuid-1",
        "destinatarioId": "user-uuid-2",
        "remitenteNombre": "Juan Pérez",
        "destinatarioNombre": "María López",
        "contenido": "Escucha esto",
        "tipo": "AUDIO",
        "fileId": "file-uuid-456",
        "fechaEnvio": "2025-10-17T10:35:00",
        "fileName": "nota_voz.mp3"
      }
    ],
    "tieneMas": false,
    "totalMensajes": 15,
    "contactoId": "25b4c1b2-899a-4f0c-a806-c6369e01563f",
    "contactoNombre": "Contacto"
  }
}
```

**Campos de cada mensaje:**
- `mensajeId`: UUID del mensaje
- `remitenteId`: UUID del remitente
- `destinatarioId`: UUID del destinatario
- `remitenteNombre`: Nombre del remitente
- `destinatarioNombre`: Nombre del destinatario
- `contenido`: Texto del mensaje
- `tipo`: `"TEXTO"`, `"IMAGEN"`, `"AUDIO"`, `"ARCHIVO"`, `"VIDEO"`
- `fileId`: UUID del archivo (null si es texto)
- `fechaEnvio`: ISO 8601 timestamp
- `fileName`: Nombre del archivo (null si es texto)

---

## 2. 📺 Historial de Canal

### Solicitar Historial

**Cliente → Servidor**
```json
{
  "action": "solicitarHistorialCanal",
  "payload": {
    "canalId": "canal-uuid-123",
    "limite": 50
  }
}
```

**Campos:**
- `action`: `"solicitarHistorialCanal"`
- `payload.canalId`: UUID del canal
- `payload.limite`: Número de mensajes a obtener (opcional, default: 50)

---

### Respuesta del Servidor

```json
{
  "action": "solicitarHistorialCanal",
  "status": "success",
  "message": "Historial obtenido: 25 mensajes",
  "data": {
    "mensajes": [
      {
        "mensajeId": "msg-canal-1",
        "canalId": "canal-uuid-123",
        "remitenteId": "user-uuid-1",
        "remitenteNombre": "Juan Pérez",
        "contenido": "Hola a todos!",
        "tipo": "TEXTO",
        "fileId": null,
        "fechaEnvio": "2025-10-17T10:30:45",
        "fileName": null
      }
    ],
    "tieneMas": false,
    "totalMensajes": 25,
    "canalId": "canal-uuid-123",
    "canalNombre": "Canal General"
  }
}
```

---

## 3. 💬 Enviar Mensaje de Texto

### Mensaje Privado

**Cliente → Servidor**
```json
{
  "action": "enviarMensajePrivado",
  "payload": {
    "remitenteId": "user-uuid-1",
    "destinatarioId": "user-uuid-2",
    "contenido": "Hola, ¿cómo estás?",
    "tipo": "TEXTO"
  }
}
```

**Campos:**
- `remitenteId`: UUID del usuario que envía
- `destinatarioId`: UUID del usuario que recibe
- `contenido`: Texto del mensaje
- `tipo`: `"TEXTO"`

---

### Respuesta al Remitente

```json
{
  "action": "enviarMensajePrivado",
  "status": "success",
  "message": "Mensaje enviado exitosamente",
  "data": {
    "id": 1234,
    "remitenteId": "user-uuid-1",
    "destinatarioId": "user-uuid-2",
    "remitenteNombre": "Juan Pérez",
    "destinatarioNombre": "María López",
    "contenido": "Hola, ¿cómo estás?",
    "tipo": "TEXTO",
    "fileId": null,
    "fileName": null,
    "fechaEnvio": "2025-10-17T10:35:00"
  }
}
```

---

### Notificación Push al Destinatario 🔔

**Servidor → Cliente Destinatario (automático)**
```json
{
  "action": "nuevoMensajeDirecto",
  "status": "success",
  "message": "Nuevo mensaje directo recibido",
  "data": {
    "id": 1234,
    "remitenteId": "user-uuid-1",
    "destinatarioId": "user-uuid-2",
    "remitenteNombre": "Juan Pérez",
    "destinatarioNombre": "María López",
    "contenido": "Hola, ¿cómo estás?",
    "tipo": "TEXTO",
    "fileId": null,
    "fileName": null,
    "fechaEnvio": "2025-10-17T10:35:00"
  }
}
```

**Importante:** Esta notificación se envía automáticamente si el destinatario está conectado. El mensaje se marca como "ENTREGADO" en la BD.

---

### Mensaje a Canal

**Cliente → Servidor**
```json
{
  "action": "enviarMensajeCanal",
  "payload": {
    "remitenteId": "user-uuid-1",
    "canalId": "canal-uuid-123",
    "contenido": "Hola a todos!",
    "tipo": "TEXTO"
  }
}
```

---

## 4. 🖼️ Enviar Mensaje con Imagen

**Proceso de 2 pasos:**

### PASO 1: Subir la imagen usando el sistema de chunks

#### 1.1 Iniciar transferencia

```json
{
  "action": "startFileUpload",
  "payload": {
    "fileName": "foto_perfil.jpg",
    "fileSize": 2048576,
    "fileType": "image/jpeg",
    "uploadType": "IMAGEN",
    "chunkSize": 65536
  }
}
```

**Respuesta:**
```json
{
  "action": "startFileUpload",
  "status": "success",
  "message": "Transferencia iniciada",
  "data": {
    "transferId": "transfer-uuid-abc",
    "fileId": "file-uuid-img-123",
    "totalChunks": 32,
    "chunkSize": 65536
  }
}
```

---

#### 1.2 Enviar chunks de la imagen

```json
{
  "action": "uploadFileChunk",
  "payload": {
    "transferId": "transfer-uuid-abc",
    "chunkIndex": 0,
    "chunkData": "/9j/4AAQSkZJRgABAQEAYABgAAD...",
    "isLastChunk": false
  }
}
```

**Campos:**
- `transferId`: ID de la transferencia (del paso 1.1)
- `chunkIndex`: Índice del chunk (0, 1, 2, ...)
- `chunkData`: Datos del chunk en Base64
- `isLastChunk`: `false` para chunks intermedios, `true` para el último

Repetir para cada chunk hasta completar la imagen.

---

#### 1.3 Finalizar transferencia

```json
{
  "action": "endFileUpload",
  "payload": {
    "transferId": "transfer-uuid-abc"
  }
}
```

**Respuesta:**
```json
{
  "action": "endFileUpload",
  "status": "success",
  "message": "Archivo subido exitosamente",
  "data": {
    "fileId": "file-uuid-img-123",
    "fileName": "foto_perfil.jpg",
    "fileSize": 2048576,
    "uploadedAt": "2025-10-17T10:40:00"
  }
}
```

---

### PASO 2: Enviar mensaje con referencia a la imagen

```json
{
  "action": "enviarMensajePrivado",
  "payload": {
    "remitenteId": "user-uuid-1",
    "destinatarioId": "user-uuid-2",
    "contenido": "Mira esta foto",
    "tipo": "IMAGEN",
    "fileId": "file-uuid-img-123",
    "fileName": "foto_perfil.jpg"
  }
}
```

**Respuesta:**
```json
{
  "action": "enviarMensajePrivado",
  "status": "success",
  "message": "Mensaje con imagen enviado exitosamente",
  "data": {
    "id": 5678,
    "remitenteId": "user-uuid-1",
    "destinatarioId": "user-uuid-2",
    "remitenteNombre": "Juan Pérez",
    "destinatarioNombre": "María López",
    "contenido": "Mira esta foto",
    "tipo": "IMAGEN",
    "fileId": "file-uuid-img-123",
    "fileName": "foto_perfil.jpg",
    "fechaEnvio": "2025-10-17T10:40:15"
  }
}
```

**Notificación Push al Destinatario:**
```json
{
  "action": "nuevoMensajeDirecto",
  "status": "success",
  "message": "Nuevo mensaje directo recibido",
  "data": {
    "id": 5678,
    "remitenteId": "user-uuid-1",
    "destinatarioId": "user-uuid-2",
    "remitenteNombre": "Juan Pérez",
    "destinatarioNombre": "María López",
    "contenido": "Mira esta foto",
    "tipo": "IMAGEN",
    "fileId": "file-uuid-img-123",
    "fileName": "foto_perfil.jpg",
    "fechaEnvio": "2025-10-17T10:40:15"
  }
}
```

---

## 5. 🎤 Enviar Mensaje de Audio

**Proceso de 2 pasos (similar a imagen):**

### PASO 1: Subir el archivo de audio

#### 1.1 Iniciar transferencia

```json
{
  "action": "startFileUpload",
  "payload": {
    "fileName": "nota_voz.mp3",
    "fileSize": 524288,
    "fileType": "audio/mpeg",
    "uploadType": "AUDIO",
    "chunkSize": 65536
  }
}
```

**Respuesta:**
```json
{
  "action": "startFileUpload",
  "status": "success",
  "message": "Transferencia iniciada",
  "data": {
    "transferId": "transfer-audio-xyz",
    "fileId": "file-audio-789",
    "totalChunks": 8,
    "chunkSize": 65536
  }
}
```

---

#### 1.2 Enviar chunks del audio

```json
{
  "action": "uploadFileChunk",
  "payload": {
    "transferId": "transfer-audio-xyz",
    "chunkIndex": 0,
    "chunkData": "SUQzBAAAAAAAI1RTU0UAAAAPAAADTGF2ZjU4Ljc2LjEwMAAAAAAAAAAAAAAA...",
    "isLastChunk": false
  }
}
```

Repetir para cada chunk (0, 1, 2, ..., 7).

---

#### 1.3 Finalizar transferencia

```json
{
  "action": "endFileUpload",
  "payload": {
    "transferId": "transfer-audio-xyz"
  }
}
```

**Respuesta:**
```json
{
  "action": "endFileUpload",
  "status": "success",
  "message": "Archivo subido exitosamente",
  "data": {
    "fileId": "file-audio-789",
    "fileName": "nota_voz.mp3",
    "fileSize": 524288,
    "uploadedAt": "2025-10-17T10:45:00"
  }
}
```

---

### PASO 2: Enviar mensaje con referencia al audio

```json
{
  "action": "enviarMensajePrivado",
  "payload": {
    "remitenteId": "user-uuid-1",
    "destinatarioId": "user-uuid-2",
    "contenido": "Escucha esto 🎤",
    "tipo": "AUDIO",
    "fileId": "file-audio-789",
    "fileName": "nota_voz.mp3"
  }
}
```

**Respuesta:**
```json
{
  "action": "enviarMensajePrivado",
  "status": "success",
  "message": "Mensaje de audio enviado exitosamente",
  "data": {
    "id": 9012,
    "remitenteId": "user-uuid-1",
    "destinatarioId": "user-uuid-2",
    "remitenteNombre": "Juan Pérez",
    "destinatarioNombre": "María López",
    "contenido": "Escucha esto 🎤",
    "tipo": "AUDIO",
    "fileId": "file-audio-789",
    "fileName": "nota_voz.mp3",
    "fechaEnvio": "2025-10-17T10:45:30"
  }
}
```

**Notificación Push al Destinatario:**
```json
{
  "action": "nuevoMensajeDirecto",
  "status": "success",
  "message": "Nuevo mensaje directo recibido",
  "data": {
    "id": 9012,
    "remitenteId": "user-uuid-1",
    "destinatarioId": "user-uuid-2",
    "remitenteNombre": "Juan Pérez",
    "destinatarioNombre": "María López",
    "contenido": "Escucha esto 🎤",
    "tipo": "AUDIO",
    "fileId": "file-audio-789",
    "fileName": "nota_voz.mp3",
    "fechaEnvio": "2025-10-17T10:45:30"
  }
}
```

---

## 6. 📎 Enviar Mensaje con Archivo

El proceso es idéntico al de imagen/audio:

### PASO 1: Subir el archivo

```json
{
  "action": "startFileUpload",
  "payload": {
    "fileName": "documento.pdf",
    "fileSize": 1048576,
    "fileType": "application/pdf",
    "uploadType": "ARCHIVO",
    "chunkSize": 65536
  }
}
```

### PASO 2: Enviar chunks

```json
{
  "action": "uploadFileChunk",
  "payload": {
    "transferId": "transfer-file-123",
    "chunkIndex": 0,
    "chunkData": "JVBERi0xLjQKJeLjz9MKMyAwIG9iago8PC9UeXBlIC9QYWdlCi9QYXJlbnQgMSAwIFIKL1Jl...",
    "isLastChunk": false
  }
}
```

### PASO 3: Finalizar transferencia

```json
{
  "action": "endFileUpload",
  "payload": {
    "transferId": "transfer-file-123"
  }
}
```

### PASO 4: Enviar mensaje con referencia

```json
{
  "action": "enviarMensajePrivado",
  "payload": {
    "remitenteId": "user-uuid-1",
    "destinatarioId": "user-uuid-2",
    "contenido": "Te envío el documento",
    "tipo": "ARCHIVO",
    "fileId": "file-doc-456",
    "fileName": "documento.pdf"
  }
}
```

---

## 7. 📥 Descargar Archivo/Audio/Imagen

Cuando el cliente recibe un mensaje con `fileId`, puede descargar el archivo.

### 7.1 Iniciar descarga

**Cliente → Servidor**
```json
{
  "action": "startFileDownload",
  "payload": {
    "fileId": "file-audio-789"
  }
}
```

**Respuesta:**
```json
{
  "action": "startFileDownload",
  "status": "success",
  "message": "Descarga iniciada",
  "data": {
    "transferId": "download-uuid-abc",
    "fileId": "file-audio-789",
    "fileName": "nota_voz.mp3",
    "fileSize": 524288,
    "totalChunks": 8,
    "chunkSize": 65536
  }
}
```

---

### 7.2 Solicitar cada chunk

**Cliente → Servidor**
```json
{
  "action": "requestFileChunk",
  "payload": {
    "transferId": "download-uuid-abc",
    "chunkIndex": 0
  }
}
```

**Respuesta:**
```json
{
  "action": "fileChunkData",
  "status": "success",
  "message": "Chunk enviado",
  "data": {
    "transferId": "download-uuid-abc",
    "chunkIndex": 0,
    "chunkData": "SUQzBAAAAAAAI1RTU0UAAAAPAAADTGF2ZjU4Ljc2LjEwMAAAAAAAAAAAAAAA...",
    "isLastChunk": false
  }
}
```

Repetir para cada chunk (0, 1, 2, ..., 7) hasta `isLastChunk: true`.

---

## 8. 🔔 Notificaciones Push

El servidor envía notificaciones push automáticamente cuando:

### 8.1 Nuevo Mensaje Directo

**Condición:** El destinatario está conectado.

**Servidor → Cliente**
```json
{
  "action": "nuevoMensajeDirecto",
  "status": "success",
  "message": "Nuevo mensaje directo recibido",
  "data": {
    "id": 1234,
    "remitenteId": "user-uuid-1",
    "destinatarioId": "user-uuid-2",
    "remitenteNombre": "Juan Pérez",
    "destinatarioNombre": "María López",
    "contenido": "Hola",
    "tipo": "TEXTO",
    "fileId": null,
    "fileName": null,
    "fechaEnvio": "2025-10-17T10:35:00"
  }
}
```

---

### 8.2 Nuevo Mensaje en Canal

**Condición:** El usuario es miembro del canal y está conectado.

**Servidor → Cliente**
```json
{
  "action": "nuevoMensajeCanal",
  "status": "success",
  "message": "Nuevo mensaje en canal",
  "data": {
    "id": 5678,
    "canalId": "canal-uuid-123",
    "remitenteId": "user-uuid-1",
    "remitenteNombre": "Juan Pérez",
    "contenido": "Hola a todos",
    "tipo": "TEXTO",
    "fileId": null,
    "fileName": null,
    "fechaEnvio": "2025-10-17T10:35:00"
  }
}
```

---

### 8.3 Handler de Notificaciones en el Cliente

**Pseudocódigo recomendado:**

```java
public void handleServerResponse(DTOResponse response) {
    switch (response.getAction()) {
        case "nuevoMensajeDirecto":
            DTOMensaje mensaje = parsearMensaje(response.getData());
            
            // Si el chat con ese contacto está abierto
            if (isChatAbierto(mensaje.getRemitenteId())) {
                // Mostrar mensaje inmediatamente en la vista
                agregarMensajeAVista(mensaje);
            } else {
                // Mostrar notificación/badge de mensaje nuevo
                mostrarNotificacionNuevoMensaje(mensaje);
                incrementarContadorMensajesNuevos(mensaje.getRemitenteId());
            }
            break;
            
        case "nuevoMensajeCanal":
            DTOMensaje mensajeCanal = parsearMensaje(response.getData());
            
            if (isCanalAbierto(mensajeCanal.getCanalId())) {
                agregarMensajeACanalVista(mensajeCanal);
            } else {
                mostrarNotificacionNuevoMensajeCanal(mensajeCanal);
                incrementarContadorMensajesNuevosCanal(mensajeCanal.getCanalId());
            }
            break;
            
        default:
            // Manejar otras acciones...
            break;
    }
}
```

---

## 9. 📝 Tipos de Mensaje Soportados

El servidor soporta los siguientes tipos de mensaje:

| Tipo | Descripción | Requiere fileId |
|------|-------------|-----------------|
| `TEXTO` | Mensaje de texto simple | ❌ No |
| `IMAGEN` | Mensaje con imagen adjunta | ✅ Sí |
| `AUDIO` | Mensaje con audio adjunto | ✅ Sí |
| `ARCHIVO` | Mensaje con archivo adjunto | ✅ Sí |
| `VIDEO` | Mensaje con video adjunto | ✅ Sí |

---

## 10. 🚦 Estados de Mensaje

Los mensajes pueden tener los siguientes estados:

| Estado | Descripción |
|--------|-------------|
| `ENVIADO` | Mensaje guardado en BD, pero destinatario offline |
| `ENTREGADO` | Mensaje entregado al destinatario (está online) |
| `LEIDO` | Destinatario marcó el mensaje como leído |

**Nota:** El servidor marca automáticamente como `ENTREGADO` cuando envía la notificación push.

---

## 📋 Resumen de Acciones Disponibles

### Mensajería
- `solicitarHistorialPrivado` - Obtener historial de chat privado
- `solicitarHistorialCanal` - Obtener historial de canal
- `enviarMensajePrivado` - Enviar mensaje directo
- `enviarMensajeCanal` - Enviar mensaje a canal
- `marcar_mensaje_leido` - Marcar mensaje como leído

### Transferencia de Archivos
- `startFileUpload` - Iniciar subida de archivo
- `uploadFileChunk` - Enviar chunk de archivo
- `endFileUpload` - Finalizar subida de archivo
- `startFileDownload` - Iniciar descarga de archivo
- `requestFileChunk` - Solicitar chunk de descarga

### Notificaciones Push (Automáticas)
- `nuevoMensajeDirecto` - Nuevo mensaje privado recibido
- `nuevoMensajeCanal` - Nuevo mensaje en canal

---

## 🔧 Configuración Recomendada

### Tamaño de Chunks
- **Imágenes:** 65536 bytes (64 KB)
- **Audios:** 65536 bytes (64 KB)
- **Archivos grandes:** 65536 bytes (64 KB)
- **Archivos pequeños:** 32768 bytes (32 KB)

### Timeouts
- **Conexión:** 30 segundos
- **Lectura:** 60 segundos
- **Escritura:** 60 segundos

### Límites
- **Tamaño máximo de archivo:** Sin límite configurado en servidor
- **Mensajes por solicitud de historial:** 50 (default)

---

## ⚠️ Manejo de Errores

Todas las respuestas de error tienen el formato:

```json
{
  "action": "nombreAccion",
  "status": "error",
  "message": "Descripción del error",
  "data": null
}
```

**Errores comunes:**

- `"Usuario no autenticado"` - La sesión expiró o no existe
- `"contactoId es requerido"` - Falta el ID del contacto
- `"El ID del usuario es requerido"` - Error interno de autenticación
- `"Acción no reconocida"` - La acción no existe en el servidor

---

## 📞 Soporte

Para cualquier duda o problema con la API, contactar al equipo de desarrollo del servidor.

**Fecha de última actualización:** 17 de Octubre de 2025
