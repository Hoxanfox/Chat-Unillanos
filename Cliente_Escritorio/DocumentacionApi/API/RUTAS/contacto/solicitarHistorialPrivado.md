Request
```json
{
  "action": "solicitarHistorialPrivado",
  "data": {
    "remitenteId": "id-del-usuario-que-solicita",
    "peerRemitenteId": "uuid-peer-del-solicitante",
    "destinatarioId": "id-del-contacto-del-chat",
    "peerDestinatarioId": "uuid-peer-del-solicitante",
  }
}
```
Response
```json
{
  "action": "solicitarHistorialPrivado",
  "status": "success",
  "message": "Historial privado obtenido exitosamente",
  "data": [
    {
      "mensajeId": "msg-uuid-001",
      "remitenteId": "id-del-usuario-que-solicita",
      "destinatarioId": "id-del-contacto-del-chat",
      "peerRemitenteId": "uuid-peer-hist-A",
      "peerDestinoId": "uuid-peer-hist-B",
      "tipo": "texto",
      "contenido": "Hola, ¿cómo estás?",
      "fechaEnvio": "2025-10-28T14:40:00Z"
    },
    {
      "mensajeId": "msg-uuid-002",
      "remitenteId": "id-del-contacto-del-chat",
      "destinatarioId": "id-del-usuario-que-solicita",
      "peerRemitenteId": "uuid-peer-hist-C",
      "peerDestinoId": "uuid-peer-hist-D",
      "tipo": "audio",
      "audioId": "msg-uuid-001=",
      "fechaEnvio": "2025-10-28T14:42:00Z"
    },
    {
      "mensajeId": "msg-uuid-003",
      "remitenteId": "id-del-usuario-que-solicita",
      "destinatarioId": "id-del-contacto-del-chat",
      "peerRemitenteId": "uuid-peer-hist-E",
      "peerDestinoId": "uuid-peer-hist-F",
      "tipo": "texto",
      "contenido": "Todo bien por acá.",
      "fechaEnvio": "2025-10-28T14:43:00Z"
    }
  ]
}
```
Error
1. Error General
```json
{
  "action": "solicitarHistorialPrivado",
  "status": "error",
  "message": "Error al obtener el historial: [descripción del error]",
  "data": null
}
```

