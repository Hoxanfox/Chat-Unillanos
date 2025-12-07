Response
```json
{
  "action": "notificacionRespuestaNuevoMiembro", // Acción específica para esta notificación
  "status": "success",
  "message": "el nuevo usuario a contestado",
  "data": {
    "invitacionId": "id-unico-de-la-invitacion-creada",
    "grupoId": "id-del-canal-al-que-se-invita",
    "peerGrupoId": "id-del-canal-al-que-se-invita",
    "nombreCanal": "Nombre del Canal",
    "nuevoMiembro": {
	    "id": "id-del-contacto-que-se-quiere-invitar",
	    "peerId": "id-del-canal-al-que-se-invita",
    },
    "estado": "FALSE o TRUE",
    "fechaInvitacion": "2025-10-16T10:35:00Z"
  }
}
```
Error
```json
{
"action": "notificacionRespuestaNuevoMiembro",
  "status": "error",
  "message": "Error al invitar: [Razón del error]", // Ej: "Permisos insuficientes", "Usuario ya es miembro"
  "data": null
}
```