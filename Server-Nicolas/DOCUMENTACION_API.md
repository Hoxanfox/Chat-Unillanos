# 📚 Documentación de API - Chat Unillanos Server

## Índice
1. [Formato de Comunicación](#formato-de-comunicación)
2. [Rutas de Usuarios](#rutas-de-usuarios)
3. [Rutas de Canales](#rutas-de-canales)
4. [Rutas de Mensajes](#rutas-de-mensajes)
5. [Rutas de Archivos](#rutas-de-archivos)
6. [Rutas P2P (Peer-to-Peer)](#rutas-p2p-peer-to-peer)
7. [Notificaciones Push (WebSocket)](#notificaciones-push-websocket)

---

## Formato de Comunicación

### Request (Cliente → Servidor)
```json
{
  "action": "nombreDeLaAccion",
  "payload": {
    // datos específicos de la acción
  }
}
```

### Response (Servidor → Cliente)
```json
{
  "action": "nombreDeLaAccion",
  "status": true/false,
  "message": "Mensaje descriptivo",
  "data": {
    // datos de respuesta
  }
}
```

---

## Rutas de Usuarios

### 1. Autenticar Usuario
**Action:** `authenticateUser` | `authenticateuser`

**Request:**
```json
{
  "action": "authenticateUser",
  "payload": {
    "nombreUsuario": "string",
    "password": "string"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "authenticateUser",
  "status": true,
  "message": "Autenticación exitosa",
  "data": {
    "userId": "uuid",
    "nombre": "string",
    "email": "string",
    "fileId": "string (ruta de foto)"
  }
}
```

**Errores:**
- `nombreUsuario` o `password` inválidos
- Usuario no existe

---

### 2. Registrar Usuario
**Action:** `registerUser` | `registeruser`

**Request:**
```json
{
  "action": "registerUser",
  "payload": {
    "username": "string",
    "email": "string",
    "password": "string (min 6 caracteres)",
    "photoFileId": "string (opcional)"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "registerUser",
  "status": true,
  "message": "Registro exitoso",
  "data": {
    "username": "string",
    "email": "string",
    "message": "Usuario registrado exitosamente. Ahora puedes iniciar sesión."
  }
}
```

**Validaciones:**
- Username: requerido
- Email: requerido, formato válido (@, .)
- Password: mínimo 6 caracteres
- Email único (no duplicado)
- Username único (no duplicado)

---

### 3. Cerrar Sesión
**Action:** `logoutUser` | `logoutuser`

**Request:**
```json
{
  "action": "logoutUser",
  "payload": {
    "userId": "uuid"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "logoutUser",
  "status": true,
  "message": "Sesión cerrada exitosamente",
  "data": null
}
```

**Requiere:** Usuario autenticado

---

### 4. Listar Contactos
**Action:** `listarContactos` | `listarcontactos`

**Request:**
```json
{
  "action": "listarContactos",
  "payload": {
    "usuarioId": "uuid"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "listarContactos",
  "status": true,
  "message": "Contactos obtenidos exitosamente",
  "data": [
    {
      "id": "uuid",
      "peerid": "uuid (opcional)",
      "nombre": "string",
      "email": "string",
      "imagenBase64": "string (opcional)",
      "imagenId": "string (ruta)",
      "conectado": "ONLINE" | "OFFLINE"
    }
  ]
}
```

**Requiere:** Usuario autenticado

---

## Rutas de Canales

### 1. Listar Canales
**Action:** `listarCanales` | `listarcanales`

**Request:**
```json
{
  "action": "listarCanales",
  "payload": {
    "usuarioId": "uuid"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "listarCanales",
  "status": true,
  "message": "Lista de canales obtenida",
  "data": [
    {
      "idCanal": "uuid",
      "idPeer": "uuid (opcional)",
      "nombreCanal": "string",
      "ownerId": "uuid"
    }
  ]
}
```

**Requiere:** Usuario autenticado

---

### 2. Crear Canal
**Action:** `crearCanal` | `crearcanal`

**Request:**
```json
{
  "action": "crearCanal",
  "payload": {
    "nombre": "string",
    "tipo": "GRUPO" | "DIRECTO" (opcional, default: GRUPO)
  }
}
```

**Response (Éxito):**
```json
{
  "action": "crearCanal",
  "status": true,
  "message": "Canal creado exitosamente",
  "data": {
    "id": "uuid",
    "nombre": "string",
    "creadorId": "uuid",
    "tipo": "string",
    "channelId": "uuid",
    "channelName": "string",
    "channelType": "string",
    "owner": {
      "userId": "uuid",
      "username": "string"
    },
    "peerId": "uuid (opcional)"
  }
}
```

**Requiere:** Usuario autenticado

---

### 3. Crear Canal Directo
**Action:** `crearCanalDirecto` | `iniciarchat` | `obtenerchatprivado`

**Request:**
```json
{
  "action": "crearCanalDirecto",
  "payload": {
    "user1Id": "uuid",
    "user2Id": "uuid"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "crearCanalDirecto",
  "status": true,
  "message": "Canal directo creado/obtenido exitosamente",
  "data": {
    "id": "uuid",
    "nombre": "string",
    "creadorId": "uuid",
    "tipo": "DIRECTO",
    "channelId": "uuid",
    "channelName": "string",
    "channelType": "DIRECTO",
    "owner": {
      "userId": "uuid",
      "username": "string"
    },
    "peerId": "uuid (opcional)",
    "otherUser": {
      "userId": "uuid",
      "username": "string",
      "email": "string",
      "photoAddress": "string",
      "conectado": "true" | "false"
    }
  }
}
```

**Validaciones:**
- Usuario autenticado debe ser user1Id o user2Id
- Ambos usuarios deben existir

**Requiere:** Usuario autenticado

---

### 4. Invitar Miembro
**Action:** `invitarMiembro` | `invitarusuario`

**Request:**
```json
{
  "action": "invitarMiembro",
  "payload": {
    "channelId": "uuid",
    "userIdToInvite": "uuid"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "invitarMiembro",
  "status": true,
  "message": "Invitación enviada exitosamente",
  "data": {
    "channelId": "uuid",
    "invitedUserId": "uuid",
    "invitedUsername": "string"
  }
}
```

**Validaciones:**
- Usuario autenticado debe ser el propietario del canal
- Usuario a invitar debe existir
- Usuario no debe ser ya miembro

**Requiere:** Usuario autenticado (propietario del canal)

**Push:** Envía evento `userInvited` al usuario invitado

---

### 5. Responder Invitación
**Action:** `responderInvitacion` | `aceptarinvitacion` | `rechazarinvitacion`

**Request:**
```json
{
  "action": "responderInvitacion",
  "payload": {
    "channelId": "uuid",
    "accepted": true | false
  }
}
```

**Response (Éxito):**
```json
{
  "action": "responderInvitacion",
  "status": true,
  "message": "Invitación aceptada. Ahora eres miembro del canal" | "Invitación rechazada",
  "data": {
    "channelId": "uuid",
    "accepted": true | false
  }
}
```

**Requiere:** Usuario autenticado con invitación pendiente

---

### 6. Obtener Invitaciones
**Action:** `obtenerInvitaciones` | `listarinvitaciones` | `invitacionespendientes`

**Request:**
```json
{
  "action": "obtenerInvitaciones",
  "payload": {
    "usuarioId": "uuid"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "obtenerInvitaciones",
  "status": true,
  "message": "Invitaciones obtenidas",
  "data": {
    "invitaciones": [
      {
        "channelId": "uuid",
        "channelName": "string",
        "channelType": "string",
        "owner": {
          "userId": "uuid",
          "username": "string"
        },
        "peerId": "uuid (opcional)"
      }
    ],
    "totalInvitaciones": 0
  }
}
```

**Requiere:** Usuario autenticado

---

### 7. Listar Miembros
**Action:** `listarMiembros` | `obtenermiembroscanal`

**Request:**
```json
{
  "action": "listarMiembros",
  "payload": {
    "canalId": "uuid",
    "solicitanteId": "uuid"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "listarMiembros",
  "status": true,
  "message": "Miembros obtenidos",
  "data": {
    "miembros": [
      {
        "userId": "uuid",
        "username": "string",
        "email": "string",
        "photoAddress": "string",
        "conectado": "true" | "false",
        "rol": "PROPIETARIO" | "MIEMBRO"
      }
    ],
    "totalMiembros": 0,
    "canalId": "uuid"
  }
}
```

**Validaciones:**
- Usuario autenticado debe ser miembro del canal

**Requiere:** Usuario autenticado

---

## Rutas de Mensajes

### 1. Enviar Mensaje de Texto
**Action:** `enviarMensajeCanal` | `enviarmensajetexto`

**Request:**
```json
{
  "action": "enviarMensajeCanal",
  "payload": {
    "canalId": "uuid",
    "contenido": "string (max 5000 caracteres)"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "enviarMensajeCanal",
  "status": true,
  "message": "Mensaje enviado",
  "data": {
    "messageId": "uuid",
    "channelId": "uuid",
    "author": {
      "userId": "uuid",
      "username": "string"
    },
    "timestamp": "ISO-8601 datetime",
    "messageType": "TEXT",
    "content": "string"
  }
}
```

**Validaciones:**
- Usuario debe ser miembro del canal
- Contenido no vacío
- Máximo 5000 caracteres

**Requiere:** Usuario autenticado

**Push:** Envía evento `newMessage` a todos los miembros del canal

---

### 2. Enviar Mensaje de Audio
**Action:** `enviarMensajeAudio` | `enviaraudio`

**Request:**
```json
{
  "action": "enviarMensajeAudio",
  "payload": {
    "canalId": "uuid",
    "audioBase64": "string (Base64)",
    "duration": 0.0 (opcional),
    "format": "webm" | "wav" (opcional, default: webm)
  }
}
```

**Response (Éxito):**
```json
{
  "action": "enviarMensajeAudio",
  "status": true,
  "message": "Audio enviado",
  "data": {
    "messageId": "uuid",
    "channelId": "uuid",
    "author": {
      "userId": "uuid",
      "username": "string"
    },
    "timestamp": "ISO-8601 datetime",
    "messageType": "AUDIO",
    "content": "string (ruta del archivo)",
    "duration": 0.0
  }
}
```

**Validaciones:**
- Usuario debe ser miembro del canal
- Audio no vacío (Base64)

**Requiere:** Usuario autenticado

**Push:** Envía evento `newMessage` a todos los miembros del canal

---

### 3. Obtener Historial de Canal
**Action:** `solicitarHistorialCanal` | `obtenermensajescanal`

**Request:**
```json
{
  "action": "solicitarHistorialCanal",
  "payload": {
    "canalId": "uuid",
    "usuarioId": "uuid"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "solicitarHistorialCanal",
  "status": true,
  "message": "Historial obtenido",
  "data": {
    "mensajes": [
      {
        "messageId": "uuid",
        "channelId": "uuid",
        "author": {
          "userId": "uuid",
          "username": "string"
        },
        "timestamp": "ISO-8601 datetime",
        "messageType": "TEXT" | "AUDIO",
        "content": "string (para AUDIO, es Base64 del archivo)",
        "error": "string (opcional, si hay error al cargar audio)"
      }
    ],
    "totalMensajes": 0
  }
}
```

**Validaciones:**
- Usuario debe ser miembro del canal

**Requiere:** Usuario autenticado

**Nota:** Los audios se devuelven en Base64 en el campo `content`

---

### 4. Obtener Transcripciones
**Action:** `obtenerTranscripciones` | `vertranscripciones`

**Request:**
```json
{
  "action": "obtenerTranscripciones",
  "payload": {
    "messageId": "uuid (opcional)"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "obtenerTranscripciones",
  "status": true,
  "message": "Transcripciones obtenidas",
  "data": {
    "transcripciones": [
      {
        "messageId": "uuid",
        "text": "string (texto transcrito)",
        "timestamp": "ISO-8601 datetime",
        "author": {
          "userId": "uuid",
          "username": "string"
        },
        "channelId": "uuid"
      }
    ],
    "totalTranscripciones": 0
  }
}
```

**Requiere:** Usuario autenticado

---

### 5. Enviar Mensaje Directo (Texto)
**Action:** `enviarMensajeDirecto` | `enviarmensajedirecto`

**Request:**
```json
{
  "action": "enviarMensajeDirecto",
  "payload": {
    "peerDestinoId": "uuid (opcional)",
    "peerRemitenteId": "uuid (opcional)",
    "remitenteId": "uuid",
    "destinatarioId": "uuid",
    "tipo": "texto" | "audio",
    "contenido": "string (max 5000 caracteres)",
    "fechaEnvio": "string (opcional)"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "enviarMensajeDirecto",
  "status": true,
  "message": "Mensaje enviado",
  "data": {
    "mensajeId": "uuid",
    "fechaEnvio": "ISO-8601 datetime"
  }
}
```

**Validaciones:**
- Remitente debe ser el usuario autenticado
- Destinatario debe existir
- Contenido no vacío
- Máximo 5000 caracteres

**Requiere:** Usuario autenticado

**Push:** Envía evento `newMessage` al destinatario

**Nota:** Crea automáticamente un canal directo si no existe

---

### 6. Enviar Mensaje Directo (Audio)
**Action:** `enviarMensajeDirectoAudio` | `enviarmensajedirectoaudio`

**Request:**
```json
{
  "action": "enviarMensajeDirectoAudio",
  "payload": {
    "peerDestinoId": "uuid (opcional)",
    "peerRemitenteId": "uuid (opcional)",
    "remitenteId": "uuid",
    "destinatarioId": "uuid",
    "tipo": "audio",
    "contenido": "string (ruta/enlace del archivo de audio)",
    "fechaEnvio": "string (opcional)"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "enviarMensajeDirectoAudio",
  "status": true,
  "message": "Mensaje de audio enviado",
  "data": {
    "mensajeId": "uuid",
    "fechaEnvio": "ISO-8601 datetime"
  }
}
```

**Validaciones:**
- Remitente debe ser el usuario autenticado
- Destinatario debe existir
- Contenido (enlace de audio) no vacío

**Requiere:** Usuario autenticado

**Push:** Envía evento `newMessage` al destinatario

---

### 7. Obtener Historial Privado
**Action:** `solicitarHistorialPrivado` | `solicitarhistorialprivado`

**Request:**
```json
{
  "action": "solicitarHistorialPrivado",
  "payload": {
    "remitenteId": "uuid",
    "peerRemitenteId": "uuid (opcional)",
    "destinatarioId": "uuid",
    "peerDestinatarioId": "uuid (opcional)"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "solicitarHistorialPrivado",
  "status": true,
  "message": "Historial privado obtenido exitosamente",
  "data": [
    {
      "mensajeId": "uuid",
      "remitenteId": "uuid",
      "destinatarioId": "uuid",
      "peerRemitenteId": "uuid (opcional)",
      "peerDestinoId": null,
      "tipo": "texto" | "audio",
      "contenido": "string",
      "fechaEnvio": "ISO-8601 datetime"
    }
  ]
}
```

**Validaciones:**
- Remitente debe ser el usuario autenticado

**Requiere:** Usuario autenticado

---

## Rutas de Archivos

### 1. Iniciar Subida de Archivo
**Action:** `startFileUpload` | `uploadfileforregistration`

**Request:**
```json
{
  "action": "startFileUpload",
  "payload": {
    "fileName": "string",
    "fileSize": 0,
    "chunkSize": 0,
    "mimeType": "string (opcional)"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "startFileUpload",
  "status": true,
  "message": "Upload iniciado",
  "data": {
    "uploadId": "string (UUID único para este upload)"
  }
}
```

---

### 2. Subir Chunk de Archivo
**Action:** `uploadFileChunk` | `uploadfilechunk`

**Request:**
```json
{
  "action": "uploadFileChunk",
  "payload": {
    "uploadId": "string",
    "chunkNumber": 0,
    "chunkDataBase64": "string (Base64)"
  }
}
```

**Response Push (Éxito):**
```json
{
  "action": "uploadFileChunk_{uploadId}_{chunkNumber}",
  "status": true,
  "message": "Chunk {chunkNumber} recibido",
  "data": null
}
```

**Nota:** La respuesta usa un action dinámico que incluye el uploadId y chunkNumber para ACK único

---

### 3. Finalizar Subida de Archivo
**Action:** `endFileUpload` | `endfileupload`

**Request:**
```json
{
  "action": "endFileUpload",
  "payload": {
    "uploadId": "string"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "endFileUpload",
  "status": true,
  "message": "Archivo subido",
  "data": {
    "fileId": "string (identificador único del archivo)",
    "fileName": "string",
    "filePath": "string (ruta en el servidor)",
    "fileSize": 0,
    "uploadedAt": "ISO-8601 datetime"
  }
}
```

**Nota:** El directorio de destino depende de si el usuario está autenticado:
- Autenticado: `audio_files/`
- No autenticado (registro): `user_photos/`

---

### 4. Iniciar Descarga de Archivo
**Action:** `startFileDownload` | `startfiledownload`

**Request:**
```json
{
  "action": "startFileDownload",
  "payload": {
    "fileId": "string"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "startFileDownload",
  "status": true,
  "message": "Descarga iniciada",
  "data": {
    "downloadId": "string (UUID único para esta descarga)",
    "fileName": "string",
    "fileSize": 0,
    "totalChunks": 0,
    "chunkSize": 0
  }
}
```

---

### 5. Solicitar Chunk de Descarga
**Action:** `requestFileChunk` | `requestfilechunk`

**Request:**
```json
{
  "action": "requestFileChunk",
  "payload": {
    "downloadId": "string",
    "chunkNumber": 0
  }
}
```

**Response Push (Éxito):**
```json
{
  "action": "downloadFileChunk_{downloadId}_{chunkNumber}",
  "status": true,
  "message": "Enviando chunk",
  "data": {
    "chunkNumber": 0,
    "chunkDataBase64": "string (Base64 del chunk)"
  }
}
```

**Nota:** La respuesta usa un action dinámico para identificar específicamente el chunk

---

### 6. Descargar Archivo desde Peer
**Action:** `descargarArchivo` | `descargararchivo`

**Request:**
```json
{
  "action": "descargarArchivo",
  "payload": {
    "peerDestinoId": "uuid",
    "fileId": "string"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "descargararchivo",
  "status": true,
  "message": "Archivo descargado exitosamente desde peer",
  "data": {
    "fileId": "string",
    "fileDataBase64": "string (Base64 completo del archivo)",
    "size": 0
  }
}
```

**Validaciones:**
- El peer destino debe estar activo y conectado
- El archivo debe existir en el peer

---

## Rutas P2P (Peer-to-Peer)

### 1. Añadir Peer
**Action:** `añadirPeer` | `aadirpeer`

**Request:**
```json
{
  "action": "añadirPeer",
  "payload": {
    "ip": "string (dirección IP)",
    "puerto": 0 (1-65535),
    "nombreServidor": "string (opcional)"
  }
}
```

**Response (Éxito):**
```json
{
  "action": "añadirPeer",
  "status": true,
  "message": "Peer añadido exitosamente",
  "data": {
    "peerId": "uuid",
    "ip": "string",
    "puerto": 0,
    "conectado": true | false,
    "ultimoLatido": "ISO-8601 datetime (puede ser null)",
    "nombreServidor": "string (opcional)"
  }
}
```

**Validaciones:**
- IP no vacía
- Puerto entre 1 y 65535
- No duplicar peer (misma IP y puerto)

---

### 2. Listar Peers Disponibles
**Action:** `listarPeersDisponibles` | `listarpeersDisponibles`

**Request:**
```json
{
  "action": "listarPeersDisponibles",
  "payload": {
    "soloActivos": true | false (opcional, default: false)
  }
}
```

**Response (Éxito):**
```json
{
  "action": "listarPeersDisponibles",
  "status": true,
  "message": "Lista de peers obtenida exitosamente",
  "data": {
    "peers": [
      {
        "peerId": "uuid",
        "ip": "string",
        "puerto": 0,
        "conectado": true | false,
        "ultimoLatido": "ISO-8601 datetime",
        "nombreServidor": "string (opcional)"
      }
    ],
    "total": 0,
    "soloActivos": true | false
  }
}
```

---

### 3. Reportar Latido (Heartbeat)
**Action:** `reportarLatido` | `reportarlatido`

**Request:**
```json
{
  "action": "reportarLatido",
  "payload": {
    "peerId": "uuid",
    "ip": "string (opcional)",
    "puerto": 0 (opcional)
  }
}
```

**Response (Éxito):**
```json
{
  "action": "reportarLatido",
  "status": true,
  "message": "Latido recibido exitosamente",
  "data": {
    "peerId": "uuid",
    "proximoLatidoMs": 0 (milisegundos hasta el próximo heartbeat esperado),
    "timestamp": "ISO-8601 datetime"
  }
}
```

**Validaciones:**
- PeerId debe existir y ser válido (UUID)

**Nota:** Los peers deben enviar heartbeats periódicamente para mantenerse activos

---

### 4. Retransmitir Petición a Peer
**Action:** `retransmitirPeticion` | `retransmitirpeticion`

**Request:**
```json
{
  "action": "retransmitirPeticion",
  "payload": {
    "peerDestinoId": "uuid",
    "peticionOriginal": {
      "action": "string (acción a ejecutar en el peer)",
      "payload": {
        // payload de la petición original
      }
    }
  }
}
```

**Response (Éxito):**
```json
{
  "action": "retransmitirPeticion",
  "status": true,
  "message": "Petición retransmitida exitosamente",
  "data": {
    "peerDestinoId": "uuid",
    "accionRetransmitida": "string",
    "respuestaPeer": {
      "action": "string",
      "status": true | false,
      "message": "string",
      "data": {}
    }
  }
}
```

**Validaciones:**
- Peer destino debe estar activo
- Petición original debe tener acción válida

**Errores comunes:**
- "Peer no está activo"
- "Peer no encontrado"
- "Error de comunicación con el peer"

---

### 5. Actualizar Lista de Peers
**Action:** `actualizarListaPeers` | `actualizarlistapeers`

**Request:**
```json
{
  "action": "actualizarListaPeers",
  "payload": {
    "peers": [
      {
        "peerId": "uuid (opcional)",
        "ip": "string",
        "puerto": 0,
        "nombreServidor": "string (opcional)"
      }
    ]
  }
}
```

**Response (Éxito):**
```json
{
  "action": "actualizarListaPeers",
  "status": true,
  "message": "Lista actualizada: X agregados, Y errores de Z recibidos",
  "data": {
    "totalRecibidos": 0,
    "peersAgregados": 0,
    "peersActualizados": 0,
    "peersError": 0,
    "errores": ["string (lista de errores, si hay)"],
    "totalPeersActuales": 0
  }
}
```

**Nota:** Esta ruta permite sincronizar la lista de peers con otro servidor

---

## Notificaciones Push (WebSocket)

Las notificaciones push son mensajes que el servidor envía proactivamente a los clientes conectados vía WebSocket, sin que el cliente haya solicitado la información explícitamente.

### Formato General de Push
```json
{
  "action": "push_{tipoDeEvento}",
  "status": true,
  "message": "Descripción del evento",
  "data": {
    // datos específicos del evento
  }
}
```

---

### 1. Nuevo Mensaje (Push)
**Event:** `NewMessageEvent`
**Action:** `push_newMessage`

**Enviado cuando:** Un usuario envía un mensaje a un canal del cual el cliente es miembro

```json
{
  "action": "push_newMessage",
  "status": true,
  "message": "Nuevo mensaje recibido",
  "data": {
    "messageId": "uuid",
    "channelId": "uuid",
    "author": {
      "userId": "uuid",
      "username": "string",
      "peerId": "uuid (opcional)"
    },
    "timestamp": "ISO-8601 datetime",
    "messageType": "TEXT" | "AUDIO",
    "content": "string"
  }
}
```

**Destinatarios:** Todos los miembros del canal (excepto el autor)

---

### 2. Invitación de Usuario (Push)
**Event:** `UserInvitedEvent`
**Action:** `push_userInvited`

**Enviado cuando:** Un usuario es invitado a un canal

```json
{
  "action": "push_userInvited",
  "status": true,
  "message": "Has sido invitado a un canal",
  "data": {
    "channelId": "uuid",
    "channelName": "string",
    "channelType": "GRUPO" | "DIRECTO",
    "owner": {
      "userId": "uuid",
      "username": "string"
    },
    "peerId": "uuid (opcional)"
  }
}
```

**Destinatarios:** El usuario invitado

---

### 3. Actualización de Lista de Contactos (Push)
**Event:** `ContactListUpdateEvent`
**Action:** `push_contactListUpdate`

**Enviado cuando:** 
- Un contacto se conecta
- Un contacto se desconecta
- Se actualiza el estado de un contacto

```json
{
  "action": "push_contactListUpdate",
  "status": true,
  "message": "Lista de contactos actualizada",
  "data": {
    "shouldRefresh": true
  }
}
```

**Destinatarios:** Todos los usuarios conectados

**Nota:** El cliente debe solicitar la lista de contactos actualizada tras recibir este push

---

### 4. Forzar Logout (Push)
**Event:** `ForceLogoutEvent`
**Action:** `push_forceLogout`

**Enviado cuando:** 
- Un administrador fuerza el logout de un usuario
- Se detecta una sesión duplicada
- Violación de seguridad

```json
{
  "action": "push_forceLogout",
  "status": true,
  "message": "Tu sesión ha sido cerrada",
  "data": {
    "userId": "uuid",
    "peerId": "uuid (opcional)",
    "motivo": "string (razón del logout forzado)"
  }
}
```

**Destinatarios:** El usuario específico que debe cerrar sesión

**Acción esperada del cliente:** Cerrar sesión inmediatamente y redirigir al login

---

### 5. Forzar Desconexión (Push)
**Event:** `ForceDisconnectEvent`
**Action:** `push_forceDisconnect`

**Enviado cuando:** El servidor necesita desconectar a un usuario sin cerrar su sesión

```json
{
  "action": "push_forceDisconnect",
  "status": true,
  "message": "Conexión cerrada por el servidor",
  "data": {
    "userId": "uuid",
    "motivo": "string (opcional)"
  }
}
```

**Destinatarios:** El usuario específico que debe desconectarse

---

### 6. Mensaje Broadcast (Push)
**Event:** `BroadcastMessageEvent`
**Action:** `push_broadcast`

**Enviado cuando:** El servidor envía un mensaje a todos los usuarios conectados

```json
{
  "action": "push_broadcast",
  "status": true,
  "message": "Mensaje del servidor",
  "data": {
    "message": "string (contenido del broadcast)",
    "timestamp": "ISO-8601 datetime",
    "priority": "INFO" | "WARNING" | "CRITICAL"
  }
}
```

**Destinatarios:** Todos los usuarios conectados

**Ejemplos de uso:**
- Anuncios del sistema
- Mantenimientos programados
- Actualizaciones importantes

---

### 7. Peer Conectado (Push)
**Event:** `PeerConnectedEvent`
**Action:** `push_peerConnected`

**Enviado cuando:** Un nuevo peer se conecta a la red P2P

```json
{
  "action": "push_peerConnected",
  "status": true,
  "message": "Nuevo peer conectado a la red",
  "data": {
    "peerId": "uuid",
    "peerIp": "string",
    "peerPort": 0
  }
}
```

**Destinatarios:** Todos los clientes interesados en la topología de la red P2P

---

### 8. Peer Desconectado (Push)
**Event:** `PeerDisconnectedEvent`
**Action:** `push_peerDisconnected`

**Enviado cuando:** Un peer se desconecta de la red P2P

```json
{
  "action": "push_peerDisconnected",
  "status": true,
  "message": "Peer desconectado de la red",
  "data": {
    "peerId": "uuid",
    "razon": "string (motivo de desconexión)"
  }
}
```

**Destinatarios:** Todos los clientes interesados en la topología de la red P2P

---

### 9. Lista de Peers Actualizada (Push)
**Event:** `PeerListUpdatedEvent`
**Action:** `push_peerListUpdated`

**Enviado cuando:** La lista de peers disponibles cambia significativamente

```json
{
  "action": "push_peerListUpdated",
  "status": true,
  "message": "Lista de peers actualizada",
  "data": {
    "activePeerIds": ["uuid", "uuid", ...],
    "totalActivePeers": 0
  }
}
```

**Destinatarios:** Todos los clientes interesados en la topología de la red P2P

**Nota:** El cliente puede solicitar la lista completa usando `listarPeersDisponibles`

---

### 10. ACK de Chunk Subido (Push Dinámico)
**Action:** `uploadFileChunk_{uploadId}_{chunkNumber}`

**Enviado cuando:** Se recibe exitosamente un chunk de archivo

```json
{
  "action": "uploadFileChunk_abc123_5",
  "status": true,
  "message": "Chunk 5 recibido",
  "data": null
}
```

**Nota:** Este es un push de acknowledgement para confirmar la recepción de chunks individuales

---

### 11. Chunk de Descarga (Push Dinámico)
**Action:** `downloadFileChunk_{downloadId}_{chunkNumber}`

**Enviado cuando:** El servidor envía un chunk solicitado de descarga

```json
{
  "action": "downloadFileChunk_def456_3",
  "status": true,
  "message": "Enviando chunk",
  "data": {
    "chunkNumber": 3,
    "chunkDataBase64": "string (Base64)"
  }
}
```

**Nota:** Este es un push en respuesta a `requestFileChunk`

---

## Códigos de Error Comunes

### Errores de Autenticación
- `Usuario no autenticado` - No se proporcionó token válido
- `Usuario no autorizado` - El usuario no tiene permisos para la acción
- `Token inválido o expirado` - La sesión ha expirado

### Errores de Validación
- `Campo requerido` - Falta un campo obligatorio
- `Formato inválido` - El formato del dato es incorrecto (ej: email, UUID)
- `Valor fuera de rango` - El valor no está en el rango permitido

### Errores de Negocio
- `Usuario ya existe` - Email o username duplicado
- `Canal no encontrado` - El canal no existe
- `No eres miembro del canal` - Usuario no tiene acceso al canal
- `Peer no está activo` - El peer destino no está conectado
- `Archivo no encontrado` - El archivo solicitado no existe

### Errores del Sistema
- `Error interno del servidor` - Error genérico del servidor
- `Servicio no disponible` - Servicio temporalmente fuera de servicio

---

## Notas Técnicas

### Autenticación
- Después de `authenticateUser`, el servidor mantiene la sesión del usuario
- Todas las rutas (excepto `authenticateUser` y `registerUser`) requieren autenticación previa
- El servidor verifica automáticamente que el `userId` en el payload coincida con el usuario autenticado

### Sistema de Chunks para Archivos
- Los archivos grandes se dividen en chunks para facilitar la transmisión
- Chunk size predeterminado: configurable en el servidor
- Cada chunk se confirma individualmente con un ACK push
- El cliente debe esperar el ACK antes de enviar el siguiente chunk

### Canales Directos
- Los canales directos se crean automáticamente al enviar el primer mensaje
- Si el canal ya existe, se reutiliza
- El nombre del canal directo se genera automáticamente

### Red P2P
- Los peers deben enviar heartbeats periódicamente (intervalo configurable)
- Si un peer no envía heartbeat por cierto tiempo, se marca como inactivo
- La retransmisión P2P permite que un servidor reenvíe peticiones a otros servidores

### WebSocket
- El cliente debe mantener la conexión WebSocket abierta para recibir pushes
- Si la conexión se cierra, el cliente debe reconectar y reautenticarse
- Los pushes se envían solo a usuarios conectados

---

## Ejemplos de Uso

### Ejemplo 1: Flujo de Autenticación y Envío de Mensaje

```javascript
// 1. Autenticar
{
  "action": "authenticateUser",
  "payload": {
    "nombreUsuario": "juan",
    "password": "123456"
  }
}

// 2. Listar canales
{
  "action": "listarCanales",
  "payload": {
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000"
  }
}

// 3. Enviar mensaje
{
  "action": "enviarMensajeCanal",
  "payload": {
    "canalId": "660e8400-e29b-41d4-a716-446655440000",
    "contenido": "Hola a todos!"
  }
}

// 4. Otros miembros reciben push
{
  "action": "push_newMessage",
  "status": true,
  "message": "Nuevo mensaje recibido",
  "data": {
    "messageId": "770e8400-e29b-41d4-a716-446655440000",
    "channelId": "660e8400-e29b-41d4-a716-446655440000",
    "author": {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "username": "juan"
    },
    "timestamp": "2025-11-07T02:53:00Z",
    "messageType": "TEXT",
    "content": "Hola a todos!"
  }
}
```

### Ejemplo 2: Subida de Archivo por Chunks

```javascript
// 1. Iniciar upload
{
  "action": "startFileUpload",
  "payload": {
    "fileName": "foto.jpg",
    "fileSize": 102400,
    "chunkSize": 10240
  }
}

// Respuesta: uploadId = "abc123"

// 2. Subir chunk 0
{
  "action": "uploadFileChunk",
  "payload": {
    "uploadId": "abc123",
    "chunkNumber": 0,
    "chunkDataBase64": "iVBORw0KG..."
  }
}

// Push ACK: uploadFileChunk_abc123_0

// 3. Repetir para chunks 1-9...

// 4. Finalizar
{
  "action": "endFileUpload",
  "payload": {
    "uploadId": "abc123"
  }
}

// Respuesta: fileId para usar en mensajes
```

### Ejemplo 3: Chat Privado

```javascript
// 1. Crear/obtener canal directo
{
  "action": "crearCanalDirecto",
  "payload": {
    "user1Id": "user-a-uuid",
    "user2Id": "user-b-uuid"
  }
}

// 2. Enviar mensaje directo
{
  "action": "enviarMensajeDirecto",
  "payload": {
    "remitenteId": "user-a-uuid",
    "destinatarioId": "user-b-uuid",
    "tipo": "texto",
    "contenido": "Hola! Cómo estás?"
  }
}

// 3. user-b recibe push
{
  "action": "push_newMessage",
  "data": { ... }
}
```

---

## Arquitectura del Sistema

### Descripción General
El servidor está construido con una arquitectura modular en capas usando Spring Framework y Maven:

**Capas principales:**
- **Vista**: Interfaz de usuario del servidor (ServerMainWindow)
- **Transporte**: Manejo de conexiones WebSocket y comunicación de red
  - `ClientHandler`: Gestiona conexiones de clientes
  - `PeerHandler`: Gestiona conexiones entre servidores P2P
  - `ServerListener`: Escucha conexiones entrantes
- **Controladores**: Procesan las peticiones y delegan a la lógica de negocio
  - `UserController`: Autenticación, registro, contactos
  - `ChannelController`: Gestión de canales y membresías
  - `MessageController`: Envío y recepción de mensajes
  - `FileController`: Subida/descarga de archivos
  - `PeerController`: Operaciones P2P
- **Negocio**: Lógica de negocio (fachadas y servicios)
- **Datos**: Persistencia con Hibernate/JPA y MySQL

**Tecnologías:**
- Java 21
- Spring Framework 6.2.11
- Hibernate 6.2.7
- MySQL 8.0
- WebSocket para comunicación en tiempo real
- Gson para serialización JSON

**Puertos:**
- Puerto principal del servidor: 22100 (configurable en `server.properties`)
- Puerto P2P: 22200 (configurable en `peer.server.port`)
- Base de datos MySQL: 3306

### Protocolo de Comunicación
El servidor usa WebSocket para comunicación bidireccional en tiempo real. Todas las peticiones y respuestas usan formato JSON.

**Flujo de comunicación:**
1. Cliente establece conexión WebSocket
2. Cliente envía petición JSON con `action` y `payload`
3. Servidor procesa mediante `RequestDispatcher` que delega a controladores
4. Servidor responde con JSON que incluye `action`, `status`, `message` y `data`
5. Servidor puede enviar notificaciones push proactivamente

---

## Versionado

**Versión de la API:** 1.0  
**Fecha de última actualización:** 7 de Noviembre, 2025  
**Compatibilidad:** Cliente v1.x

---

## Información de Contacto y Soporte

Para reportar problemas o sugerencias sobre la API, contactar al equipo de desarrollo.

**Repositorio del proyecto:** Chat Unillanos Server  
**Arquitectura:** Modular en capas con Spring Framework  
**Base de datos:** MySQL 8.0  
**Protocolo:** WebSocket con JSON

