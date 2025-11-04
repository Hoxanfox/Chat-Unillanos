# Documentación de rutas — GestionArchivos

Documento general sobre las rutas (acciones) que maneja `GestionArchivos` en el cliente. Describe los JSON de request/response, las acciones "push" dinámicas por chunk y las variantes (autenticada o no) de subida de archivos.

Ubicación del componente:
- Cliente: `Negocio/GestionArchivos/src/main/java/gestionArchivos/GestionArchivosImpl.java`

Resumen rápido
- Acciones de petición (cliente -> servidor):
  - `startFileUpload` (iniciar subida, normalmente autenticada)
  - `uploadFile` (inicio de subida en modo genérico; puede usarse sin auth según la API)
  - `uploadFileChunk` (envío de chunk)
  - `endFileUpload` (finalizar subida)
  - `startFileDownload` (iniciar descarga)
  - `requestFileChunk` (solicitar chunk de descarga)

- Push (servidor -> cliente) — respuestas:
  - Respuestas con la misma acción para inicio/finalización: `startFileUpload`, `uploadFile`, `endFileUpload`, `startFileDownload`.
  - Acciones dinámicas por chunk:
    - Upload ack: `uploadFileChunk_<uploadId>_<chunkNumber>`
    - Download chunk: `downloadFileChunk_<downloadId>_<chunkNumber>`

Formato general de envoltorio
- Request del cliente (envoltorio):
```json
{
  "action": "<accion>",
  "data": { /* payload específico */ }
}
```

- Response del servidor (envoltorio esperado):
```json
{
  "action": "<accion>",
  "success": true,
  "message": "mensaje opcional",
  "data": { /* payload específico o null */ }
}
```

Ejemplo de respuesta de error:
```json
{
  "action": "<accion>",
  "success": false,
  "message": "Descripción del error",
  "data": null
}
```

Detalles por acción (más general)

1) startFileUpload — iniciar subida (uso típico autenticado)
- Request
```json
{
  "action": "startFileUpload",
  "data": {
    "fileName": "miarchivo.pdf",
    "mimeType": "application/pdf",
    "totalChunks": 10
  }
}
```
- Response (push) — success
```json
{
  "action": "startFileUpload",
  "success": true,
  "message": null,
  "data": { "uploadId": "upload-uuid-1234" }
}
```
- Response (error)
```json
{
  "action": "startFileUpload",
  "success": false,
  "message": "Descripción del error",
  "data": null
}
```

2) uploadFile — inicio de subida en modo genérico (puede sustituir a una acción específica para registro)
- Request
```json
{
  "action": "uploadFile",
  "data": {
    "fileName": "avatar.png",
    "mimeType": "image/png",
    "totalChunks": 3
  }
}
```
- Response (push) — success
```json
{
  "action": "uploadFile",
  "success": true,
  "data": { "uploadId": "upload-uuid-reg-001" }
}
```

Nota: si la API del servidor define una ruta específica para subidas sin autenticación (por ejemplo `uploadFileForRegistration`), el cliente puede seguir usando esa acción; `uploadFile` aquí representa la opción genérica.

3) uploadFileChunk — enviar un chunk (petición genérica)
- Request
```json
{
  "action": "uploadFileChunk",
  "data": {
    "uploadId": "upload-uuid-1234",
    "chunkNumber": 1,
    "chunkDataBase64": "BASE64_DEL_CHUNK..."
  }
}
```
- Response (push) — ack (dinámico)
  - Patrón: `uploadFileChunk_<uploadId>_<chunkNumber>`
```json
{
  "action": "uploadFileChunk_upload-uuid-1234_1",
  "success": true,
  "message": null,
  "data": null
}
```
- Response (error)
```json
{
  "action": "uploadFileChunk_upload-uuid-1234_1",
  "success": false,
  "message": "Error al procesar chunk 1",
  "data": null
}
```

4) endFileUpload — finalizar subida
- Request
```json
{
  "action": "endFileUpload",
  "data": {
    "uploadId": "upload-uuid-1234",
    "hashSHA256": "hex-del-hash"
  }
}
```
- Response (push) — success (debe incluir `fileId` en data)
```json
{
  "action": "endFileUpload",
  "success": true,
  "message": null,
  "data": {
    "fileId": "server-file-id-789",
    "fileName": "miarchivo.pdf",
    "size": 1234567,
    "mimeType": "application/pdf",
    "hash": "hex-del-hash"
  }
}
```
- Response (error)
```json
{
  "action": "endFileUpload",
  "success": false,
  "message": "Error al finalizar",
  "data": null
}
```

5) startFileDownload — iniciar descarga
- Request
```json
{
  "action": "startFileDownload",
  "data": { "fileId": "server-file-id-789" }
}
```
- Response (push) — success (DTODownloadInfo)
```json
{
  "action": "startFileDownload",
  "success": true,
  "message": null,
  "data": {
    "downloadId": "download-uuid-456",
    "fileName": "miarchivo.pdf",
    "mimeType": "application/pdf",
    "fileSize": 1234567,
    "totalChunks": 10
  }
}
```

6) requestFileChunk — solicitar chunk para descarga
- Request
```json
{
  "action": "requestFileChunk",
  "data": {
    "downloadId": "download-uuid-456",
    "chunkNumber": 1
  }
}
```
- Response (push) — chunk (dinámico)
  - Patrón: `downloadFileChunk_<downloadId>_<chunkNumber>`
```json
{
  "action": "downloadFileChunk_download-uuid-456_1",
  "success": true,
  "message": null,
  "data": {
    "chunkNumber": 1,
    "chunkDataBase64": "BASE64_DEL_CHUNK..."
  }
}
```

Nota: La acción `solicitarHistorialPrivado` pertenece al módulo de mensajería (`Negocio/GestionContactos`) y no forma parte de la API de gestión de archivos documentada aquí. Si necesitas la documentación de la acción de historial, revisa:

- Implementación y uso: `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/GestionMensajesImpl.java`
- DTO petición: `Infraestructura/DTO/src/main/java/dto/comunicacion/peticion/mensaje/DTOSolicitarHistorial.java`
- DTO respuesta (historial): `Infraestructura/DTO/src/main/java/dto/comunicacion/respuesta/DTOHistorialMensajes.java`

Estructuras / DTOs (campos y tipos)
- DTOStartUpload
  - fileName: String
  - mimeType: String
  - totalChunks: int

- UploadIdResponse
  - uploadId: String

- DTOUploadChunk
  - uploadId: String
  - chunkNumber: int
  - chunkDataBase64: String

- DTOEndUpload
  - uploadId: String
  - hashSHA256: String

- FileUploadResponse (respuesta de `endFileUpload`)
  - fileId: String
  - fileName: String
  - size: long
  - mimeType: String
  - hash: String

- DTOStartDownload
  - fileId: String

- DTODownloadInfo
  - downloadId: String
  - fileName: String
  - mimeType: String
  - fileSize: long
  - totalChunks: int

- DTORequestChunk
  - downloadId: String
  - chunkNumber: int

- DTODownloadChunk
  - chunkNumber: int
  - chunkData: String (Base64)

Buenas prácticas y notas operativas
- Antes de enviar una petición por chunk se registra un manejador para la acción de push exacta (nombre completo). `GestionArchivosImpl` hace esto antes de enviar cada petición.
- Asegurar coherencia entre el campo `success` del servidor y la comprobación `DTOResponse.fueExitoso()` en el cliente.
- CHUNK_SIZE: en el código actual `CHUNK_SIZE` está declarado como `256` (comentario indica 1.5 MB). Revisar y ajustar a un valor realista si se desea optimizar (ej: 1_572_864 para 1.5MB).
- Para audio y otros binarios se envía contenido en Base64 dentro de `chunkDataBase64` o `chunkData`.
- `endFileUpload` debe devolver `fileId` — el cliente espera este valor para referencias futuras.

Flujos típicos (resumen)
- Subida (genérica):
  1. `startFileUpload` o `uploadFile` -> server responde con `uploadId`.
  2. Por cada chunk: `uploadFileChunk` -> server responde `uploadFileChunk_<uploadId>_<chunkNumber>` (ack).
  3. `endFileUpload` -> server responde con metadata incluyendo `fileId`.

- Descarga:
  1. `startFileDownload` -> server responde con `downloadId` y `totalChunks`.
  2. Por cada chunk: `requestFileChunk` -> server responde `downloadFileChunk_<downloadId>_<chunkNumber>` con `chunkDataBase64`.
  3. Cliente ensambla y guarda en caché/BD local.

Ejemplo rápido de uso (pseudocódigo cliente)
- Iniciar subida:
```text
uploadId = startFileUpload(fileName, mimeType, totalChunks)
for chunkNumber in 1..totalChunks:
  enviar uploadFileChunk(uploadId, chunkNumber, chunkBase64)
end
fileId = endFileUpload(uploadId, hash)
```

- Iniciar descarga:
```text
downloadInfo = startFileDownload(fileId)
for chunkNumber in 1..downloadInfo.totalChunks:
  chunk = requestFileChunk(downloadInfo.downloadId, chunkNumber)
  assemble chunk
end
```

Contacto
- Archivo generado automáticamente: `Negocio/GestionArchivos/DOCUMENTACION_RUTAS.md`.
- Si quieres puedo también: añadir este archivo al control de versiones y crear un commit con un mensaje sugerido, o generar una versión `README.md` en la raíz del componente en lugar de `DOCUMENTACION_RUTAS.md`.

---
Generado el: 2025-11-04
