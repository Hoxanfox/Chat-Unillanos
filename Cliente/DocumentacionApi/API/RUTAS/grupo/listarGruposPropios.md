Request
```json
{
  "action": "listarGrupos",
  "data": {
    "usuarioId": "el-id-del-usuario-actual",
    "peerUserId" "asdasdase uuid", // ID del usuario que solicita la lista
  }
}
```
Response
```json
{
  "action": "listarGrupos",
  "status": "success", // o "success": true [cite: 13]
  "message": "Lista de canales obtenida",
  "data": [
    {
      "idGrupo": "uuid-canal-2",
      "idPeer": "uuid-canal-peer-2",
      "nombreCanal": "Proyecto Alpha",
      "ownerId": "uuid-creador-2",
      "tipo": "GRUPO o DIRECTO",
      "descripcion": "cadasdajsdkasjdkasjd",
	  "fechaCreacion":"2025-10-16T10:30:00Z",
	  "miembros":[
		  {
			  "id":"uuid",
			  "peerId":"uuid",
			  "estado":"ONLINE"
		  },
		  {
			  "id":"uuid",
			  "peerId":"uuid",
			  "estado":"OFFLINE", 
		  },
		  
	  ],
    },
    {
      "idGrupo": "uuid-canal-2",
      "idPeer": "uuid-canal-peer-2",
      "nombreCanal": "Proyecto Alpha",
      "ownerId": "uuid-creador-2",
      "tipo": "GRUPO o DIRECTO",
      "descripcion": "cadasdajsdkasjdkasjd",
	  "fechaCreacion":"2025-10-16T10:30:00Z",
	  "invitaciones":[
		  {
			  "id":"uuid",
				  "peerId":"uuid",
				  "estado":"TRUE",
				  "invitacionId": "id-unico-de-la-invitacion-creada",
		  },
		  {
			  "id":"uuid",
				  "peerId":"uuid",
				  "estado":"FALSE",
				  "invitacionId": "id-unico-de-la-invitacion-creada",
		  },
	  ],
	  
	  "miembros":[
		  {
			  "id":"uuid",
			  "peerId":"uuid",
			  "estado":"ONLINE"
		  },
		  {
			  "id":"uuid",
			  "peerId":"uuid",
			  "estado":"OFFLINE", 
		  },
		  
	  ],
    }
    // ... más canales
  ]
}
```
Error
1. Error General
```json
{
  "action": "listarGrupos",
  "status": "error",
  "message": "Error al listar canales: [descripción del error]",
  "data": null
}
```
2. Error Específico
```json
{
  "action": "listarGrupos",
  "status": "error",
  "message": "Usuario no autorizado para ver esta lista",
  "data": null
}
```