Request
```json
{
  "action": "listarPeersDisponibles",
  "data": {
    "usuarioId": "el-id-del-usuario-actual"
  }
}
```
Response
```json
{
  "action": "listarPeersDisponibles",
  "status": "success",
  "message": "Lista de peers y su estado obtenida",
  "data": [
    {
      "peerId": "uuid-peer-A1",
      "ip": "192.168.1.5",
      "puerto": 9000,
      "conectado": "ONLINE" 
    },
    {
      "peerId": "uuid-peer-B2",
      "ip": "192.168.1.10",
      "puerto": 9000,
      "conectado": "ONLINE"
    },
    {
      "peerId": "uuid-peer-C3",
      "ip": "192.168.1.15",
      "puerto": 9000,
      "conectado": "OFFLINE" 
    }
  ]
}
```
Error
1. Error General
```json
{
  "action": "listarPeersDisponibles",
  "status": "error",
  "message": "Error al obtener la lista de peers",
  "data": null
}
```