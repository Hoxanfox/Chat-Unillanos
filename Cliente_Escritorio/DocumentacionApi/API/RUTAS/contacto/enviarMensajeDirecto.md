Request
```json
{
  "action": "enviarMensajeDirecto",
  "data": {
    "peerDestinoId": "uuiid-peer",
    "peerRemitenteId": "uuid-peer",
    "remitenteId": "id-del-usuario-que-envia",
    "destinatarioId": "id-del-contacto-destino",
    "tipo": "texto",
    "contenido": "Hola, ¿cómo estás?"
  }
}
```
Response
```json
{
  "action": "enviarMensajeDirecto",
  "status": "success", // o "success": true [cite: 13]
  "message": "Mensaje enviado",
  "data": {
    "mensajeId": "msg-uuid-servidor-xyz",
    "fechaEnvio": "2025-10-28T14:40:00Z"
  }
}
```
Error
1. Error general
```json
{
  "action": "enviarMensajeDirecto",
  "status": "error",
  "message": "Error al enviar mensaje: [descripción del error]",
  "data": null
}
```
2. 
```json
{
  "action": "enviarMensajeDirecto",
  "status": "error",
  "message": "Destinatario no encontrado o desconectado",
  "data": null
}
```
3. Error de Validación
```json
{
  "action": "enviarMensajeDirecto",
  "status": "error",
  "message": "Datos de mensaje inválidos",
  "data": {
    "campo": "contenido",
    "motivo": "El contenido no puede estar vacío"
  }
}
```