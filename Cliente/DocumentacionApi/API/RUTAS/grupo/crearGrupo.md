Request
```json
{
  "action": "crearGrupo",
  "data": {
    "creadorId": "123e4567-e89b-12d3-a456-426614174000",
    "servidorPadreId": "asdasdas uuid",
    "nombre": "Canal General",
    "descripcion": "Canal para discusiones generales del equipo",
    "tipo": "GRUPO o DIRECTO",
    ]
  }
```
Response
```json
{
  "action": "crearGrupo",
  "status": "success",
  "message": "Canal creado exitosamente",
  "data": {
    "channelId": "987fcdeb-51a2-43f8-9c7d-8e9f1a2b3c4d", // ID asignado por el servidor
    "peerId": "uuid asdasdasd",
    "nombre": "Canal General",                     // Nombre confirmado
    "creadorId": "123e4567-e89b-12d3-a456-426614174000", // ID del creador confirmado
    "tipo": "GRUPO o DIRECTO",
    "fechaCreacion": "2025-10-16T10:30:00Z",
    "membresias": [
    {
	    "contactoId": "uuid",
	    "peerContactoId": "uuid",
	    "estado": "TRUE o FALSE"
    },
    {
	    "contactoId": "uuid",
	    "peerContactoId": "uuid",
	    "estado": "TRUE o FALSE"
    }       // Timestamp de creación (ISO 8601)
  }
}
```
Error
```json
{
"action": "crearGrupo",
  "status": "error",
  "message": "El nombre del canal ya existe", // O el mensaje de error específico
  "data": null
```
