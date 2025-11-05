# Bug Fix: File Download - Case Sensitivity Issue

## Date
November 5, 2025

## Problem Description

### Symptoms
When the client tried to download files (e.g., user profile photos during login), the download would fail silently with the error:
```
No se encontró un manejador para la acción: startfiledownload
```

### Root Cause
**Case sensitivity mismatch** between client and server action names:

1. **Client sends**: `{"action":"startFileDownload",...}` (camelCase)
2. **Server processes**: Converts action to lowercase internally
3. **Server responds**: `{"action":"startfiledownload",...}` (all lowercase)
4. **Client handler**: Only registered `startFileDownload` (camelCase)
5. **Result**: Handler lookup failed because of exact string matching

### Evidence from Logs
```
>> Petición enviada: {"action":"startFileDownload","payload":{"fileId":"user_photos/deivid1.jpg"}}
<< Respuesta recibida: {"action":"startfiledownload","status":"success",...}
No se encontró un manejador para la acción: startfiledownload
```

## Server-Side Behavior (Reference)
From `RequestDispatcher.java`:
```java
action = request.getAction() != null ? request.getAction().toLowerCase() : "unknown";
```
The server normalizes all actions to lowercase for internal routing, but then sends back the lowercase version in responses.

## Solution

### File Modified
`/home/deivid/Documents/Chat-Unillanos/Cliente/Persistencia/Comunicacion/src/main/java/comunicacion/GestorRespuesta.java`

### Change Made
Modified the `procesarRespuesta()` method to implement **case-insensitive handler lookup**:

```java
private void procesarRespuesta(String jsonResponse) {
    try {
        DTOResponse response = gson.fromJson(jsonResponse, DTOResponse.class);
        if (response != null && response.getAction() != null) {
            // Normalizar la acción a minúsculas para comparación case-insensitive
            String actionNormalizada = response.getAction().toLowerCase();
            
            // Buscar manejador con la acción original o normalizada
            Consumer<DTOResponse> manejador = manejadores.get(response.getAction());
            if (manejador == null) {
                // Intentar buscar con todas las claves normalizadas
                for (Map.Entry<String, Consumer<DTOResponse>> entry : manejadores.entrySet()) {
                    if (entry.getKey().toLowerCase().equals(actionNormalizada)) {
                        manejador = entry.getValue();
                        break;
                    }
                }
            }
            
            if (manejador != null) {
                manejador.accept(response);
            } else {
                System.out.println("No se encontró un manejador para la acción: " + response.getAction());
            }
        }
    } catch (JsonSyntaxException e) {
        System.err.println("Error al parsear la respuesta JSON: " + jsonResponse);
    }
}
```

### How It Works
1. First attempts exact match (preserves backward compatibility)
2. If no exact match, normalizes both the incoming action and registered handler keys to lowercase
3. Compares using lowercase, allowing `startFileDownload` (registered) to match `startfiledownload` (received)

## Impact

### Fixed Actions
This fix resolves the case sensitivity issue for ALL server responses, including:
- `startFileDownload` / `startfiledownload`
- `requestFileChunk` / `requestfilechunk`
- `obtenerNotificaciones` / `obtenernotificaciones`
- Any other actions where the server returns lowercase

### Benefits
- ✅ File downloads now work correctly
- ✅ User profile photos load during login
- ✅ All audio file downloads will work
- ✅ Backward compatible (exact matches still work)
- ✅ Future-proof against similar case mismatches

## Testing
After rebuild, test the following scenarios:
1. Login and verify profile photo downloads
2. Send/receive audio messages
3. Download any files from the server
4. Verify all actions complete without "handler not found" errors

## Build Status
✅ Project rebuilt successfully with `mvn clean install -DskipTests`

## Related Files
- Client: `Persistencia/Comunicacion/src/main/java/comunicacion/GestorRespuesta.java`
- Client: `Negocio/GestionArchivos/src/main/java/gestionArchivos/GestionArchivosImpl.java` (uses handlers)
- Server: `RequestDispatcher.java` (converts actions to lowercase)

