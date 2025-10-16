---

## 📝 NOTAS DE IMPLEMENTACIÓN

1. **Passwords**: SIEMPRE enviar hasheados (SHA-256 o bcrypt)
2. **Tokens**: En producción, usar JWT para autenticación
3. **Sesiones**: Mantener sessionId en cada petición autenticada
4. **Sync**: Sincronizar BD local con servidor al login
5. **Cache**: Usar BD local como cache para modo offline
6. **Fotos**: Descargar fotos de perfil bajo demanda o al login
7. **Estados**: Respetar estados del servidor ('activo', 'inactivo', 'baneado')
8. **IDs**: SIEMPRE usar IDs del servidor para identificación

---

## ✅ VALIDACIONES

### Lado Cliente (antes de enviar):
- Email formato válido
- Password >= 8 caracteres
- Nombre no vacío
- Foto de perfil cargada (registro)

### Lado Servidor (al recibir):
- Email único en BD
- Password cumple política de seguridad
- photoId existe en sistema de archivos
- IP válida
- Rate limiting para prevenir ataques

---

## 🔄 SINCRONIZACIÓN

### Al Login:
1. Cliente recibe datos del servidor
2. Compara con BD local
3. Si hay diferencias → actualiza local
4. Descarga foto si photoId cambió
5. Notifica UI que datos están listos

### Al Registro:
1. Cliente envía datos al servidor
2. Servidor responde con IDs generados
3. Cliente guarda en BD local
4. Cliente notifica registro exitoso
5. Auto-login opcional

### Al Actualizar:
1. Cliente envía cambios al servidor
2. Servidor valida y actualiza
3. Cliente actualiza BD local
4. Cliente notifica cambio a UI
# Protocolo JSON: Gestión de Usuarios (Registro y Autenticación)

## 📋 REGISTRO DE USUARIO (Sign Up)

### 1. Registro Nuevo Usuario - `registerUser`

**Petición del Cliente:**
```json
{
  "action": "registerUser",
  "data": {
    "name": "Juan Pérez",
    "email": "juan.perez@example.com",
    "password": "SecurePass123!",
    "ip": "192.168.1.100",
    "photoId": "file-abc123-foto-perfil.jpg"
  }
}
```

**Campos:**
- `name`: Nombre completo del usuario
- `email`: Email único del usuario
- `password`: Contraseña (se enviará hasheada en producción)
- `ip`: Dirección IP del cliente
- `photoId`: ID del archivo de foto de perfil (obtenido de `uploadFileForRegistration`)

**Respuesta del Servidor (Éxito):**
```json
{
  "success": true,
  "message": "Usuario registrado exitosamente",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "fechaRegistro": "2025-10-16T14:30:00.000Z",
    "photoId": "file-abc123-foto-perfil.jpg"
  }
}
```

**Campos de Respuesta:**
- `userId`: UUID generado por el servidor (NUNCA por el cliente)
- `fechaRegistro`: Timestamp del registro en formato ISO 8601
- `photoId`: Confirmación del ID de la foto

**Respuesta de Error (Email Duplicado):**
```json
{
  "success": false,
  "message": "El email ya está registrado",
  "data": null
}
```

**Respuesta de Error (Datos Inválidos):**
```json
{
  "success": false,
  "message": "Datos de registro inválidos: El password debe tener al menos 8 caracteres",
  "data": null
}
```

---

## 🔐 AUTENTICACIÓN DE USUARIO (Login)

### 1. Autenticar Usuario - `authenticateUser`

**Petición del Cliente:**
```json
{
  "action": "authenticateUser",
  "data": {
    "email": "juan.perez@example.com",
    "password": "SecurePass123!"
  }
}
```

**Respuesta del Servidor (Éxito):**
```json
{
  "success": true,
  "message": "Autenticación exitosa",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Juan Pérez",
    "email": "juan.perez@example.com",
    "photoId": "file-abc123-foto-perfil.jpg",
    "estado": "activo",
    "fechaRegistro": "2025-10-16T14:30:00.000Z"
  }
}
```

**Campos de Respuesta:**
- `userId`: UUID del usuario (del servidor)
- `nombre`: Nombre completo del usuario
- `email`: Email del usuario
- `photoId`: ID de la foto de perfil en el servidor
- `estado`: Estado actual ('activo', 'inactivo', 'baneado')
- `fechaRegistro`: Fecha de cuando se registró

**Respuesta de Error (Credenciales Incorrectas):**
```json
{
  "success": false,
  "message": "Credenciales inválidas",
  "data": null
}
```

**Respuesta de Error (Usuario Baneado):**
```json
{
  "success": false,
  "message": "Usuario baneado. Contacte al administrador.",
  "data": {
    "estado": "baneado",
    "razon": "Violación de términos de servicio"
  }
}
```

**Respuesta de Error (Usuario Inactivo):**
```json
{
  "success": false,
  "message": "Usuario inactivo. Por favor reactive su cuenta.",
  "data": {
    "estado": "inactivo"
  }
}
```

---

## 👤 OBTENER INFORMACIÓN DE USUARIO

### 1. Obtener Perfil de Usuario - `getUserProfile`

**Petición del Cliente:**
```json
{
  "action": "getUserProfile",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**Respuesta del Servidor:**
```json
{
  "success": true,
  "message": "Perfil obtenido",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Juan Pérez",
    "email": "juan.perez@example.com",
    "photoId": "file-abc123-foto-perfil.jpg",
    "estado": "activo",
    "ip": "192.168.1.100",
    "fechaRegistro": "2025-10-16T14:30:00.000Z"
  }
}
```

---

## 🔄 ACTUALIZAR INFORMACIÓN DE USUARIO

### 1. Actualizar Perfil - `updateUserProfile`

**Petición del Cliente:**
```json
{
  "action": "updateUserProfile",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Juan Carlos Pérez",
    "photoId": "file-xyz789-nueva-foto.jpg"
  }
}
```

**Respuesta del Servidor:**
```json
{
  "success": true,
  "message": "Perfil actualizado exitosamente",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Juan Carlos Pérez",
    "photoId": "file-xyz789-nueva-foto.jpg"
  }
}
```

---

## 🚪 CERRAR SESIÓN (Logout)

### 1. Cerrar Sesión - `logoutUser`

**Petición del Cliente:**
```json
{
  "action": "logoutUser",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**Respuesta del Servidor:**
```json
{
  "success": true,
  "message": "Sesión cerrada exitosamente",
  "data": null
}
```

---

## 📊 FLUJO COMPLETO: REGISTRO Y LOGIN

### Caso 1: Registro de Nuevo Usuario

```
1. Cliente sube foto de perfil:
   uploadFileForRegistration → obtiene photoId

2. Cliente envía registro:
   POST registerUser
   {
     "name": "Juan Pérez",
     "email": "juan.perez@example.com",
     "password": "SecurePass123!",
     "photoId": "file-abc123-foto.jpg"
   }

3. Servidor valida y crea usuario:
   - Genera userId (UUID)
   - Hashea password
   - Guarda en BD del servidor
   - Retorna:
   {
     "userId": "550e8400...",
     "fechaRegistro": "2025-10-16T14:30:00.000Z",
     "photoId": "file-abc123-foto.jpg"
   }

4. Cliente guarda en BD local:
   - userId (del servidor)
   - nombre
   - email
   - photoId
   - fecha_registro
   - foto (bytes descargados)

5. Cliente notifica UI:
   Observer → "REGISTRO_EXITOSO"
```

### Caso 2: Login de Usuario Existente

```
1. Cliente envía credenciales:
   POST authenticateUser
   {
     "email": "juan.perez@example.com",
     "password": "SecurePass123!"
   }

2. Servidor valida:
   - Verifica email existe
   - Compara password hasheado
   - Verifica estado != 'baneado'
   - Retorna datos completos del usuario

3. Cliente recibe respuesta:
   {
     "userId": "550e8400...",
     "nombre": "Juan Pérez",
     "email": "...",
     "photoId": "file-abc123-foto.jpg",
     "estado": "activo"
   }

4. Cliente busca en BD local:
   - Si existe: actualiza datos
   - Si no existe: guarda nuevo registro
   - Descarga foto si photoId es diferente

5. Cliente guarda sesión:
   GestorSesionUsuario.setUserId(userId)
   GestorSesionUsuario.setUsuarioLogueado(usuario)

6. Cliente notifica UI:
   Observer → "AUTENTICACION_EXITOSA"
   Observer → "USUARIO_LOGUEADO"
```

### Caso 3: Actualizar Perfil

```
1. Cliente sube nueva foto (opcional):
   startFileUpload → obtiene nuevo photoId

2. Cliente envía actualización:
   POST updateUserProfile
   {
     "userId": "550e8400...",
     "nombre": "Juan Carlos Pérez",
     "photoId": "file-xyz789-nueva.jpg"
   }

3. Servidor actualiza y retorna confirmación

4. Cliente actualiza BD local:
   UPDATE usuarios SET nombre = ?, photoId = ? WHERE id_usuario = ?

5. Cliente notifica UI:
   Observer → "PERFIL_ACTUALIZADO"
```

---

## 🔑 IMPORTANTE: IDs DEL SERVIDOR

### El servidor SIEMPRE genera:
- ✅ `userId` - UUID del usuario
- ✅ `photoId` - ID del archivo de foto
- ✅ `fechaRegistro` - Timestamp del servidor

### El cliente NUNCA genera:
- ❌ UUID de usuario
- ❌ Timestamps de servidor
- ❌ IDs de archivos

### El cliente SÍ genera:
- ✅ IDs locales de BD (AUTOINCREMENT o UUID local)
- ✅ Timestamps locales para tracking

---

## 🗄️ PERSISTENCIA EN BD LOCAL

### Estructura de tabla `usuarios`:

```sql
CREATE TABLE usuarios (
    id_usuario UUID PRIMARY KEY,              -- userId del servidor
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    estado VARCHAR(10) DEFAULT 'activo',
    foto BLOB,                                -- Foto en bytes
    ip VARCHAR(45),
    fecha_registro TIMESTAMP,                 -- Del servidor
    photoIdServidor VARCHAR(255)              -- photoId del servidor
);
```

### Datos que se persisten localmente:

1. **Al Registrar:**
   - userId (del servidor)
   - nombre
   - email
   - foto (bytes)
   - photoIdServidor
   - fecha_registro (del servidor)
   - ip
   - estado = 'activo'

2. **Al Autenticar:**
   - Si usuario NO existe en BD local → INSERT completo
   - Si usuario SÍ existe → UPDATE datos recientes
   - Descargar foto si photoId cambió

3. **Al Actualizar Perfil:**
   - UPDATE campos modificados
   - Actualizar foto si cambió

---

## 🔔 EVENTOS DEL OBSERVADOR

### Para Registro:

| Evento | Datos | Descripción |
|--------|-------|-------------|
| `REGISTRO_INICIADO` | `DTORegistro` | Comenzó proceso de registro |
| `FOTO_PERFIL_SUBIDA` | `String photoId` | Foto subida exitosamente |
| `REGISTRO_EXITOSO` | `Usuario` | Usuario registrado y guardado en BD |
| `REGISTRO_ERROR` | `String mensaje` | Error en el registro |

### Para Autenticación:

| Evento | Datos | Descripción |
|--------|-------|-------------|
| `AUTENTICACION_INICIADA` | `String email` | Comenzó proceso de login |
| `AUTENTICACION_EXITOSA` | `Usuario` | Login exitoso |
| `USUARIO_LOGUEADO` | `Usuario` | Usuario guardado en sesión |
| `AUTENTICACION_ERROR` | `String mensaje` | Credenciales incorrectas |
| `USUARIO_BANEADO` | `String razon` | Usuario no puede acceder |

### Para Perfil:

| Evento | Datos | Descripción |
|--------|-------|-------------|
| `PERFIL_ACTUALIZADO` | `Usuario` | Perfil actualizado en BD |
| `FOTO_PERFIL_CAMBIADA` | `String photoId` | Nueva foto de perfil |
| `SESION_CERRADA` | `null` | Logout exitoso |

---

## 💡 CASOS DE ERROR COMUNES

### 1. Email Duplicado (Registro)
```json
{
  "success": false,
  "message": "El email ya está registrado",
  "data": null
}
```
**Acción del Cliente:** Mostrar error, sugerir login

### 2. Credenciales Incorrectas (Login)
```json
{
  "success": false,
  "message": "Email o contraseña incorrectos",
  "data": null
}
```
**Acción del Cliente:** Mostrar error, permitir reintentar

### 3. Usuario Baneado
```json
{
  "success": false,
  "message": "Cuenta suspendida",
  "data": {
    "estado": "baneado"
  }
}
```
**Acción del Cliente:** Mostrar mensaje, no permitir acceso

### 4. Red Sin Conexión
**Acción del Cliente:** 
- Intentar operación offline si es posible
- Guardar para sincronizar después
- Notificar al usuario

---

## 🚀 ACCIONES REGISTRADAS EN EL CLIENTE

### Registro:
- `registerUser` → Respuesta con userId, fechaRegistro, photoId

### Autenticación:
- `authenticateUser` → Respuesta con datos completos del usuario

### Perfil:
- `getUserProfile` → Obtener datos de usuario
- `updateUserProfile` → Actualizar información
- `logoutUser` → Cerrar sesión


