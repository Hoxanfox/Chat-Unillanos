Response
```json
{  
  "action": "nuevoMensajeDirecto",  
  "status": "success",  
  "message": "Nuevo mensaje recibido",  
  "data": {  
    "mensajeId": "msg-uuid-123",  
    "peerRemitenteId": "peer-uuid-remitente",  
    "peerDestinoId": "peer-uuid-destino",  
    "remitenteId": "user-abc",  
    "remitenteNombre": "Juan Pérez",  
    "destinatarioId": "tu-user-id",  
    "tipo": "texto",  
    "contenido": "Hola, ¿cómo estás?",  
    "fechaEnvio": "2025-01-28T14:40:00Z"  
  }  
}
```
Error
1. Error General
```json
{  
  "action": "nuevoMensajeDirecto",  
  "status": "error",  
  "message": "Error al obtener el mensaje",  
  "data": null  
}
```

