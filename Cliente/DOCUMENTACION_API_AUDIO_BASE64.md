# 📡 API de Mensajes de Audio con Base64

## 📋 Resumen

La nueva API de mensajes de audio utiliza Base64 para transmitir el contenido de audio directamente en el JSON, eliminando la necesidad de subir archivos previamente. Incluye tracking temporal para confirmar mensajes.

---

## 📤 REQUEST: Enviar Mensaje de Audio

### Estructura JSON
```json
{
  "action": "enviarMensajeDirectoAudio",
  "data": {
    "peerDestinoId": "uuid-peer-destino",
    "peerRemitenteId": "uuid-peer-remitente",
    "remitenteId": "id-del-usuario-que-envia",
    "destinatarioId": "id-del-contacto-destino",
    "tipo": "audio",
    "contenido": "aW9kYXNkaGFza2RoYXNrZGpoYXNrZGpoYXNrZGg=",
    "mensajeTempId": "temp-uuid-cliente-456"
  }
}
```

### Campos

| Campo | Tipo | Descripción | Obligatorio |
|-------|------|-------------|-------------|
| `peerDestinoId` | String | UUID del peer WebRTC destino | ✅ Sí |
| `peerRemitenteId` | String | UUID del peer WebRTC remitente | ✅ Sí |
| `remitenteId` | String | ID del usuario que envía | ✅ Sí |
| `destinatarioId` | String | ID del usuario destino | ✅ Sí |
| `tipo` | String | Siempre `"audio"` | ✅ Sí |
| `contenido` | String | Audio codificado en Base64 | ✅ Sí |
| `mensajeTempId` | String | ID temporal para tracking | ⚠️ Opcional* |

*Si no se proporciona, se genera automáticamente.

### Ejemplo de Código Java
```java
// Enviar mensaje de audio
String audioBase64 = convertirAudioABase64(archivoAudio);
String tempId = "temp-" + UUID.randomUUID();

gestionMensajes.enviarMensajeAudio(
    "contacto-456",      // destinatarioId
    audioBase64,         // contenido en Base64
    tempId               // mensajeTempId
);
```

---

## 📥 RESPONSE: Confirmación de Envío

### ✅ Respuesta Exitosa
```json
{
  "action": "enviarMensajeDirectoAudio",
  "status": "success",
  "message": "Mensaje de audio enviado",
  "data": {
    "mensajeId": "msg-uuid-servidor-abc",
    "fechaEnvio": "2025-10-28T14:42:00Z",
    "mensajeTempId": "temp-uuid-cliente-456"
  }
}
```

### ❌ Errores Posibles

#### 1. Error General
```json
{
  "action": "enviarMensajeDirectoAudio",
  "status": "error",
  "message": "Error al enviar mensaje de audio: [descripción del error]",
  "data": null
}
```

#### 2. Destinatario No Encontrado
```json
{
  "action": "enviarMensajeDirectoAudio",
  "status": "error",
  "message": "Destinatario no encontrado o desconectado",
  "data": null
}
```

#### 3. Error de Validación
```json
{
  "action": "enviarMensajeDirectoAudio",
  "status": "error",
  "message": "Datos de mensaje inválidos",
  "data": {
    "campo": "contenido",
    "motivo": "Formato de audio Base64 inválido o corrupto"
  }
}
```

---

## 🔔 PUSH NOTIFICATION: Nuevo Mensaje de Audio

### ✅ Notificación Exitosa
```json
{
  "action": "nuevoMensajeDirectoAudio",
  "status": "success",
  "message": "Nuevo mensaje de audio recibido",
  "data": {
    "mensajeId": "msg-audio-456",
    "peerRemitenteId": "peer-xyz",
    "peerDestinoId": "peer-abc",
    "remitenteId": "contacto-456",
    "remitenteNombre": "María González",
    "destinatarioId": "user-123",
    "tipo": "AUDIO",
    "audioBase64": "aW9kYXNkaGFza2RoYXNrZGpoYXNrZGpoYXNrZGg=",
    "fechaEnvio": "2025-11-01T11:15:00Z"
  }
}
```

### ❌ Error en Push
```json
{
  "action": "nuevoMensajeDirectoAudio",
  "status": "error",
  "message": "Error al obtener el mensaje",
  "data": null
}
```

---

## 🔄 Flujo Completo de Envío y Recepción

```
┌─────────────┐                                              ┌─────────────┐
│  Cliente A  │                                              │  Cliente B  │
└──────┬──────┘                                              └──────┬──────┘
       │                                                             │
       │ 1. Graba audio                                             │
       │ 2. Convierte a Base64                                      │
       │ 3. Genera mensajeTempId                                    │
       │                                                             │
       │ 4. REQUEST: enviarMensajeDirectoAudio                      │
       ├─────────────────────────────►┌──────────┐                 │
       │                               │ Servidor │                 │
       │ 5. RESPONSE: confirmación     └────┬─────┘                 │
       │◄───────────────────────────────────┤                       │
       │    (mensajeId + tempId)            │                       │
       │                                    │                       │
       │ 6. Notifica UI:                    │ 7. PUSH: nuevoMensaje │
       │    MENSAJE_AUDIO_ENVIADO_EXITOSO   ├──────────────────────►│
       │                                    │                       │
       │                                    │ 8. Filtra duplicados  │
       │                                    │    ✓ NO es mío        │
       │                                    │    ✓ Es para mi peer  │
       │                                    │                       │
       │                                    │ 9. Notifica UI:       │
       │                                    │    NUEVO_MENSAJE_     │
       │                                    │    AUDIO_PRIVADO      │
       │                                    │                       │
       │                                    │ 10. Decodifica Base64 │
       │                                    │ 11. Reproduce audio   │
```

---

## 🎯 Eventos del Observador

### Cliente que envía:
- `MENSAJE_AUDIO_ENVIADO_EXITOSO` → Mensaje enviado correctamente
- `ERROR_DESTINATARIO_NO_DISPONIBLE` → Destinatario no encontrado
- `ERROR_VALIDACION` → Datos inválidos
- `ERROR_ENVIO_MENSAJE_AUDIO` → Error general

### Cliente que recibe:
- `NUEVO_MENSAJE_AUDIO_PRIVADO` → Nuevo mensaje de audio recibido
- `ERROR_NOTIFICACION_MENSAJE_AUDIO` → Error en notificación push

---

## 🛡️ Validaciones y Filtros

### Al Enviar
1. ✅ Peer destino debe existir
2. ✅ Audio Base64 no debe estar vacío
3. ✅ `mensajeTempId` se genera automáticamente si no se proporciona

### Al Recibir Push
1. ✅ **Filtro Anti-Duplicados**: Ignora si `remitenteId == myUserId`
2. ✅ **Filtro Multi-Dispositivo**: Ignora si `peerDestinoId != myPeerId`
3. ✅ Marca como `esMio = false`

---

## 📦 DTOs Involucrados

### DTOEnviarMensajeAudio.java
```java
public class DTOEnviarMensajeAudio {
    private final String peerDestinoId;
    private final String peerRemitenteId;
    private final String remitenteId;
    private final String destinatarioId;
    private final String tipo;              // "audio"
    private final String contenido;         // Base64
    private final String mensajeTempId;
}
```

### DTOMensaje.java (actualizado)
```java
public class DTOMensaje {
    // ...campos existentes...
    private String audioBase64;         // ← NUEVO
    private String mensajeTempId;       // ← NUEVO
}
```

---

## 🔍 Diferencias con la API Anterior

| Aspecto | API Antigua (FileId) | API Nueva (Base64) |
|---------|---------------------|-------------------|
| **Action** | `enviarMensajeDirecto` | `enviarMensajeDirectoAudio` |
| **Contenido** | `audioFileId` (referencia) | `contenido` (Base64 completo) |
| **Tracking** | No tenía | `mensajeTempId` |
| **Push Action** | `nuevoMensajeDirecto` | `nuevoMensajeDirectoAudio` |
| **Campo Audio** | `audioFileId` + `fileName` | `audioBase64` |
| **Subida previa** | ✅ Requerida | ❌ No necesaria |

---

## ⚙️ Ejemplo de Uso Completo

```java
// 1. Capturar audio
byte[] audioBytes = grabarAudio();

// 2. Convertir a Base64
String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);

// 3. Generar ID temporal
String tempId = "temp-" + UUID.randomUUID();

// 4. Enviar mensaje
gestionMensajes.enviarMensajeAudio(
    "contacto-456",
    audioBase64,
    tempId
);

// 5. Escuchar respuesta
gestionMensajes.registrarObservador(new IObservador() {
    @Override
    public void actualizar(String tipo, Object datos) {
        switch (tipo) {
            case "MENSAJE_AUDIO_ENVIADO_EXITOSO":
                DTOMensaje msg = (DTOMensaje) datos;
                System.out.println("✅ Audio enviado: " + msg.getMensajeId());
                System.out.println("   TempId: " + msg.getMensajeTempId());
                break;
                
            case "NUEVO_MENSAJE_AUDIO_PRIVADO":
                DTOMensaje audioRecibido = (DTOMensaje) datos;
                String base64 = audioRecibido.getAudioBase64();
                byte[] audioData = Base64.getDecoder().decode(base64);
                reproducirAudio(audioData);
                break;
                
            case "ERROR_ENVIO_MENSAJE_AUDIO":
                System.err.println("❌ Error: " + datos);
                break;
        }
    }
});
```

---

## 🧪 Tests Disponibles

```java
@Test
void testEnviarMensajeAudio_Exitoso() {
    gestionMensajes.enviarMensajeAudio(
        "contacto-456",
        "aW9kYXNkaGFza2RoYXNrZGpoYXNrZGpoYXNrZGg=",
        "temp-uuid-cliente-456"
    );
    
    verify(mockEnviadorPeticiones).enviar(
        argThat(req -> 
            req.getAction().equals("enviarMensajeDirectoAudio")
        )
    );
}
```

---

## 📚 Referencias

- `GestionMensajesImpl.java` - Implementación completa
- `DTOEnviarMensajeAudio.java` - DTO de request
- `DTOMensaje.java` - DTO de mensaje con audioBase64
- `GestionMensajesImplTest.java` - Tests unitarios

