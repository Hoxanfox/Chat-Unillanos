# Protocolo JSON: Gestión de Archivos (Subida y Descarga)

## 📤 SUBIDA DE ARCHIVOS (Upload)

### 1. Iniciar Subida - `startFileUpload`

**Petición del Cliente:**
```json
{
  "action": "startFileUpload",
  "data": {
    "fileName": "documento.pdf",
    "mimeType": "application/pdf",
    "totalChunks": 5
  }
}
```

**Respuesta del Servidor:**
```json
{
  "success": true,
  "message": "Upload iniciado",
  "data": {
    "uploadId": "upload-123e4567-e89b-12d3-a456-426614174000"
  }
}
```

**Respuesta de Error:**
```json
{
  "success": false,
  "message": "Archivo demasiado grande",
  "data": null
}
```

---

### 2. Enviar Chunk - `uploadFileChunk`

**Petición del Cliente:**
```json
{
  "action": "uploadFileChunk",
  "data": {
    "uploadId": "upload-123e4567-e89b-12d3-a456-426614174000",
    "chunkNumber": 1,
    "chunkData": "JVBERi0xLjQKJeLjz9MKMyAwIG9iago8PC9UeXBlIC9QYWdlCi9QYXJl..."
  }
}
```

**Nota:** `chunkData` es el contenido del chunk codificado en Base64

**Respuesta del Servidor:**
```json
{
  "success": true,
  "message": "Chunk 1 recibido correctamente",
  "data": {
    "uploadId": "upload-123e4567-e89b-12d3-a456-426614174000",
    "chunkNumber": 1,
    "received": true
  }
}
```

**Respuesta de Error:**
```json
{
  "success": false,
  "message": "Chunk corrupto o inválido",
  "data": null
}
```

---

### 3. Finalizar Subida - `endFileUpload`

**Petición del Cliente:**
```json
{
  "action": "endFileUpload",
  "data": {
    "uploadId": "upload-123e4567-e89b-12d3-a456-426614174000",
    "fileHash": "a3d5c7b9e1f2..."
  }
}
```

**Respuesta del Servidor:**
```json
{
  "success": true,
  "message": "Archivo guardado exitosamente",
  "data": {
    "fileName": "file-789abc-documento.pdf",
    "fileId": "file-789abc-def012-345678",
    "fileUrl": "/files/file-789abc-documento.pdf",
    "fileSize": 1572864
  }
}
```

**Respuesta de Error:**
```json
{
  "success": false,
  "message": "Hash no coincide. Archivo corrupto.",
  "data": null
}
```

---

### 4. Subida para Registro (Sin Autenticación) - `uploadFileForRegistration`

**Petición del Cliente:**
```json
{
  "action": "uploadFileForRegistration",
  "data": {
    "fileName": "foto_perfil.jpg",
    "mimeType": "image/jpeg",
    "totalChunks": 2
  }
}
```

**Respuesta del Servidor:**
```json
{
  "success": true,
  "message": "Upload para registro iniciado",
  "data": {
    "uploadId": "upload-reg-123e4567-e89b-12d3-a456-426614174000"
  }
}
```

---

## 📥 DESCARGA DE ARCHIVOS (Download)

### 1. Iniciar Descarga - `startFileDownload`

**Petición del Cliente:**
```json
{
  "action": "startFileDownload",
  "data": {
    "fileId": "file-789abc-def012-345678"
  }
}
```

**Respuesta del Servidor:**
```json
{
  "success": true,
  "message": "Descarga iniciada",
  "data": {
    "downloadId": "download-456def-789ghi-012jkl",
    "fileName": "documento.pdf",
    "fileSize": 1572864,
    "totalChunks": 5,
    "mimeType": "application/pdf"
  }
}
```

**Respuesta de Error:**
```json
{
  "success": false,
  "message": "Archivo no encontrado",
  "data": null
}
```

---

### 2. Solicitar Chunk - `requestFileChunk`

**Petición del Cliente:**
```json
{
  "action": "requestFileChunk",
  "data": {
    "downloadId": "download-456def-789ghi-012jkl",
    "chunkNumber": 1
  }
}
```

**Respuesta del Servidor:**
```json
{
  "success": true,
  "message": "Chunk 1 enviado",
  "data": {
    "downloadId": "download-456def-789ghi-012jkl",
    "chunkNumber": 1,
    "chunkData": "JVBERi0xLjQKJeLjz9MKMyAwIG9iago8PC9UeXBlIC9QYWdlCi9QYXJl...",
    "isLast": false
  }
}
```

**Último Chunk:**
```json
{
  "success": true,
  "message": "Chunk 5 enviado (último)",
  "data": {
    "downloadId": "download-456def-789ghi-012jkl",
    "chunkNumber": 5,
    "chunkData": "dGVzdCBkYXRhIGZvciB0aGUgbGFzdCBjaHVuaw==",
    "isLast": true
  }
}
```

**Respuesta de Error:**
```json
{
  "success": false,
  "message": "Chunk no disponible",
  "data": null
}
```

---

## 📋 CASOS DE USO COMPLETOS

### Caso 1: Subir un archivo de 2 MB (2 chunks)

**Flujo completo:**

```json
// 1. Iniciar subida
Cliente → Servidor: {
  "action": "startFileUpload",
  "data": {
    "fileName": "reporte.pdf",
    "mimeType": "application/pdf",
    "totalChunks": 2
  }
}

Servidor → Cliente: {
  "success": true,
  "data": {
    "uploadId": "upload-abc123"
  }
}

// 2. Enviar chunk 1
Cliente → Servidor: {
  "action": "uploadFileChunk",
  "data": {
    "uploadId": "upload-abc123",
    "chunkNumber": 1,
    "chunkData": "JVBERi0x..."
  }
}

Servidor → Cliente: {
  "success": true,
  "message": "Chunk 1 recibido"
}

// 3. Enviar chunk 2
Cliente → Servidor: {
  "action": "uploadFileChunk",
  "data": {
    "uploadId": "upload-abc123",
    "chunkNumber": 2,
    "chunkData": "dGVzdCBk..."
  }
}

Servidor → Cliente: {
  "success": true,
  "message": "Chunk 2 recibido"
}

// 4. Finalizar subida
Cliente → Servidor: {
  "action": "endFileUpload",
  "data": {
    "uploadId": "upload-abc123",
    "fileHash": "a3d5c7b9e1f2..."
  }
}

Servidor → Cliente: {
  "success": true,
  "data": {
    "fileName": "file-xyz789-reporte.pdf",
    "fileId": "file-xyz789",
    "fileSize": 2097152
  }
}
```

---

### Caso 2: Descargar un archivo de 1.5 MB (1 chunk)

**Flujo completo:**

```json
// 1. Iniciar descarga
Cliente → Servidor: {
  "action": "startFileDownload",
  "data": {
    "fileId": "file-xyz789"
  }
}

Servidor → Cliente: {
  "success": true,
  "data": {
    "downloadId": "download-def456",
    "fileName": "reporte.pdf",
    "fileSize": 1572864,
    "totalChunks": 1,
    "mimeType": "application/pdf"
  }
}

// 2. Solicitar chunk 1 (único)
Cliente → Servidor: {
  "action": "requestFileChunk",
  "data": {
    "downloadId": "download-def456",
    "chunkNumber": 1
  }
}

Servidor → Cliente: {
  "success": true,
  "data": {
    "downloadId": "download-def456",
    "chunkNumber": 1,
    "chunkData": "JVBERi0xLjQKJeLjz9MK...",
    "isLast": true
  }
}
```

---

## 🔐 NOTAS IMPORTANTES

### Tamaño de Chunks
- **Tamaño por defecto**: 1.5 MB (1,572,864 bytes)
- **Codificación**: Base64 (aumenta ~33% el tamaño)
- **Chunk en Base64**: ~2 MB aproximadamente

### IDs del Servidor
- ✅ **uploadId**: Generado por el servidor al iniciar subida
- ✅ **downloadId**: Generado por el servidor al iniciar descarga
- ✅ **fileId**: ID permanente del archivo, generado por el servidor
- ⚠️ El cliente **NUNCA** genera estos IDs, siempre los recibe del servidor

### Formato de IDs
```
uploadId:   "upload-{UUID}"
downloadId: "download-{UUID}"
fileId:     "file-{UUID}" o "file-{hash}-{nombre}"
```

### Validación de Chunks
- El servidor valida que los chunks lleguen en orden
- Se calcula un hash SHA-256 del archivo completo
- Si el hash no coincide, se rechaza la subida

### Manejo de Errores Comunes

**Cliente no autenticado (para `startFileUpload`):**
```json
{
  "success": false,
  "message": "No autorizado. Inicie sesión primero.",
  "data": null
}
```

**Chunk fuera de orden:**
```json
{
  "success": false,
  "message": "Esperaba chunk 2, recibió chunk 3",
  "data": null
}
```

**Archivo ya existe:**
```json
{
  "success": false,
  "message": "El archivo ya existe en el servidor",
  "data": {
    "existingFileId": "file-xyz789"
  }
}
```

---

## 🎯 Mejores Prácticas

1. **Reintento automático**: Si un chunk falla, reintentarlo máximo 3 veces
2. **Timeout**: Esperar máximo 30 segundos por chunk
3. **Validación local**: Calcular el hash antes de enviar
4. **Progreso**: Notificar al usuario el % completado
5. **Cancelación**: Implementar un mecanismo para cancelar uploads largos

---

## 📊 Ejemplo de Monitoreo de Progreso

```javascript
// Progreso de subida
{
  "uploadId": "upload-abc123",
  "totalChunks": 10,
  "completedChunks": 7,
  "progress": 70,
  "status": "uploading"
}

// Progreso de descarga
{
  "downloadId": "download-def456",
  "totalChunks": 5,
  "receivedChunks": 3,
  "progress": 60,
  "status": "downloading"
}
```

---

## 🚀 Acciones Registradas en el Cliente

### Para Subida:
- `startFileUpload` → Respuesta con uploadId
- `uploadFileChunk_${uploadId}_${chunkNumber}` → Confirmación del chunk
- `endFileUpload` → Respuesta con fileId final

### Para Descarga:
- `startFileDownload` → Respuesta con downloadInfo
- `downloadFileChunk_${downloadId}_${chunkNumber}` → Chunk data

---

## 📝 Notas de Implementación

- Los IDs son generados por el **servidor** para evitar colisiones
- El cliente almacena los IDs recibidos del servidor
- La base de datos local guarda el `fileId` del servidor
- Para fotos de perfil en registro, se usa `uploadFileForRegistration`
- El servidor debe mantener sesiones de upload/download temporales

