Request
```json
{
  "action": "authenticateUser",
  "data": {
    "nombreUsuario": "juan123",
    "password": "SecurePass123!"
  }
}
```
Response
```json
{
  "action": "authenticateUser",
  "success": true, // o "status": "success"
  "message": "Autenticación exitosa",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Juan Pérez",
    "email": "juan.perez@example.com",
    "imagenBase64": "sdasdbkjashdkashdk"
  }
}
```
Error
1. Error de Credenciales Inválidas
```json
{  
  "action": "authenticateUser",  
  "status": "error",  
  "message": "Credenciales incorrectas",  
  "data": null  
}
```
2. Error de Usuario No Encontrado
```json
{  
  "action": "authenticateUser",  
  "status": "error",  
  "message": "Usuario no encontrado",  
  "data": null  
}
```
3. Error del Servidor
```json
{  
  "action": "authenticateUser",  
  "status": "error",  
  "message": "Error interno del servidor",  
  "data": null  
}
```
4. Error del Servidor
```json
{  
  "action": "authenticateUser",  
  "status": "error",  
  "message": "Email o contraseña inválidos",  
  "data": {  
    "campo": "nombra",  
    "motivo": "Formato de email inválido"  
  }  
}
```