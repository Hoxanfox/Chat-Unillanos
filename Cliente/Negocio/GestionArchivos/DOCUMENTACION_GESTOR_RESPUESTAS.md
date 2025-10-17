# DOCUMENTACIÓN: Sistema de Gestión de JSON - Peticiones y Respuestas

## Tabla de Contenidos
1. [Arquitectura del Sistema](#1-arquitectura-del-sistema)
2. [Formato de Mensajes JSON](#2-formato-de-mensajes-json)
3. [Catálogo de Acciones](#3-catálogo-de-acciones)
4. [Flujo de Comunicación](#4-flujo-de-comunicación)
5. [Diagnóstico de Errores](#5-diagnóstico-de-errores)
6. [Guía de Soluciones](#6-guía-de-soluciones)
7. [Ejemplos Completos](#7-ejemplos-completos)
8. [Mejores Prácticas](#8-mejores-prácticas)

---

## 1. Arquitectura del Sistema

### 1.1 Componentes Principales

#### **GestorRespuesta** (Cliente)
- **Tipo**: Singleton
- **Responsabilidad**: Procesar respuestas JSON del servidor
- **Funcionamiento**:
  1. Lee líneas JSON desde el `BufferedReader` de la sesión activa
  2. Deserializa cada línea a un objeto `DTOResponse`
  3. Busca un manejador registrado usando `response.getAction()` como clave
  4. Invoca el manejador pasándole el `DTOResponse`
  5. Si no existe manejador: registra `"No se encontró un manejador para la acción: <action>"`

**Implicación crítica**: El string `action` en la respuesta del servidor debe coincidir exactamente con la clave registrada en el mapa de manejadores del cliente.

#### **EnviadorPeticiones** (Cliente)
- **Responsabilidad**: Enviar peticiones JSON al servidor
- **Funcionamiento**:
  1. Serializa objetos `DTORequest` a JSON
  2. Envía la línea JSON a través del `BufferedWriter` de la sesión
  3. Registra en stdout: `">> Petición enviada: <json>"`

#### **DTOs de Transferencia**
- `DTORequest`: Estructura de peticiones (action + payload)
- `DTOResponse`: Estructura de respuestas (action + status + message + data)
- DTOs específicos en `dto/gestionArchivos`: `DTOStartUpload`, `DTOUploadChunk`, `DTOEndUpload`, etc.

---

## 2. Formato de Mensajes JSON

### 2.1 Estructura de Peticiones (Cliente → Servidor)

**Objeto**: `DTORequest` serializado

```json
{
  "action": "<nombreAccion>",
  "payload": { 
    // DTO específico serializado
  }
}
```

**Componentes**:
- `action` (String): Identificador de la operación solicitada
- `payload` (Object): Datos específicos según el DTO correspondiente

### 2.2 Estructura de Respuestas (Servidor → Cliente)

**Objeto**: `DTOResponse` serializado

```json
{
  "action": "<nombreAccion>",
  "status": "success" | "error",
  "message": "Descripción legible (opcional)",
  "data": { 
    // Contenido específico o null
  }
}
```

**Componentes**:
- `action` (String): Identificador de la operación (debe coincidir con manejador registrado)
- `status` (String): Estado de la operación (`"success"` o `"error"`)
- `message` (String): Descripción del resultado (opcional)
- `data` (Object|null): Datos de respuesta en caso de éxito

**Validación en cliente**: `DTOResponse.fueExitoso()` compara `status` con `"success"` (case-insensitive)

---

## 3. Catálogo de Acciones

### 3.1 startFileUpload
**Descripción**: Iniciar subida de archivo autenticada

**Petición**:
```json
{
  "action": "startFileUpload",
  "payload": {
    "fileName": "documento.pdf",
    "fileMimeType": "application/pdf",
    "totalChunks": 8
  }
}
```

**DTO**: `DTOStartUpload`

**Respuesta Exitosa**:
```json
{
  "action": "startFileUpload",
  "status": "success",
  "message": "Subida iniciada correctamente",
  "data": {
    "uploadId": "upload-abc123-456"
  }
}
```

**Respuesta Error**:
```json
{
  "action": "startFileUpload",
  "status": "error",
  "message": "Usuario no autenticado",
  "data": null
}
```

---

### 3.2 uploadFileForRegistration
**Descripción**: Iniciar subida de archivo sin autenticación (para registro de usuario)

**Petición**:
```json
{
  "action": "uploadFileForRegistration",
  "payload": {
    "fileName": "foto_perfil.jpg",
    "fileMimeType": "image/jpeg",
    "totalChunks": 3
  }
}
```

**DTO**: `DTOStartUpload`

**Respuesta**: Idéntica a `startFileUpload` (devuelve `uploadId` en `data`)

---

### 3.3 uploadFileChunk
**Descripción**: Enviar un chunk específico de un archivo

**Petición**:
```json
{
  "action": "uploadFileChunk",
  "payload": {
    "uploadId": "upload-abc123-456",
    "chunkNumber": 1,
    "chunkData_base64": "iVBORw0KGgoAAAANSUhEUgAA..."
  }
}
```

**DTO**: `DTOUploadChunk`

**Patrones de Respuesta Observados**:

#### Patrón A: Acción Base
```json
{
  "action": "uploadFileChunk",
  "status": "success",
  "message": "Chunk recibido",
  "data": {
    "uploadId": "upload-abc123-456",
    "chunkNumber": 1
  }
}
```

#### Patrón B: Acción Específica por Chunk
```json
{
  "action": "uploadFileChunk_upload-abc123-456_1",
  "status": "success",
  "message": "Chunk procesado",
  "data": {
    "processed": true
  }
}
```

**⚠️ Problema Común**: Si el cliente registra manejador para `uploadFileChunk_<uploadId>_<chunkNumber>` pero el servidor responde con `uploadFileChunk`, el manejador NO será encontrado.

---

### 3.4 endFileUpload
**Descripción**: Finalizar subida de archivo y validar integridad

**Petición**:
```json
{
  "action": "endFileUpload",
  "payload": {
    "uploadId": "upload-abc123-456",
    "fileHash_sha256": "a3b2c1d4e5f6..."
  }
}
```

**DTO**: `DTOEndUpload`

**Respuesta Exitosa**:
```json
{
  "action": "endFileUpload",
  "status": "success",
  "message": "Archivo almacenado correctamente",
  "data": {
    "fileName": "documento_final.pdf",
    "fileId": "file-789xyz"
  }
}
```

---

### 3.5 startFileDownload
**Descripción**: Iniciar descarga de archivo

**Petición**:
```json
{
  "action": "startFileDownload",
  "payload": {
    "fileId": "file-789xyz"
  }
}
```

**DTO**: `DTOStartDownload`

**Respuesta Exitosa**:
```json
{
  "action": "startFileDownload",
  "status": "success",
  "message": "Descarga iniciada",
  "data": {
    "downloadId": "dl-987zyx",
    "fileName": "documento_final.pdf",
    "fileSize": 2048576,
    "totalChunks": 8,
    "mimeType": "application/pdf"
  }
}
```

**DTO de data**: `DTODownloadInfo`

---

### 3.6 requestFileChunk
**Descripción**: Solicitar un chunk específico durante descarga

**Petición**:
```json
{
  "action": "requestFileChunk",
  "payload": {
    "downloadId": "dl-987zyx",
    "chunkNumber": 3
  }
}
```

**DTO**: `DTORequestChunk`

**Patrones de Respuesta Observados**:

#### Patrón A: Acción Base
```json
{
  "action": "downloadFileChunk",
  "status": "success",
  "message": "",
  "data": {
    "downloadId": "dl-987zyx",
    "chunkNumber": 3,
    "chunkData": "base64EncodedData...",
    "isLast": false
  }
}
```

#### Patrón B: Acción Específica por Chunk
```json
{
  "action": "downloadFileChunk_dl-987zyx_3",
  "status": "success",
  "message": "",
  "data": {
    "chunkData": "base64EncodedData...",
    "isLast": false
  }
}
```

**DTO de data**: `DTODownloadChunk`

---

## 4. Flujo de Comunicación

### 4.1 Flujo de Subida de Archivo

```
Cliente                          Servidor
  |                                 |
  |---(1) startFileUpload---------->|
  |<--(2) {uploadId}----------------|
  |                                 |
  |---(3) uploadFileChunk (1)------>|
  |<--(4) {success}-----------------|
  |                                 |
  |---(5) uploadFileChunk (2)------>|
  |<--(6) {success}-----------------|
  |                                 |
  |     ... (chunks restantes)      |
  |                                 |
  |---(N) endFileUpload------------>|
  |<--(N+1) {fileName, fileId}------|
```

### 4.2 Flujo de Descarga de Archivo

```
Cliente                          Servidor
  |                                 |
  |---(1) startFileDownload-------->|
  |<--(2) {downloadId, info}--------|
  |                                 |
  |---(3) requestFileChunk (1)----->|
  |<--(4) {chunkData}---------------|
  |                                 |
  |---(5) requestFileChunk (2)----->|
  |<--(6) {chunkData}---------------|
  |                                 |
  |     ... (chunks restantes)      |
```

---

## 5. Diagnóstico de Errores

### 5.1 "No se encontró un manejador para la acción: \<action\>"

**Causa**: Desajuste entre el `action` de la respuesta del servidor y las claves registradas en `GestorRespuesta.manejadores`

**Ejemplo**:
- Cliente registra: `"uploadFileChunk_upload123_1"`
- Servidor responde: `"uploadFileChunk"`
- Resultado: ❌ No hay coincidencia

**Diagnóstico**:
1. Verificar el JSON exacto recibido (revisa logs del `GestorRespuesta`)
2. Verificar las claves registradas en el mapa de manejadores
3. Identificar si el servidor usa patrón A (base) o B (específico)

---

### 5.2 "Usuario no autenticado"

**Causa**: Sesión inválida, expirada o falta de credenciales en la petición

**Ejemplo de respuesta**:
```json
{
  "action": "uploadFileChunk",
  "status": "error",
  "message": "Usuario no autenticado",
  "data": null
}
```

**Diagnóstico**:
1. **Verificar sesión activa**:
   ```java
   DTOSesion sesion = GestorConexion.getInstancia().getSesion();
   boolean activa = sesion != null && sesion.estaActiva();
   System.out.println("Sesión activa: " + activa);
   ```

2. **Inspeccionar petición enviada**:
   - Revisar stdout: `">> Petición enviada: ..."`
   - Verificar si incluye token/credenciales requeridas

3. **Revisar logs del servidor**:
   - ¿Espera un campo `token` en el payload?
   - ¿Usa headers de autenticación?
   - ¿La sesión expiró en el servidor?

---

### 5.3 Chunk no procesado (sin error explícito)

**Síntomas**: La promesa/futuro del chunk nunca se completa

**Causas posibles**:
1. El servidor respondió con una acción diferente a la esperada
2. Error de red (respuesta no llegó)
3. Manejador registrado pero con lógica incorrecta

**Diagnóstico**:
1. Añadir logs en el manejador registrado
2. Verificar que `GestorRespuesta.iniciarEscucha()` está activo
3. Revisar si hubo excepción en la deserialización JSON

---

## 6. Guía de Soluciones

### 6.1 Solución A: Registrar Manejador Base (Cliente)
**Recomendado para**: Cambio mínimo, no requiere coordinación con servidor

**Estrategia**: Registrar un único manejador para la acción base y filtrar por `uploadId`/`chunkNumber` en el `data`

**Implementación conceptual**:
```java
// En lugar de registrar: "uploadFileChunk_<uploadId>_<chunkNumber>"
// Registrar una sola vez:
gestorRespuesta.registrarManejador("uploadFileChunk", response -> {
    if (response.fueExitoso()) {
        Map<String, Object> data = response.getDataAsMap();
        String uploadId = (String) data.get("uploadId");
        Integer chunkNumber = (Integer) data.get("chunkNumber");
        
        // Buscar la transferencia activa que coincida
        TransferenciaActiva transferencia = buscarTransferencia(uploadId);
        if (transferencia != null && transferencia.esperaChunk(chunkNumber)) {
            transferencia.completarChunk(chunkNumber);
        }
    } else {
        // Manejar error
        logger.error("Error en chunk: " + response.getMessage());
    }
});
```

**Ventajas**:
- No requiere registrar/desregistrar manejadores dinámicamente
- Funciona con ambos patrones de servidor (A y B)
- Más fácil de mantener

---

### 6.2 Solución B: Incluir Token en Payload (Cliente)
**Recomendado para**: Resolver error "Usuario no autenticado"

**Estrategia**: Añadir campo `token` a los DTOs de petición

**Opción 1 - Extender DTO existente**:
```java
// Modificar DTOUploadChunk
public class DTOUploadChunk {
    private String uploadId;
    private int chunkNumber;
    private String chunkData_base64;
    private String token; // NUEVO
    
    // Constructor y getters
}
```

**Opción 2 - Wrapper de autenticación**:
```json
{
  "action": "uploadFileChunk",
  "payload": {
    "auth": {
      "token": "eyJhbGciOiJIUzI1NiIs..."
    },
    "data": {
      "uploadId": "upload-abc123-456",
      "chunkNumber": 1,
      "chunkData_base64": "..."
    }
  }
}
```

**Implementación**:
```java
// Antes de enviar petición
DTOSesion sesion = GestorConexion.getInstancia().getSesion();
if (sesion != null && sesion.estaActiva()) {
    String token = sesion.getToken();
    DTOUploadChunk payload = new DTOUploadChunk(
        uploadId, 
        chunkNumber, 
        chunkData, 
        token
    );
    enviadorPeticiones.enviar("uploadFileChunk", payload);
} else {
    throw new IllegalStateException("Sesión no activa");
}
```

---

### 6.3 Solución C: Estandarizar Protocolo (Servidor + Cliente)
**Recomendado para**: Solución a largo plazo

**Cambios en DTORequest**:
```java
public class DTORequest {
    private String action;
    private Object payload;
    private Map<String, String> meta; // NUEVO: para token, traceId, etc.
    
    // Getters y setters
}
```

**Ejemplo de uso**:
```json
{
  "action": "uploadFileChunk",
  "meta": {
    "token": "eyJhbGci...",
    "traceId": "trace-xyz123"
  },
  "payload": {
    "uploadId": "upload-abc123-456",
    "chunkNumber": 1,
    "chunkData_base64": "..."
  }
}
```

**Ventajas**:
- Metadatos separados de lógica de negocio
- Extensible para futuros requerimientos
- No contamina los DTOs específicos

---

## 7. Ejemplos Completos

### 7.1 Ejemplo: Subida Completa de Archivo

**Paso 1: Iniciar subida**
```json
>> Petición enviada:
{
  "action": "startFileUpload",
  "payload": {
    "fileName": "reporte_mensual.pdf",
    "fileMimeType": "application/pdf",
    "totalChunks": 4
  }
}

<< Respuesta recibida:
{
  "action": "startFileUpload",
  "status": "success",
  "message": "",
  "data": {
    "uploadId": "upload-20231015-abc456"
  }
}
```

**Paso 2: Enviar chunk 1**
```json
>> Petición enviada:
{
  "action": "uploadFileChunk",
  "payload": {
    "uploadId": "upload-20231015-abc456",
    "chunkNumber": 1,
    "chunkData_base64": "JVBERi0xLjQKJeLj..."
  }
}

<< Respuesta recibida:
{
  "action": "uploadFileChunk",
  "status": "success",
  "message": "",
  "data": {
    "uploadId": "upload-20231015-abc456",
    "chunkNumber": 1
  }
}
```

**Paso 3: Enviar chunks 2-4** (similar al paso 2)

**Paso 4: Finalizar subida**
```json
>> Petición enviada:
{
  "action": "endFileUpload",
  "payload": {
    "uploadId": "upload-20231015-abc456",
    "fileHash_sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
  }
}

<< Respuesta recibida:
{
  "action": "endFileUpload",
  "status": "success",
  "message": "Archivo verificado y almacenado",
  "data": {
    "fileName": "reporte_mensual_20231015.pdf",
    "fileId": "file-server-789xyz"
  }
}
```

---

### 7.2 Ejemplo: Manejo de Error de Autenticación

**Escenario**: Token expirado durante subida de chunk

```json
>> Petición enviada:
{
  "action": "uploadFileChunk",
  "payload": {
    "uploadId": "upload-20231015-abc456",
    "chunkNumber": 2,
    "chunkData_base64": "..."
  }
}

<< Respuesta recibida:
{
  "action": "uploadFileChunk",
  "status": "error",
  "message": "Usuario no autenticado",
  "data": null
}
```

**Acción del cliente**:
1. Detectar `fueExitoso() == false`
2. Verificar mensaje de error
3. Renovar autenticación
4. Reintentar envío del chunk

---

## 8. Mejores Prácticas

### 8.1 Gestión de Manejadores

✅ **Hacer**:
- Registrar manejadores para acciones base cuando sea posible
- Desregistrar manejadores cuando ya no sean necesarios
- Implementar manejo de errores en cada manejador
- Añadir logs para depuración

❌ **Evitar**:
- Registrar dinámicamente manejadores por cada chunk
- Dejar manejadores registrados indefinidamente
- Asumir que todas las respuestas son exitosas

### 8.2 Autenticación

✅ **Hacer**:
- Verificar sesión activa antes de cada operación crítica
- Implementar renovación automática de tokens
- Manejar errores de autenticación de forma específica
- Incluir token en peticiones que lo requieran

❌ **Evitar**:
- Asumir que la sesión siempre está activa
- Ignorar respuestas de "Usuario no autenticado"
- Reintentar indefinidamente sin re-autenticar

### 8.3 Manejo de Chunks

✅ **Hacer**:
- Implementar reintentos con backoff exponencial
- Validar hash SHA-256 al finalizar
- Mostrar progreso al usuario
- Cancelar transferencias cuando sea necesario

❌ **Evitar**:
- Enviar todos los chunks en paralelo sin control
- Ignorar errores en chunks individuales
- Continuar subida si un chunk falla repetidamente

### 8.4 Logging y Depuración

✅ **Hacer**:
```java
// Log de peticiones
logger.debug(">> Enviando: action={}, payload={}", action, payload);

// Log de respuestas
logger.debug("<< Recibido: action={}, status={}, message={}", 
    response.getAction(), response.getStatus(), response.getMessage());

// Log de errores
if (!response.fueExitoso()) {
    logger.error("Error en {}: {}", response.getAction(), response.getMessage());
}
```

### 8.5 Checklist de Depuración

Cuando algo falle, verificar en orden:

1. ✓ ¿La sesión está activa? (`sesion.estaActiva()`)
2. ✓ ¿Se envió la petición? (revisar stdout)
3. ✓ ¿Se recibió respuesta? (revisar logs de `GestorRespuesta`)
4. ✓ ¿El `action` de la respuesta coincide con el manejador registrado?
5. ✓ ¿El `status` es `"success"`?
6. ✓ ¿El `data` contiene la información esperada?
7. ✓ ¿Hay excepciones en los logs?

---

## Historial de Cambios

| Fecha | Versión | Cambios |
|-------|---------|---------|
| 2024-10-17 | 2.0 | Documentación completa reestructurada con mejores prácticas, ejemplos y guías de solución |
| 2024-XX-XX | 1.0 | Documentación inicial |

---

## Resumen Ejecutivo

**Para resolver rápidamente los problemas más comunes**:

1. **"No se encontró un manejador"** → Usa Solución A (registrar manejador base)
2. **"Usuario no autenticado"** → Usa Solución B (incluir token en payload)
3. **Chunks no se procesan** → Verifica logs y coincidencia de actions

**Cambio mínimo recomendado**: Implementar Solución A en `GestionArchivosImpl` para usar manejadores base con filtrado por `uploadId`/`chunkNumber` en lugar de manejadores dinámicos por chunk.
