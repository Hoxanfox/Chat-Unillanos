Request
```json
{
  "action": "listarContactos",
  "data": {
    "usuarioId": "el-id-del-usuario-actual"
  }
}
```
Response
```json
{
  "action": "añadirPeer",
  "status": "success", // o "success": true
  "message": "Peer añadido y lista de peers actualizada",
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
    // ... más peers en la red
  ]
}
```
Error
1. Error General
```json
{
  "action": "añadirPeer",
  "status": "error",
  "message": "Error al añadir el peer: [descripción del error]",
  "data": null
}
```
2. Peer ya existe
```json
{
  "action": "añadirPeer",
  "status": "error",
  "message": "El peer ya se encuentra en la lista",
  "data": {
    "ip": "192.168.1.10",
    "puerto": 9000
  }
}
```
2. Error 
```json
{
  "action": "añadirPeer",
  "status": "error",
  "message": "Datos del peer inválidos",
  "data": {
    "campo": "ip",
    "motivo": "Formato de IP
```