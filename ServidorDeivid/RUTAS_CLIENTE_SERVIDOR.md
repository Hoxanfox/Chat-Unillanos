# Rutas Cliente-Servidor (CS) - Sistema de Chat

## 📋 Estado de Implementación

### ✅ Implementadas

#### 1. **ServicioAutenticacion**
- **Ruta**: `authenticateUser`
  - **Entrada**: `{ nombreUsuario: string, contrasena: string }`
  - **Salida exitosa**: 
    ```json
    {
      "action": "authenticateUser",
      "status": "success",
      "message": "Bienvenido",
      "data": {
        "idUsuario": "uuid",
        "nombre": "string",
        "email": "string",
        "photoIdServidor": "string",
        "estado": "ONLINE",
        "peerPadre": "uuid|null"
      }
    }
    ```
  - **Salida error**:
    ```json
    {
      "action": "authenticateUser",
      "status": "error",
      "message": "Credenciales incorrectas",
      "data": {
        "campo": "nombreUsuario|contrasena",
        "motivo": "Descripción del error"
      }
    }
    ```
  - **Funcionalidad**:
    - Valida credenciales contra BD
    - Actualiza estado del usuario a ONLINE
    - Registra sesión en el gestor
    - Retorna información completa del usuario

- **Ruta**: `logout`
  - **Entrada**: `{}`
  - **Salida**: `{ action: "logout", status: "success", message: "Sesión cerrada" }`
  - **Funcionalidad**:
    - Actualiza estado del usuario a OFFLINE
    - Desregistra sesión del gestor

---

#### 2. **ServicioArchivos** ✅ NUEVO
Gestiona subida y descarga de archivos por chunks. Los archivos se almacenan físicamente en `Bucket/` y los metadatos en BD.

##### **Subida de Archivos**

- **Ruta**: `startFileUpload` (requiere autenticación)
  - **Entrada**: 
    ```json
    {
      "fileName": "foto.jpg",
      "mimeType": "image/jpeg",
      "totalChunks": 10
    }
    ```
  - **Salida**: 
    ```json
    {
      "action": "startFileUpload",
      "status": "success",
      "data": {
        "uploadId": "uuid-del-upload"
      }
    }
    ```

- **Ruta**: `uploadFileForRegistration` (sin autenticación - para registro de usuarios)
  - **Entrada**: Igual que `startFileUpload`
  - **Salida**: Similar con `uploadId`

- **Ruta**: `uploadFileChunk`
  - **Entrada**:
    ```json
    {
      "uploadId": "uuid",
      "chunkNumber": 1,
      "chunkDataBase64": "base64_string"
    }
    ```
  - **Salida**: Confirmación de chunk recibido

- **Ruta**: `endFileUpload`
  - **Entrada**:
    ```json
    {
      "uploadId": "uuid",
      "fileHash": "sha256_hash"
    }
    ```
  - **Salida**:
    ```json
    {
      "action": "endFileUpload",
      "status": "success",
      "data": {
        "fileId": "user_photos/uuid_foto.jpg",
        "fileName": "foto.jpg",
        "size": 1024000,
        "mimeType": "image/jpeg",
        "hash": "sha256..."
      }
    }
    ```

##### **Descarga de Archivos**

- **Ruta**: `startFileDownload`
  - **Entrada**:
    ```json
    {
      "fileId": "user_photos/uuid_foto.jpg"
    }
    ```
  - **Salida**:
    ```json
    {
      "action": "startFileDownload",
      "status": "success",
      "data": {
        "downloadId": "uuid",
        "fileName": "foto.jpg",
        "mimeType": "image/jpeg",
        "fileSize": 1024000,
        "totalChunks": 10
      }
    }
    ```

- **Ruta**: `requestFileChunk`
  - **Entrada**:
    ```json
    {
      "downloadId": "uuid",
      "chunkNumber": 1
    }
    ```
  - **Salida**:
    ```json
    {
      "action": "downloadFileChunk_uuid_1",
      "status": "success",
      "data": {
        "chunkDataBase64": "base64_string",
        "chunkNumber": 1
      }
    }
    ```

##### **Características**:
- ✅ **Subida por chunks** de 512KB para manejar archivos grandes
- ✅ **Descarga por chunks** para optimizar transferencia
- ✅ **Validación de integridad** con hash SHA-256
- ✅ **Categorización automática**: user_photos, images, audio, documents, otros
- ✅ **Almacenamiento físico** en `Bucket/` con rutas relativas
- ✅ **Metadatos en BD** para búsqueda rápida
- ✅ **Notificación a red P2P** cuando se sube un archivo nuevo
- ✅ **Sanitización de nombres** para prevenir path traversal
- ✅ **Soporte para registro** sin autenticación (fotos de perfil en signup)

---

## 🔧 Pendientes de Implementación

### 2. **ServicioRegistro** (Alta prioridad)
- **Ruta**: `registerUser`
  - **Entrada**: 
    ```json
    {
      "nombre": "string",
      "email": "string",
      "contrasena": "string"
    }
    ```
  - **Validaciones**:
    - Email único
    - Formato de email válido
    - Contraseña mínimo 6 caracteres
  - **Salida**: Similar a authenticateUser

### 3. **ServicioUsuarios** (Gestión de perfil)
- **Ruta**: `updateProfile`
  - Actualizar nombre, email, contraseña
- **Ruta**: `getUserProfile`
  - Obtener información del usuario autenticado
- **Ruta**: `searchUsers`
  - Buscar usuarios por nombre o email
- **Ruta**: `getUsersOnline`
  - Lista de usuarios en línea

### 4. **ServicioFotos** ❌ YA NO ES NECESARIO
- ✅ **Integrado en ServicioArchivos**
- Las fotos de perfil se manejan como cualquier archivo
- FileId se guarda en campo `foto` de la tabla `usuarios`

### 5. **ServicioContactos** (Lista de amigos)
- **Ruta**: `addContact`
  - Enviar solicitud de amistad
- **Ruta**: `acceptContact`
  - Aceptar solicitud
- **Ruta**: `rejectContact`
  - Rechazar solicitud
- **Ruta**: `getContacts`
  - Listar contactos del usuario
- **Ruta**: `removeContact`
  - Eliminar contacto

### 6. **ServicioGrupos** (Gestión de grupos)
- **Ruta**: `createGroup`
  - Crear nuevo grupo
- **Ruta**: `addMember`
  - Agregar miembro a grupo
- **Ruta**: `removeMember`
  - Remover miembro
- **Ruta**: `updateGroup`
  - Actualizar info del grupo
- **Ruta**: `getGroups`
  - Listar grupos del usuario
- **Ruta**: `leaveGroup`
  - Salir de un grupo

### 7. **ServicioMensajes** (Historial centralizado)
- **Ruta**: `getMessageHistory`
  - Obtener historial con un contacto/grupo
- **Ruta**: `syncMessages`
  - Sincronizar mensajes pendientes
- **Ruta**: `deleteMessage`
  - Eliminar mensaje

### 8. **ServicioNotificaciones** (Ya existe parcialmente)
- **Ruta**: `sendNotification`
  - Enviar notificación push
- **Ruta**: `getNotifications`
  - Obtener notificaciones pendientes
- **Ruta**: `markAsRead`
  - Marcar notificación como leída

---

## 🏗️ Arquitectura Actual

### Flujo de una petición:
1. **Cliente** envía `DTORequest` con acción
2. **RouterMensajesCliente** enruta la petición según la acción
3. **Servicio específico** procesa la petición
4. **Repositorio** accede a la BD si es necesario
5. **Servicio** retorna `DTOResponse`
6. **Cliente** recibe la respuesta y actualiza UI

### Estructura del servidor:
```
Negocio/GestorClientes/
├── servicios/
│   ├── ServicioAutenticacion.java ✅
│   ├── ServicioNotificacionCliente.java ⚠️
│   ├── ServicioRegistro.java ❌ (pendiente)
│   ├── ServicioUsuarios.java ❌ (pendiente)
│   ├── ServicioFotos.java ❌ (pendiente)
│   ├── ServicioContactos.java ❌ (pendiente)
│   ├── ServicioGrupos.java ❌ (pendiente)
│   └── ServicioMensajes.java ❌ (pendiente)
│   └── ServicioArchivos.java ✅ NUEVO
```

---

## 📝 Notas Técnicas

### Mejoras implementadas en ServicioAutenticacion:
1. ✅ **Validación exhaustiva** de campos de entrada
2. ✅ **Mensajes de error descriptivos** con campo y motivo
3. ✅ **Logging** de eventos importantes (intentos fallidos, login exitoso)
4. ✅ **Gestión de estado** (ONLINE/OFFLINE)
5. ✅ **Vinculación de sesión** con usuario autenticado
6. ⚠️ **TODO**: Implementar hash de contraseñas con BCrypt

### Repositorio actualizado:
- ✅ `buscarPorEmail(String email)` - Para login
- ✅ `buscarPorId(UUID id)` - Para consultas por ID
- ✅ `actualizarEstado(UUID id, Estado estado)` - Para cambiar estado

---

## 🎯 Próximos Pasos Sugeridos

1. **Implementar ServicioRegistro** para completar el flujo de autenticación
2. **Implementar ServicioFotos** para la gestión de avatares
3. **Implementar ServicioUsuarios** para búsqueda y gestión de perfil
4. **Agregar hash de contraseñas** con BCrypt en producción
5. **Implementar ServicioContactos** para lista de amigos
6. **Implementar ServicioGrupos** para chats grupales
7. **Actualizar ServicioAutenticacion** para incluir `photoIdServidor` en respuesta
8. **Implementar ServicioRegistro** que use `uploadFileForRegistration` para foto
9. **Crear tabla SQL** con `init_archivos.sql`
10. **Configurar limpieza** de sesiones huérfanas (timeout)
11. **Implementar compresión** de imágenes en servidor (opcional)
12. **Agregar cuotas** de almacenamiento por usuario (opcional)

---

## 🔒 Seguridad

### Recomendaciones implementadas:
- ✅ **Sanitización de nombres** de archivo
- ✅ **Validación de hash** SHA-256
- ✅ **Prevención de path traversal**
- ✅ **Categorización forzada** (no permite rutas arbitrarias)
- ✅ **Autenticación requerida** (excepto para registro)

### Pendientes:
- ⚠️ **Escaneo de virus** en archivos subidos
- ⚠️ **Límite de tamaño** por archivo (ej: 10MB)
- ⚠️ **Rate limiting** en subidas
- ⚠️ **Cuotas de almacenamiento** por usuario
- ⚠️ **Compresión automática** de imágenes grandes
- ⚠️ **Limpieza de archivos** huérfanos/antiguos

---

**Fecha**: 2025-01-24
**Estado**: Servicios de Autenticación y Archivos completos ✅
**Compatible con**: Cliente que ya tiene `ArchivoServiceImpl` y `GestionArchivosImpl`
