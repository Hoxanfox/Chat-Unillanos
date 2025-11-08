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
  "action": "listarContactos",
  "status": "success", // o "success"
  "message": "Lista de contactos obtenida",
  "data": [
    {
      "id": "contacto-uuid-1",
      "idPeer": "servidor-uuid-1",
      "nombre": "Juan Pérez",
      "email": "juan.perez@example.com",
      "imagenBase64": "sadkjashdkjahskdjhasjkdhaskjdhqwql",
      "conectado": "OFFLINE,
    },
    {
      "id": "contacto-uuid-2",
      "idServidor": "servidor-uuid-2",
      "nombre": "Maria García",
      "email": "maria.garcia@example.com",
      "conectado": "ONLINE",
      "imagenBase64": "sadkjashdkjahskdjhasjkdhaskjdhqwql"
    }
    // ... más contactos
  ]
}
```
Error
1. Error General
```json
{
  "action": "listarContactos",
  "status": "error",
  "message": "Error al obtener contactos: [descripción del error]",
  "data": null
}
```

