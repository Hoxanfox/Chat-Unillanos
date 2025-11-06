Request
```json
{
  "action": "logoutUser",
  "data": {
    "userId": "id-del-usuario-que-cierra-sesion"
  }
}
```
Response
```json
{
  "action": "logoutUser",
  "success": true, // o "status": "success" [cite: 13]
  "message": "Sesión cerrada exitosamente",
  "data": null
}
```
Error
1. Error General
```json
{
  "action": "logoutUser",
  "status": "error",
  "message": "Error al cerrar sesión: [descripción del error]",
  "data": null
}
```
2. Error Específico
```json
{
  "action": "logoutUser",
  "status": "error",
  "message": "Usuario no autenticado o token inválido",
  "data": null
}
```