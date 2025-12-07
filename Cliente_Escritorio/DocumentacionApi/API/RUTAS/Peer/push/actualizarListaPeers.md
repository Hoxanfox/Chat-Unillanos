Response
```json
{
  "action": "añadirPeer",
  "status": "success",
  "message": "Peer añadido y lista de peers actualizada",
  "data": {
    "listaPeers": [
      {
        "peerId": "uuid-peer-A1",
        "ip": "192.168.1.5",
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
}
```
Error 
1. Error general
```json
{
  "action": "añadirPeer",
  "status": "error",
  "message": "Error interno del servidor al añadir el peer"[cite: 41],
  "data": null [cite: 42]
}
```
2. Error especifico
```json
{
  "action": "añadirPeer",
  "status": "error",
  "message": "El peer ya se encuentra en la lista",
  "data": null [cite: 35]
}
```