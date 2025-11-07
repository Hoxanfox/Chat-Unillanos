---

## ⚠️ Problema Identificado: Mayúsculas vs Minúsculas

### **Servidor envía en la respuesta:**
```json
"messageType": "TEXT"
```

### **Cliente envía en el request:**
```java
"tipo": "texto"
```

### **¿Es esto un problema?**

**NO**, porque:
1. El servidor **NO LEE** el campo `tipo` del cliente
2. El servidor genera el `messageType` basándose en el endpoint llamado
3. En `handleSendTextMessage()` siempre usa `"TEXT"`
4. En `handleSendAudioMessage()` siempre usa `"AUDIO"`

Sin embargo, **cuando el cliente recibe la respuesta**, debe mapear correctamente:

```java
// En GestorMensajesCanalImpl.construirDTOMensajeDesdeMap()
mensaje.setTipo(getString(data, "messageType")); // Lee "TEXT" del servidor
```

Luego el cliente debe convertir:
- Servidor: `"TEXT"` → Cliente interno: `"texto"` o `"TEXT"` (ambos funcionan)
- Servidor: `"AUDIO"` → Cliente interno: `"audio"` o `"AUDIO"` (ambos funcionan)

---

## 🔧 Verificación del Cliente

Voy a revisar cómo el cliente maneja el campo `messageType`:

### **En la respuesta del historial:**
```java
// MessageController.java - handleGetHistory()
for (MessageResponseDto mensaje : mensajes) {
    Map<String, Object> mensajeMap = new HashMap<>();
    // ...
    mensajeMap.put("messageType", mensaje.getMessageType()); // "TEXT" o "AUDIO"
    
    if ("AUDIO".equals(mensaje.getMessageType())) {
        // Lógica para audio
    } else {
        // Lógica para texto
    }
}
```

El servidor siempre envía `messageType` en **MAYÚSCULAS**.

### **En el cliente:**
```java
// GestorMensajesCanalImpl.java - construirDTOMensajeDesdeMap()
mensaje.setTipo(getString(data, "messageType")); // Almacena "TEXT" o "AUDIO"
```

```java
// RepositorioMensajeCanalImpl.java - convertirDTOAMensajeRecibido()
String contenidoStr = "texto".equals(dto.getTipo()) ? dto.getContenido() : dto.getFileId();
```

**⚠️ PROBLEMA ENCONTRADO:** El cliente compara con `"texto"` en minúsculas, pero el servidor envía `"TEXT"` en mayúsculas.

---

## 🐛 Bug Identificado

**En `RepositorioMensajeCanalImpl.java`:**
```java
String contenidoStr = "texto".equals(dto.getTipo()) ? dto.getContenido() : dto.getFileId();
```

Esto **NUNCA** será true si `dto.getTipo()` es `"TEXT"` (del servidor).

**Solución:** Usar comparación case-insensitive o normalizar a mayúsculas.

---

## ✅ Corrección Necesaria

El cliente debe normalizar los tipos de mensaje para ser consistente con el servidor.

### **Opciones:**

#### **Opción 1: Normalizar al recibir del servidor (RECOMENDADO)**
```java
// En construirDTOMensajeDesdeMap()
String messageType = getString(data, "messageType");
if (messageType != null) {
    messageType = messageType.toUpperCase(); // "TEXT" o "AUDIO"
}
mensaje.setTipo(messageType);
```

#### **Opción 2: Usar comparación case-insensitive**
```java
String contenidoStr = "texto".equalsIgnoreCase(dto.getTipo()) ? dto.getContenido() : dto.getFileId();
```

#### **Opción 3: Normalizar al enviar (NO recomendado porque el servidor lo ignora)**

---

## 📋 Resumen de Compatibilidad

| Aspecto | Estado | Notas |
|---------|--------|-------|
| Campos requeridos | ✅ **OK** | Cliente envía `canalId` y `contenido` |
| Validaciones | ✅ **OK** | Cliente valida antes de enviar |
| Autenticación | ✅ **OK** | Servidor usa sesión autenticada |
| Tipo de mensaje (envío) | ✅ **OK** | Servidor ignora campo `tipo` del cliente |
| Tipo de mensaje (recepción) | ❌ **BUG** | Cliente usa minúsculas, servidor mayúsculas |
| Respuesta del servidor | ✅ **OK** | Cliente mapea correctamente |

---

## 🚀 Correcciones Requeridas

### **1. Normalizar tipo de mensaje al recibir**

**Archivo:** `GestorMensajesCanalImpl.java`

```java
private DTOMensajeCanal construirDTOMensajeDesdeMap(Map<String, Object> data) {
    DTOMensajeCanal mensaje = new DTOMensajeCanal();
    
    // ... código existente ...
    
    // ✅ FIX: Normalizar tipo de mensaje
    String messageType = getString(data, "messageType");
    if (messageType != null) {
        messageType = messageType.toUpperCase();
    }
    mensaje.setTipo(messageType);
    
    // ... resto del código ...
}
```

### **2. Usar comparación case-insensitive en repositorio**

**Archivo:** `RepositorioMensajeCanalImpl.java`

```java
private MensajeRecibidoCanal convertirDTOAMensajeRecibido(DTOMensajeCanal dto, String usuarioId) {
    // ... código existente ...
    
    // ✅ FIX: Comparación case-insensitive
    String contenidoStr = "TEXT".equalsIgnoreCase(dto.getTipo()) 
        ? dto.getContenido() 
        : dto.getFileId();
    
    // ... resto del código ...
}
```

### **3. Actualizar obtenerHistorialCanal**

**Archivo:** `RepositorioMensajeCanalImpl.java`

```java
if (contenidoBytes != null) {
    String contenidoStr = new String(contenidoBytes);
    // ✅ FIX: Comparación case-insensitive
    if ("TEXT".equalsIgnoreCase(dto.getTipo())) {
        dto.setContenido(contenidoStr);
    } else {
        dto.setFileId(contenidoStr);
    }
}
```

---

## 📊 Conclusión

### **Estado Actual:**
- ✅ El cliente envía correctamente los campos requeridos
- ✅ El servidor procesa correctamente las peticiones
- ❌ Hay inconsistencia en mayúsculas/minúsculas del tipo de mensaje

### **Acción Requerida:**
1. ✅ Normalizar tipos de mensaje a MAYÚSCULAS (compatibilidad con servidor)
2. ✅ Usar comparaciones case-insensitive en todo el código
3. ✅ Documentar que el servidor usa MAYÚSCULAS para tipos

### **Impacto:**
- **Crítico:** Los mensajes de audio pueden no guardarse correctamente en BD
- **Solución:** Aplicar las 3 correcciones mencionadas

**El cliente está funcionando correctamente para enviar mensajes, pero necesita las correcciones para procesar correctamente las respuestas del servidor.**
# Análisis: Servidor vs Cliente - Envío de Mensajes a Canal

## 📅 Fecha: 7 de Noviembre, 2025

---

## 🔍 Análisis del Código del Servidor

### **Endpoint: `handleSendTextMessage()` (MessageController.java)**

#### **Campos que el servidor LEE del payload:**
```java
JsonObject mensajeJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
String canalIdStr = mensajeJson.has("canalId") ? mensajeJson.get("canalId").getAsString() : null;
String contenido = mensajeJson.has("contenido") ? mensajeJson.get("contenido").getAsString() : null;
```

#### **Validaciones del servidor:**
1. ✅ `canalId` - **Requerido**, no puede estar vacío
2. ✅ `contenido` - **Requerido**, no puede estar vacío
3. ✅ `contenido` - Máximo 5000 caracteres

#### **Campo que el servidor GENERA automáticamente:**
```java
UUID autorId = handler.getAuthenticatedUser().getUserId();
```
- El servidor **IGNORA** cualquier `remitenteId` del payload
- El servidor usa el **usuario autenticado** de la sesión

#### **DTO interno del servidor:**
```java
SendMessageRequestDto sendMessageDto = new SendMessageRequestDto(
    canalId,      // UUID extraído del payload
    "TEXT",       // Constante en MAYÚSCULAS
    contenido     // Texto del mensaje
);
```

---

## 📤 Análisis del Código del Cliente

### **DTO que el cliente envía: `DTOEnviarMensajeCanal`**

```java
{
    "remitenteId": "9b404e26-55b4-4aef-9cc0-efa4a686cafa",
    "canalId": "13f2cc70-d18d-4da7-8506-92c3fa4ea1b7",
    "tipo": "texto",
    "contenido": "asdasd",
    "fileId": null
}
```

#### **Campos enviados por el cliente:**
1. ✅ `remitenteId` - ID del usuario que envía (el servidor lo IGNORA)
2. ✅ `canalId` - ID del canal destino
3. ✅ `tipo` - Tipo de mensaje ("texto", "audio", "archivo")
4. ✅ `contenido` - Contenido del mensaje (texto o null para archivos)
5. ✅ `fileId` - ID del archivo (null para texto)

---

## ✅ Compatibilidad: Cliente vs Servidor

| Campo Servidor | Campo Cliente | Estado | Notas |
|---------------|---------------|--------|-------|
| `canalId` | `canalId` | ✅ **COMPATIBLE** | Mismo nombre y formato |
| `contenido` | `contenido` | ✅ **COMPATIBLE** | Mismo nombre y formato |
| `autorId` (de sesión) | `remitenteId` | ⚠️ **IGNORADO** | Servidor usa sesión, ignora payload |
| - | `tipo` | ⚠️ **NO USADO** | Servidor no lee este campo |
| - | `fileId` | ⚠️ **NO USADO** | Servidor no lee este campo |

### **Conclusión:**
✅ **El cliente está enviando CORRECTAMENTE todos los campos requeridos**

El servidor solo necesita:
- ✅ `canalId` → Cliente lo envía ✅
- ✅ `contenido` → Cliente lo envía ✅

---

## 🎯 Respuesta del Servidor

### **Respuesta exitosa:**
```json
{
    "action": "enviarMensajeCanal",
    "status": "success",
    "message": "Mensaje enviado",
    "data": {
        "messageId": "ceccf28c-0e01-4956-a0ae-6d13c7455049",
        "channelId": "13f2cc70-d18d-4da7-8506-92c3fa4ea1b7",
        "author": {
            "userId": "9b404e26-55b4-4aef-9cc0-efa4a686cafa",
            "username": "1"
        },
        "timestamp": "2025-11-07T08:36:57.752197109",
        "messageType": "TEXT",
        "content": "asdasd"
    }
}
```

### **Campos en la respuesta:**
- ✅ `messageId` - ID único del mensaje generado por el servidor
- ✅ `channelId` - ID del canal
- ✅ `author` - Objeto con `userId` y `username`
- ✅ `timestamp` - Fecha y hora del servidor
- ✅ `messageType` - **"TEXT"** en MAYÚSCULAS
- ✅ `content` - Contenido del mensaje


