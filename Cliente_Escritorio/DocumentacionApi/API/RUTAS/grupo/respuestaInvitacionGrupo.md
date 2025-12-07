Request
```json
{
  "action": "respuestaInvitacionGrupo",
  "data": {
    "usuarioId": "el-id-del-usuario-actual",
    "peerUserId": "asdasdase uuid",
    "invitacionId": "id-unico-de-la-invitacion-creada",
	"estado": "TRUE o FALSE",
  }
}
```
Response 
- TRUE
```json
{
  "action": "respuestaInvitacionGrupo",
  "status": "success", // o "success": true [cite: 13]
  "message": "Respuesta a invitacion aceptada",
  "data":
    {
      "idGrupo": "uuid-canal-2",
      "idPeer": "uuid-canal-peer-2",
      "nombreCanal": "Proyecto Alpha",
      "ownerId": "uuid-creador-2",
      "tipo": "GRUPO o DIRECTO",
      "descripcion": "cadasdajsdkasjdkasjd",
	  "fechaCreacion":"2025-10-16T10:30:00Z"
    }
    // ... más canales

}
```
- FALSE
```json
{
  "action": "respuestaInvitacionGrupo",
  "status": "success", // o "success": true [cite: 13]
  "message": "Respuesta a invitacion rechazada",
  "data": {
  }
}
```
Error
1. Error General
```json
{
  "action": "respuestaInvitacionGrupo",
  "status": "error",
  "message": "Error al listar canales: [descripción del error]",
  "data": null
}
```
2. Error Específico
```json
{
  "action": "respuestaInvitacionGrupo",
  "status": "error",
  "message": "Usuario no autorizado para ver esta lista",
  "data": null
}
```