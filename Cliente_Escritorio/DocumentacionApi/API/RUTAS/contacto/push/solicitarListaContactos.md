Response
```json
{  
  "action": "solicitarListaContactos",  
  "status": "success",  
  "message": "Lista de contactos obtenida exitosamente",  
  "data": [  
    {  
      "id": "550e8400-e29b-41d4-a716-446655440000",  
      "nombre": "María García",  
      "email": "maria@example.com",  
      "photoId": "foto456.jpg",  
      "estado": "ONLINE",  
      "fechaRegistro": "2024-01-20T15:45:00"  
    },  
    {  
      "id": "660e8400-e29b-41d4-a716-446655440001",  
      "nombre": "Carlos López",  
      "email": "carlos@example.com",  
      "photoId": "foto789.jpg",  
      "estado": "OFFLINE",  
      "fechaRegistro": "2024-01-18T10:30:00"  
    }  
  ]  
}
```
Error
1. Error General
```json
{  
  "action": "solicitarListaContactos",  
  "status": "error",  
  "message": "Error al obtener los contactos",  
  "data": null  
}
```

