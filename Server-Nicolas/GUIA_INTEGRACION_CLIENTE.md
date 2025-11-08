# 🔌 Guía de Integración del Cliente - Chat Unillanos

## Índice
1. [Conexión WebSocket](#conexión-websocket)
2. [Autenticación](#autenticación)
3. [Envío de Mensajes](#envío-de-mensajes)
4. [Manejo de Notificaciones Push](#manejo-de-notificaciones-push)
5. [Gestión de Archivos](#gestión-de-archivos)
6. [Ejemplos de Código](#ejemplos-de-código)
7. [Mejores Prácticas](#mejores-prácticas)

---

## Conexión WebSocket

### Establecer Conexión

**JavaScript/TypeScript:**
```javascript
const socket = new WebSocket('ws://localhost:22100');

socket.onopen = () => {
  console.log('Conectado al servidor');
};

socket.onmessage = (event) => {
  const response = JSON.parse(event.data);
  handleServerResponse(response);
};

socket.onerror = (error) => {
  console.error('Error de conexión:', error);
};

socket.onclose = () => {
  console.log('Desconectado del servidor');
  // Intentar reconexión
  setTimeout(() => reconnect(), 5000);
};
```

**Python:**
```python
import websocket
import json

def on_message(ws, message):
    response = json.loads(message)
    handle_server_response(response)

def on_error(ws, error):
    print(f"Error: {error}")

def on_close(ws):
    print("Conexión cerrada")

def on_open(ws):
    print("Conectado al servidor")

ws = websocket.WebSocketApp(
    "ws://localhost:22100",
    on_message=on_message,
    on_error=on_error,
    on_close=on_close,
    on_open=on_open
)

ws.run_forever()
```

**Java:**
```java
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.google.gson.Gson;

public class ChatClient extends WebSocketClient {
    private Gson gson = new Gson();
    
    public ChatClient(URI serverUri) {
        super(serverUri);
    }
    
    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Conectado al servidor");
    }
    
    @Override
    public void onMessage(String message) {
        Response response = gson.fromJson(message, Response.class);
        handleServerResponse(response);
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Desconectado: " + reason);
    }
    
    @Override
    public void onError(Exception ex) {
        System.err.println("Error: " + ex.getMessage());
    }
}
```

---

## Autenticación

### Flujo de Autenticación

```
1. Cliente → Servidor: authenticateUser
2. Servidor → Cliente: Respuesta con userId y datos
3. Cliente guarda sesión
4. Cliente puede realizar operaciones autenticadas
```

### Ejemplo: Login

**Request:**
```javascript
function login(username, password) {
  const request = {
    action: "authenticateUser",
    payload: {
      nombreUsuario: username,
      password: password
    }
  };
  
  socket.send(JSON.stringify(request));
}

// Uso
login("juan", "123456");
```

**Response esperada:**
```json
{
  "action": "authenticateUser",
  "status": "success",
  "message": "Autenticación exitosa",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "juan",
    "email": "juan@example.com",
    "fileId": "user_photos/juan.jpg"
  }
}
```

**Manejo de respuesta:**
```javascript
function handleServerResponse(response) {
  if (response.action === "authenticateUser") {
    if (response.status === "success") {
      // Guardar sesión
      localStorage.setItem('userId', response.data.userId);
      localStorage.setItem('username', response.data.nombre);
      
      // Redirigir a pantalla principal
      navigateToMainScreen();
    } else {
      // Mostrar error
      showError(response.message);
    }
  }
}
```

### Ejemplo: Registro

```javascript
function register(username, email, password, photoFileId = null) {
  const request = {
    action: "registerUser",
    payload: {
      username: username,
      email: email,
      password: password,
      photoFileId: photoFileId
    }
  };
  
  socket.send(JSON.stringify(request));
}

// Uso
register("maria", "maria@example.com", "password123");
```

### Ejemplo: Logout

```javascript
function logout(userId) {
  const request = {
    action: "logoutUser",
    payload: {
      userId: userId
    }
  };
  
  socket.send(JSON.stringify(request));
  
  // Limpiar sesión local
  localStorage.clear();
}
```

---

## Envío de Mensajes

### Mensaje de Texto en Canal

```javascript
function sendTextMessage(channelId, content) {
  const request = {
    action: "enviarMensajeCanal",
    payload: {
      canalId: channelId,
      contenido: content
    }
  };
  
  socket.send(JSON.stringify(request));
}

// Uso
sendTextMessage("660e8400-e29b-41d4-a716-446655440000", "Hola a todos!");
```

### Mensaje de Audio en Canal

```javascript
async function sendAudioMessage(channelId, audioBlob) {
  // Convertir audio a Base64
  const base64Audio = await blobToBase64(audioBlob);
  
  const request = {
    action: "enviarMensajeAudio",
    payload: {
      canalId: channelId,
      audioBase64: base64Audio,
      duration: audioBlob.duration || 0,
      format: "webm"
    }
  };
  
  socket.send(JSON.stringify(request));
}

// Helper para convertir Blob a Base64
function blobToBase64(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => {
      const base64 = reader.result.split(',')[1];
      resolve(base64);
    };
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}
```

### Mensaje Directo (Chat Privado)

```javascript
function sendDirectMessage(recipientId, content) {
  const userId = localStorage.getItem('userId');
  
  const request = {
    action: "enviarMensajeDirecto",
    payload: {
      remitenteId: userId,
      destinatarioId: recipientId,
      tipo: "texto",
      contenido: content
    }
  };
  
  socket.send(JSON.stringify(request));
}

// Uso
sendDirectMessage("770e8400-e29b-41d4-a716-446655440000", "Hola, ¿cómo estás?");
```

---

## Manejo de Notificaciones Push

### Estructura de Push

Todas las notificaciones push tienen el formato:
```json
{
  "action": "push_{tipoDeEvento}",
  "status": true,
  "message": "Descripción",
  "data": { ... }
}
```

### Implementación de Listener

```javascript
function handleServerResponse(response) {
  // Detectar si es un push
  if (response.action && response.action.startsWith('push_')) {
    handlePushNotification(response);
    return;
  }
  
  // Manejar respuestas normales
  handleNormalResponse(response);
}

function handlePushNotification(push) {
  const eventType = push.action.replace('push_', '');
  
  switch(eventType) {
    case 'newMessage':
      handleNewMessage(push.data);
      break;
      
    case 'userInvited':
      handleUserInvited(push.data);
      break;
      
    case 'contactListUpdate':
      handleContactListUpdate(push.data);
      break;
      
    case 'forceLogout':
      handleForceLogout(push.data);
      break;
      
    default:
      console.log('Push desconocido:', eventType);
  }
}
```

### Ejemplo: Nuevo Mensaje

```javascript
function handleNewMessage(data) {
  const { messageId, channelId, author, content, messageType, timestamp } = data;
  
  // Actualizar UI del canal
  if (currentChannelId === channelId) {
    appendMessageToChat({
      id: messageId,
      author: author.username,
      content: content,
      type: messageType,
      time: new Date(timestamp)
    });
  }
  
  // Mostrar notificación
  if (messageType === 'TEXT') {
    showNotification(`${author.username}: ${content}`);
  } else {
    showNotification(`${author.username} envió un audio`);
  }
  
  // Reproducir sonido
  playNotificationSound();
  
  // Actualizar badge de mensajes no leídos
  incrementUnreadCount(channelId);
}
```

### Ejemplo: Invitación a Canal

```javascript
function handleUserInvited(data) {
  const { channelId, channelName, owner } = data;
  
  // Mostrar notificación
  showNotification(`${owner.username} te invitó a ${channelName}`);
  
  // Actualizar lista de invitaciones
  refreshInvitationsList();
  
  // Mostrar badge de invitaciones pendientes
  updateInvitationsBadge();
}
```

### Ejemplo: Actualización de Contactos

```javascript
function handleContactListUpdate(data) {
  if (data.shouldRefresh) {
    // Solicitar lista actualizada de contactos
    requestContactList();
  }
}

function requestContactList() {
  const userId = localStorage.getItem('userId');
  
  const request = {
    action: "listarContactos",
    payload: {
      usuarioId: userId
    }
  };
  
  socket.send(JSON.stringify(request));
}
```

### Ejemplo: Logout Forzado

```javascript
function handleForceLogout(data) {
  const { motivo } = data;
  
  // Mostrar mensaje al usuario
  alert(`Tu sesión ha sido cerrada: ${motivo}`);
  
  // Limpiar sesión
  localStorage.clear();
  
  // Cerrar conexión
  socket.close();
  
  // Redirigir a login
  window.location.href = '/login';
}
```

---

## Gestión de Archivos

### Subida de Archivo por Chunks

```javascript
class FileUploader {
  constructor(socket, file, chunkSize = 64 * 1024) {
    this.socket = socket;
    this.file = file;
    this.chunkSize = chunkSize;
    this.uploadId = null;
    this.currentChunk = 0;
    this.totalChunks = Math.ceil(file.size / chunkSize);
  }
  
  async start() {
    // 1. Iniciar upload
    const startRequest = {
      action: "startFileUpload",
      payload: {
        fileName: this.file.name,
        fileSize: this.file.size,
        chunkSize: this.chunkSize,
        mimeType: this.file.type
      }
    };
    
    this.socket.send(JSON.stringify(startRequest));
    
    // Esperar respuesta con uploadId
    return new Promise((resolve, reject) => {
      this.onStartCallback = (uploadId) => {
        this.uploadId = uploadId;
        this.uploadNextChunk();
        resolve();
      };
    });
  }
  
  async uploadNextChunk() {
    if (this.currentChunk >= this.totalChunks) {
      this.finalize();
      return;
    }
    
    // Leer chunk del archivo
    const start = this.currentChunk * this.chunkSize;
    const end = Math.min(start + this.chunkSize, this.file.size);
    const chunk = this.file.slice(start, end);
    
    // Convertir a Base64
    const base64Chunk = await this.blobToBase64(chunk);
    
    // Enviar chunk
    const chunkRequest = {
      action: "uploadFileChunk",
      payload: {
        uploadId: this.uploadId,
        chunkNumber: this.currentChunk,
        chunkDataBase64: base64Chunk
      }
    };
    
    this.socket.send(JSON.stringify(chunkRequest));
    
    // Actualizar progreso
    const progress = ((this.currentChunk + 1) / this.totalChunks) * 100;
    this.onProgress(progress);
    
    this.currentChunk++;
  }
  
  finalize() {
    const endRequest = {
      action: "endFileUpload",
      payload: {
        uploadId: this.uploadId
      }
    };
    
    this.socket.send(JSON.stringify(endRequest));
  }
  
  async blobToBase64(blob) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onloadend = () => {
        const base64 = reader.result.split(',')[1];
        resolve(base64);
      };
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  }
  
  onProgress(percent) {
    console.log(`Upload progress: ${percent.toFixed(2)}%`);
  }
}

// Uso
const fileInput = document.getElementById('fileInput');
fileInput.addEventListener('change', async (e) => {
  const file = e.target.files[0];
  const uploader = new FileUploader(socket, file);
  
  uploader.onProgress = (percent) => {
    updateProgressBar(percent);
  };
  
  await uploader.start();
});

// Manejar respuestas del servidor
function handleServerResponse(response) {
  if (response.action === "startFileUpload") {
    uploader.onStartCallback(response.data.uploadId);
  }
  
  if (response.action.startsWith("uploadFileChunk_")) {
    // ACK recibido, enviar siguiente chunk
    uploader.uploadNextChunk();
  }
  
  if (response.action === "endFileUpload") {
    const fileId = response.data.fileId;
    console.log("Archivo subido:", fileId);
    // Usar fileId para enviar mensaje o actualizar perfil
  }
}
```

### Descarga de Archivo por Chunks

```javascript
class FileDownloader {
  constructor(socket, fileId) {
    this.socket = socket;
    this.fileId = fileId;
    this.downloadId = null;
    this.chunks = [];
    this.totalChunks = 0;
  }
  
  start() {
    const startRequest = {
      action: "startFileDownload",
      payload: {
        fileId: this.fileId
      }
    };
    
    this.socket.send(JSON.stringify(startRequest));
  }
  
  onStartResponse(data) {
    this.downloadId = data.downloadId;
    this.totalChunks = data.totalChunks;
    this.chunks = new Array(this.totalChunks);
    
    // Solicitar todos los chunks
    for (let i = 0; i < this.totalChunks; i++) {
      this.requestChunk(i);
    }
  }
  
  requestChunk(chunkNumber) {
    const request = {
      action: "requestFileChunk",
      payload: {
        downloadId: this.downloadId,
        chunkNumber: chunkNumber
      }
    };
    
    this.socket.send(JSON.stringify(request));
  }
  
  onChunkReceived(chunkNumber, chunkDataBase64) {
    this.chunks[chunkNumber] = chunkDataBase64;
    
    // Verificar si todos los chunks fueron recibidos
    if (this.chunks.every(chunk => chunk !== undefined)) {
      this.assembleFile();
    }
  }
  
  assembleFile() {
    // Concatenar todos los chunks
    const fullBase64 = this.chunks.join('');
    
    // Convertir Base64 a Blob
    const byteCharacters = atob(fullBase64);
    const byteNumbers = new Array(byteCharacters.length);
    
    for (let i = 0; i < byteCharacters.length; i++) {
      byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray]);
    
    // Crear URL para descarga
    const url = URL.createObjectURL(blob);
    
    // Descargar archivo
    const a = document.createElement('a');
    a.href = url;
    a.download = this.fileId;
    a.click();
    
    URL.revokeObjectURL(url);
  }
}

// Uso
function downloadFile(fileId) {
  const downloader = new FileDownloader(socket, fileId);
  downloader.start();
}

// Manejar respuestas
function handleServerResponse(response) {
  if (response.action === "startFileDownload") {
    downloader.onStartResponse(response.data);
  }
  
  if (response.action.startsWith("downloadFileChunk_")) {
    const parts = response.action.split('_');
    const chunkNumber = parseInt(parts[2]);
    downloader.onChunkReceived(chunkNumber, response.data.chunkDataBase64);
  }
}
```

---

## Ejemplos de Código

### Cliente Completo en JavaScript

```javascript
class ChatClient {
  constructor(serverUrl) {
    this.serverUrl = serverUrl;
    this.socket = null;
    this.userId = null;
    this.handlers = {};
  }
  
  connect() {
    return new Promise((resolve, reject) => {
      this.socket = new WebSocket(this.serverUrl);
      
      this.socket.onopen = () => {
        console.log('Conectado');
        resolve();
      };
      
      this.socket.onmessage = (event) => {
        const response = JSON.parse(event.data);
        this.handleResponse(response);
      };
      
      this.socket.onerror = (error) => {
        console.error('Error:', error);
        reject(error);
      };
      
      this.socket.onclose = () => {
        console.log('Desconectado');
        setTimeout(() => this.connect(), 5000);
      };
    });
  }
  
  send(action, payload) {
    const request = { action, payload };
    this.socket.send(JSON.stringify(request));
  }
  
  on(action, handler) {
    this.handlers[action] = handler;
  }
  
  handleResponse(response) {
    const handler = this.handlers[response.action];
    if (handler) {
      handler(response);
    }
  }
  
  // Métodos de API
  async login(username, password) {
    return new Promise((resolve, reject) => {
      this.on('authenticateUser', (response) => {
        if (response.status === 'success') {
          this.userId = response.data.userId;
          resolve(response.data);
        } else {
          reject(new Error(response.message));
        }
      });
      
      this.send('authenticateUser', {
        nombreUsuario: username,
        password: password
      });
    });
  }
  
  sendMessage(channelId, content) {
    this.send('enviarMensajeCanal', {
      canalId: channelId,
      contenido: content
    });
  }
  
  getChannels() {
    return new Promise((resolve) => {
      this.on('listarCanales', (response) => {
        if (response.status === 'success') {
          resolve(response.data);
        }
      });
      
      this.send('listarCanales', {
        usuarioId: this.userId
      });
    });
  }
}

// Uso
const client = new ChatClient('ws://localhost:22100');

async function main() {
  await client.connect();
  
  // Login
  const user = await client.login('juan', '123456');
  console.log('Usuario:', user);
  
  // Obtener canales
  const channels = await client.getChannels();
  console.log('Canales:', channels);
  
  // Enviar mensaje
  client.sendMessage(channels[0].idCanal, 'Hola!');
  
  // Escuchar nuevos mensajes
  client.on('push_newMessage', (push) => {
    console.log('Nuevo mensaje:', push.data);
  });
}

main();
```

---

## Mejores Prácticas

### 1. Manejo de Reconexión

```javascript
class ReconnectingWebSocket {
  constructor(url, maxRetries = 5) {
    this.url = url;
    this.maxRetries = maxRetries;
    this.retries = 0;
    this.connect();
  }
  
  connect() {
    this.socket = new WebSocket(this.url);
    
    this.socket.onopen = () => {
      console.log('Conectado');
      this.retries = 0;
      this.onopen && this.onopen();
    };
    
    this.socket.onclose = () => {
      if (this.retries < this.maxRetries) {
        const delay = Math.min(1000 * Math.pow(2, this.retries), 30000);
        console.log(`Reconectando en ${delay}ms...`);
        setTimeout(() => {
          this.retries++;
          this.connect();
        }, delay);
      } else {
        console.error('Máximo de reintentos alcanzado');
        this.onerror && this.onerror(new Error('No se pudo reconectar'));
      }
    };
    
    this.socket.onmessage = (event) => {
      this.onmessage && this.onmessage(event);
    };
  }
  
  send(data) {
    if (this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(data);
    } else {
      console.warn('Socket no está abierto');
    }
  }
}
```

### 2. Cola de Mensajes

```javascript
class MessageQueue {
  constructor(socket) {
    this.socket = socket;
    this.queue = [];
    this.processing = false;
  }
  
  enqueue(message) {
    this.queue.push(message);
    this.process();
  }
  
  async process() {
    if (this.processing || this.queue.length === 0) {
      return;
    }
    
    this.processing = true;
    
    while (this.queue.length > 0) {
      const message = this.queue.shift();
      this.socket.send(JSON.stringify(message));
      
      // Pequeño delay para no saturar
      await new Promise(resolve => setTimeout(resolve, 10));
    }
    
    this.processing = false;
  }
}
```

### 3. Caché de Datos

```javascript
class DataCache {
  constructor(ttl = 60000) {
    this.cache = new Map();
    this.ttl = ttl;
  }
  
  set(key, value) {
    this.cache.set(key, {
      value: value,
      timestamp: Date.now()
    });
  }
  
  get(key) {
    const item = this.cache.get(key);
    
    if (!item) {
      return null;
    }
    
    if (Date.now() - item.timestamp > this.ttl) {
      this.cache.delete(key);
      return null;
    }
    
    return item.value;
  }
  
  clear() {
    this.cache.clear();
  }
}

// Uso
const cache = new DataCache(60000); // 1 minuto

function getChannels() {
  const cached = cache.get('channels');
  if (cached) {
    return Promise.resolve(cached);
  }
  
  return fetchChannelsFromServer().then(channels => {
    cache.set('channels', channels);
    return channels;
  });
}
```

### 4. Validación de Datos

```javascript
function validateMessage(content) {
  if (!content || content.trim().length === 0) {
    throw new Error('El mensaje no puede estar vacío');
  }
  
  if (content.length > 5000) {
    throw new Error('El mensaje es demasiado largo (máximo 5000 caracteres)');
  }
  
  return content.trim();
}

function validateEmail(email) {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!regex.test(email)) {
    throw new Error('Email inválido');
  }
  return email;
}
```

### 5. Manejo de Errores

```javascript
function handleError(error, context) {
  console.error(`Error en ${context}:`, error);
  
  // Mostrar mensaje al usuario
  if (error.message) {
    showUserError(error.message);
  } else {
    showUserError('Ocurrió un error inesperado');
  }
  
  // Reportar a servicio de logging
  logError({
    context: context,
    error: error.toString(),
    stack: error.stack,
    timestamp: new Date().toISOString()
  });
}

// Uso
try {
  const content = validateMessage(userInput);
  sendMessage(channelId, content);
} catch (error) {
  handleError(error, 'sendMessage');
}
```

---

## Checklist de Integración

- [ ] Establecer conexión WebSocket
- [ ] Implementar autenticación (login/registro)
- [ ] Manejar respuestas del servidor
- [ ] Implementar listeners de push
- [ ] Enviar mensajes de texto
- [ ] Enviar mensajes de audio
- [ ] Listar canales
- [ ] Crear canales directos
- [ ] Obtener historial de mensajes
- [ ] Subir archivos por chunks
- [ ] Descargar archivos por chunks
- [ ] Implementar reconexión automática
- [ ] Manejar errores gracefully
- [ ] Implementar caché de datos
- [ ] Validar datos antes de enviar
- [ ] Mostrar notificaciones al usuario
- [ ] Actualizar UI en tiempo real

---

## Recursos Adicionales

- [Documentación de API](DOCUMENTACION_API.md)
- [Arquitectura del Servidor](ARQUITECTURA_SERVIDOR.md)
- [WebSocket API - MDN](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [Gson Documentation](https://github.com/google/gson)
