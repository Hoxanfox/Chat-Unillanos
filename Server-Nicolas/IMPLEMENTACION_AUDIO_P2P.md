# Implementación de Transferencia de Audio entre Servidores P2P

## 📋 Descripción General

Este documento explica cómo funciona el sistema de transferencia de archivos de audio entre servidores P2P cuando se envían mensajes de voz. El desafío principal es que cuando un usuario envía un mensaje de audio, el archivo se guarda en su servidor local, pero los usuarios conectados a otros servidores necesitan acceso a ese archivo.

## 🔧 Componentes Implementados

### 1. **AudioFileP2PService** 
`negocio/server-LogicaPeers/src/main/java/com/arquitectura/logicaPeers/AudioFileP2PService.java`

Servicio principal que maneja la transferencia de archivos de audio usando chunks (piezas de 512KB).

#### Métodos principales:

- **`transferirArchivoAudio(UUID peerDestinoId, String rutaArchivoLocal)`**
  - Envía un archivo de audio a otro servidor
  - Divide el archivo en chunks de 512KB
  - Retorna la ruta donde el servidor destino guardó el archivo

- **`iniciarRecepcionAudio(String transferId, String fileName, long fileSize, int totalChunks, String originalPath)`**
  - Prepara el servidor para recibir un archivo de audio
  - Inicializa estructuras temporales para almacenar chunks

- **`recibirChunkAudio(String transferId, int chunkNumber, String chunkDataBase64)`**
  - Recibe y almacena un chunk individual del archivo
  - Los datos vienen codificados en Base64

- **`finalizarRecepcionAudio(String transferId)`**
  - Ensambla todos los chunks recibidos
  - Guarda el archivo completo en `storage/audio_files/`
  - Limpia las estructuras temporales

### 2. **PeerNotificationServiceImpl (Modificado)**
`negocio/server-LogicaPeers/src/main/java/com/arquitectura/logicaPeers/PeerNotificationServiceImpl.java`

Se agregó lógica para detectar mensajes de audio y transferir automáticamente el archivo antes de notificar al peer destino.

```java
@Override
public boolean notificarNuevoMensaje(UUID peerDestinoId, MessageResponseDto mensaje) {
    // Si es un mensaje de audio, transferir el archivo primero
    if ("AUDIO".equals(mensaje.getMessageType())) {
        String rutaArchivoLocal = mensaje.getContent();
        String rutaArchivoRemoto = audioFileP2PService.transferirArchivoAudio(
            peerDestinoId, rutaArchivoLocal);
        
        if (rutaArchivoRemoto == null) {
            return false; // Fallo en la transferencia
        }
        
        // Actualizar el content con la ruta remota
        content = rutaArchivoRemoto;
    }
    // ... continúa con la notificación normal
}
```

### 3. **P2PNotificationController (Actualizado)**
`transporte/server-controladorTransporte/src/main/java/com/arquitectura/controlador/controllers/P2PNotificationController.java`

Se agregaron nuevos handlers para manejar las acciones P2P de transferencia de audio:

- **`handleIniciarTransferenciaAudio`** - Inicia la recepción de un archivo
- **`handleRecibirChunkAudio`** - Recibe cada chunk del archivo
- **`handleFinalizarTransferenciaAudio`** - Ensambla y guarda el archivo completo

## 🔄 Flujo Completo de Transferencia

### Cuando un usuario envía un mensaje de audio:

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Cliente envía audio por chunks                              │
│    - startUpload                                                │
│    - uploadChunk (múltiples veces)                              │
│    - endUpload                                                  │
│    Resultado: audio_files/uuid_timestamp.wav                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. MessageServiceImpl.enviarMensajeAudio()                      │
│    - Guarda mensaje en BD con ruta del archivo                 │
│    - Publica evento NewMessageEvent (usuarios locales)          │
│    - Llama a notificarMensajeAPeers()                           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. PeerNotificationService.notificarNuevoMensaje()             │
│    - Detecta que es mensaje de AUDIO                            │
│    - Llama a AudioFileP2PService.transferirArchivoAudio()       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. AudioFileP2PService.transferirArchivoAudio()                │
│    a) Lee archivo local desde storage/                          │
│    b) Envía petición "iniciarTransferenciaAudio" al peer        │
│    c) Divide archivo en chunks de 512KB                         │
│    d) Envía cada chunk con "recibirChunkAudio"                  │
│    e) Envía petición "finalizarTransferenciaAudio"              │
│    f) Recibe ruta donde se guardó en servidor remoto            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. Servidor Destino (P2PNotificationController)                │
│    - Recibe inicio: prepara estructuras para chunks             │
│    - Recibe chunks: almacena temporalmente en memoria           │
│    - Recibe fin: ensambla archivo y guarda en storage/          │
│    - Retorna nueva ruta: audio_files/uuid_timestamp.wav         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. Notificación del Mensaje                                     │
│    - Se envía "notificarMensaje" con la ruta REMOTA            │
│    - Usuarios del servidor destino reciben el mensaje           │
│    - Pueden descargar el audio desde su servidor local          │
└─────────────────────────────────────────────────────────────────┘
```

## 📡 Protocolo de Comunicación P2P para Audio

### Acción: `iniciarTransferenciaAudio`

**Request:**
```json
{
  "action": "iniciarTransferenciaAudio",
  "payload": {
    "transferId": "uuid-random",
    "fileName": "uuid_timestamp.wav",
    "fileSize": 1048576,
    "totalChunks": 3,
    "originalPath": "audio_files/uuid_timestamp.wav"
  }
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Recepción iniciada",
  "data": null
}
```

### Acción: `recibirChunkAudio`

**Request:**
```json
{
  "action": "recibirChunkAudio",
  "payload": {
    "transferId": "uuid-random",
    "chunkNumber": 0,
    "chunkData": "base64EncodedData..."
  }
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Chunk recibido",
  "data": null
}
```

### Acción: `finalizarTransferenciaAudio`

**Request:**
```json
{
  "action": "finalizarTransferenciaAudio",
  "payload": {
    "transferId": "uuid-random"
  }
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Transferencia completada",
  "data": {
    "filePath": "audio_files/uuid_timestamp.wav"
  }
}
```

## ⚙️ Configuración

### Tamaño de Chunks
Por defecto: **512 KB** (512 * 1024 bytes)

Se puede ajustar en `AudioFileP2PService`:
```java
private static final int CHUNK_SIZE = 512 * 1024;
```

### Timeout de Conexión
Por defecto: **10 segundos**

```java
private static final int CONNECTION_TIMEOUT_MS = 10000;
```

## 🔍 Logs y Debugging

El sistema genera logs detallados para seguir el proceso:

```
→ [AudioFileP2PService] Iniciando transferencia de audio a peer abc-123: audio_files/file.wav
→ Transferencia iniciada, enviando 5 chunks...
  → Progreso: 10/50 chunks enviados
  → Progreso: 20/50 chunks enviados
  ...
✓ Transferencia completada exitosamente. Ruta remota: audio_files/file.wav
```

En el servidor receptor:
```
→ [P2PNotificationController] Iniciando recepción de audio:
  Transfer ID: transfer-uuid
  Archivo: file.wav
  Tamaño: 2621440 bytes
  Total chunks: 5
✓ [P2PNotificationController] Recepción de audio iniciada
  → Recibidos 10/50 chunks
  → Recibidos 20/50 chunks
  ...
→ Ensamblando archivo de 5 chunks...
✓ Archivo ensamblado y guardado en: audio_files/file.wav
```

## 🚨 Manejo de Errores

### Errores Comunes

1. **Archivo no encontrado**
   - El archivo de audio no existe en `storage/audio_files/`
   - Verificar que el path sea correcto

2. **Peer no disponible**
   - El servidor destino no responde
   - Verificar conectividad de red y estado del peer

3. **Chunks incompletos**
   - No se recibieron todos los chunks
   - Se limpia automáticamente y se retorna error

4. **Timeout de conexión**
   - La transferencia tomó más de 10 segundos
   - Considerar aumentar el timeout para archivos grandes

## 📊 Consideraciones de Rendimiento

### Ventajas del Sistema de Chunks:

1. **Memoria eficiente**: No carga archivos completos en memoria
2. **Recuperación ante fallos**: Se puede reintentar chunks individuales
3. **Progress tracking**: Se puede mostrar progreso de transferencia
4. **Compatible con archivos grandes**: Archivos de varios MB se manejan sin problemas

### Limitaciones Actuales:

1. **Almacenamiento en memoria temporal**: Los chunks se almacenan en HashMap en memoria
   - Para archivos muy grandes, considerar almacenamiento en disco

2. **Sin compresión**: Los archivos se envían sin comprimir
   - Se podría agregar compresión GZIP para reducir transferencia

3. **Transferencia síncrona**: La transferencia bloquea hasta completarse
   - Considerar hacer completamente asíncrona para mejor UX

## 🔐 Seguridad

### Medidas Implementadas:

1. **Validación de transferID**: Cada transferencia tiene un ID único
2. **Validación de chunks completos**: Se verifica que todos los chunks se recibieron
3. **Timeout de conexión**: Evita conexiones colgadas

### Mejoras Sugeridas:

1. **Autenticación entre peers**: Validar que el peer emisor es confiable
2. **Checksum de archivos**: Validar integridad con SHA-256
3. **Límite de tamaño**: Establecer tamaño máximo de archivos
4. **Rate limiting**: Evitar sobrecarga de transferencias simultáneas

## 🧪 Pruebas

### Escenario de Prueba 1: Mensaje de Audio Local

1. Usuario en Servidor A envía mensaje de audio a canal
2. Archivo se guarda en Servidor A: `storage/audio_files/file.wav`
3. Usuarios en Servidor A reciben mensaje inmediatamente
4. Usuarios en Servidor B deben recibir el archivo transferido

### Escenario de Prueba 2: Canal Cross-Server

1. Canal creado en Servidor A con usuarios de Servidor A y B
2. Usuario de Servidor A envía audio
3. Verificar que archivo se transfiere a Servidor B
4. Usuario de Servidor B puede descargar y reproducir audio

### Comandos de Verificación:

```bash
# Verificar archivos en servidor local
dir storage\audio_files

# Verificar logs de transferencia
# Buscar: "[AudioFileP2PService]" en los logs del servidor

# Verificar conexión entre peers
# Los logs mostrarán "✓ Transferencia completada exitosamente"
```

## 📚 Archivos Relacionados

- `AudioFileP2PService.java` - Servicio de transferencia
- `PeerNotificationServiceImpl.java` - Servicio de notificaciones (modificado)
- `P2PNotificationController.java` - Controlador P2P (actualizado)
- `MessageServiceImpl.java` - Servicio de mensajes (usa la transferencia)

## 🔄 Comparación: Cliente-Servidor vs P2P

### Cliente → Servidor (Ya funcionaba):
```
Cliente → uploadChunk → Servidor → storage/audio_files/
```

### Servidor → Servidor (Nueva implementación):
```
Servidor A → transferirArchivoAudio → Servidor B → storage/audio_files/
```

Ambos usan el mismo concepto de chunks, pero con diferentes protocolos:
- **Cliente-Servidor**: Usa `DTOUploadChunk` y `FileChunkManager`
- **Servidor-Servidor**: Usa `DTORequest/DTOResponse` y `AudioFileP2PService`

## ✅ Resumen

El sistema ahora soporta completamente la transferencia de archivos de audio entre servidores P2P:

1. ✅ Detección automática de mensajes de audio
2. ✅ Transferencia eficiente usando chunks de 512KB
3. ✅ Manejo de errores y timeouts
4. ✅ Logs detallados para debugging
5. ✅ Compatible con el sistema de chunks existente
6. ✅ Integración transparente con el flujo de mensajes

El usuario final no nota ninguna diferencia: simplemente envía un mensaje de audio y todos los usuarios (locales y remotos) lo reciben correctamente.

