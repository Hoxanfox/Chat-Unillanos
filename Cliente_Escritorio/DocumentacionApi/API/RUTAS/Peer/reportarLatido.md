Request
```json
{
  "action": "reportarLatido",
  "data": {
    "peerId": "uuid-peer-B2",
    "ip": "192.168.1.5",
    "puerto": 9000,
  }
}
```
Response
```json
{
  "action": "reportarLatido",
  "status": "success",
  "message": "Latido recibido",
  "data": {
    "proximoLatidoMs": 30000 
  }
}
```
Error
1. Error General
```json
{
  "action": "reportarLatido",
  "status": "error",
  "message": "Peer no reconocido o no registrado",
  "data": {
    "peerId": "uuid-peer-DESCONOCIDO"
  }
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