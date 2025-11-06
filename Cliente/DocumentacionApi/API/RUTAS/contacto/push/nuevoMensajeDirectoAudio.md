Response
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
    "audioId": "sg-audio-456", 
    "fechaEnvio": "2025-11-01T11:15:00Z"  
  }  
}
```
Error
1. Error General
```json
{  
  "action": "nuevoMensajeDirectoAudio",  
  "status": "error",  
  "message": "Error al obtener el mensaje",  
  "data": null  
}
```

