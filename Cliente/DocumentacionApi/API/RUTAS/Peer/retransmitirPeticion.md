Request
```json
{
  "action": "retransmitirPeticion",
  "data": {
    "peerOrigen": {
      "peerId": "uuid-servidor-A",
      "ip": "192.168.1.10",
      "puerto": 9000
    },
    "peticionCliente": {
      // JSON Original del Paso 1
      "action": "enviarMensajeDirecto",
      "data": {
        "peerDestinoId": "uuid-servidor-B",
        "peerRemitenteId": "uuid-servidor-A",
        "remitenteId": "id-cliente-A",
        "destinatarioId": "id-cliente-B",
        "tipo": "texto",
        "contenido": "Hola, ¿cómo estás?"
      }
    }
  }
}
```
Response
```json
{
  "action": "retransmitirPeticion",
  "status": "success",
  "message": "Petición del cliente procesada exitosamente.",
  "data": {
    "respuestaCliente": {
      // Respuesta de éxito del Paso 4
      "action": "enviarMensajeDirecto",
      "status": "success",
      "message": "Mensaje enviado",
      "data": {
        "mensajeId": "msg-uuid-generado-en-B",
        "fechaEnvio": "2025-10-28T14:40:00Z"
      }
    }
  }
}
```
Error
1. Error General
```json
{
  "action": "retransmitirPeticion",
  "status": "success", 
  "message": "Petición del cliente procesada, pero resultó en un error.",
  "data": {
    "respuestaCliente": {
      // AQUÍ VA LA RESPUESTA DE ERROR DE "crearGrupo"
      "status": "error",
      "message": "El nombre del canal 'Grupo Nuevo' ya existe",
      "data": null
    }
  }
}
```
