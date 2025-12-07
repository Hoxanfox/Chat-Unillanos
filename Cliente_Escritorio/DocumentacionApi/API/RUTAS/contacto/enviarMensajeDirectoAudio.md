Request
```json
{
  "action": "enviarMensajeDirectoAudio",
  "data": {
    "peerDestinoId": "uuiid-peer",
    "peerRemitenteId": "uuid-peer",
    "remitenteId": "id-del-usuario-que-envia",
    "destinatarioId": "id-del-contacto-destino",
    "tipo": "audio",
    "audioId": "sg-audio-456="
    
  }
}
```
Response
```json
{
  "action": "enviarMensajeDirectoAudio",
  "status": "success", // o "success": true
  "message": "Mensaje de audio enviado",
  "data": {
    "mensajeId": "msg-uuid-servidor-abc",
    "fechaEnvio": "2025-10-28T14:42:00Z"
  }
}
```
Error
1. Error general
```json
{
  "action": "enviarMensajeDirectoAudio",
  "status": "error",
  "message": "Error al enviar mensaje de audio: [descripción del error]",
  "data": null
}
```
2. Destinatario no encontrado
```json
{
  "action": "enviarMensajeDirectoAudio",
  "status": "error",
  "message": "Destinatario no encontrado o desconectado",
  "data": null
}
```
3. Error de Validación
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