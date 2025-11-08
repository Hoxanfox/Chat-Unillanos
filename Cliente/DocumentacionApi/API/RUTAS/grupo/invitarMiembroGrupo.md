Request
```json
{
  "action": "invitarMiembroGrupo",
  "data": {
    "grupoId": "id-del-canal-al-que-se-invita",
    "peerGrupoId": "id-del-canal-al-que-se-invita",
    "usuarioIdInvitado": {
	    "id": "id-del-contacto-que-se-quiere-invitar",
	    "peerId": "id-del-canal-al-que-se-invita",
    },
  }
}
```
Response
```json
{
"action": "invitarMiembroGrupo",
  "status": "success",
  "message": "Invitación enviada exitosamente",
  "data": {
    "invitacionId": "id-unico-de-la-invitacion-creada",
    "grupoId": "id-del-canal-al-que-se-invita",
    "peerGrupoId": "id-del-canal-al-que-se-invita",
    "usuarioIdInvitado": {
	    "id": "id-del-contacto-que-se-quiere-invitar",
	    "peerId": "id-del-canal-al-que-se-invita",
    },
    "estado": "PENDIENTE", // Estado inicial de la invitación
    "fechaInvitacion": "2025-10-16T10:35:00Z" // Timestamp del servidor
  }
}
```
Error
```json
{
"action": "invitarMiembroGrupo",
  "status": "error",
  "message": "El nombre del canal ya existe", // O el mensaje de error específico
  "data": null
  }
```
