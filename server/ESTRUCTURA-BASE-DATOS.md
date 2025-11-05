# Estructura de Base de Datos - Chat Unillanos

## 📋 Resumen General

La base de datos `chat_unillanos` está organizada en **8 tablas principales** que soportan toda la funcionalidad del sistema de mensajería.

---

## 🗂️ TABLAS PRINCIPALES

### 1. **usuarios**
Almacena la información de todos los usuarios del sistema.

**Campos:**
- `id` (VARCHAR 36, PK) - UUID único del usuario
- `nombre_usuario` (VARCHAR 100) - Nombre completo del usuario
- `email` (VARCHAR 100, UNIQUE) - Correo electrónico (único)
- `password_hash` (VARCHAR 255) - Contraseña hasheada con BCrypt
- `photo_id` (VARCHAR 255, NULLABLE) - ID de la foto de perfil
- `ip_address` (VARCHAR 45, NULLABLE) - Última IP de conexión
- `fecha_registro` (TIMESTAMP) - Fecha de creación de la cuenta
- `ultimo_acceso` (TIMESTAMP, NULLABLE) - Último acceso al sistema
- `estado` (ENUM: 'ONLINE', 'OFFLINE', 'AWAY') - Estado actual del usuario

**Índices:**
- `idx_email` - Búsqueda rápida por email
- `idx_estado` - Filtrado por estado

**Entidad Java:** `UsuarioEntity.java`

---

### 2. **canales**
Almacena los canales de comunicación grupal.

**Campos:**
- `id` (VARCHAR 36, PK) - UUID único del canal
- `nombre` (VARCHAR 100, UNIQUE) - Nombre del canal (único)
- `descripcion` (TEXT, NULLABLE) - Descripción del canal
- `creador_id` (VARCHAR 36, FK → usuarios.id) - Usuario que creó el canal
- `fecha_creacion` (TIMESTAMP) - Fecha de creación
- `activo` (BOOLEAN) - Si el canal está activo o archivado

**Índices:**
- `idx_nombre` - Búsqueda rápida por nombre
- `idx_creador` - Canales por creador

**Relaciones:**
- FK: `creador_id` → `usuarios.id` (ON DELETE CASCADE)

**Entidad Java:** `CanalEntity.java`

---

### 3. **canal_miembros**
Relación N:M entre usuarios y canales (tabla intermedia).

**Campos:**
- `canal_id` (VARCHAR 36, PK, FK → canales.id) - ID del canal
- `usuario_id` (VARCHAR 36, PK, FK → usuarios.id) - ID del usuario
- `fecha_union` (TIMESTAMP) - Cuándo se unió al canal
- `rol` (ENUM: 'ADMIN', 'MEMBER') - Rol del usuario en el canal

**Clave Primaria Compuesta:** (`canal_id`, `usuario_id`)

**Índices:**
- `idx_canal` - Miembros de un canal
- `idx_usuario` - Canales de un usuario

**Relaciones:**
- FK: `canal_id` → `canales.id` (ON DELETE CASCADE)
- FK: `usuario_id` → `usuarios.id` (ON DELETE CASCADE)

**Entidad Java:** `CanalMiembroEntity.java`

---

### 4. **mensajes**
Almacena mensajes directos (usuario a usuario) y mensajes de canal.

**Campos:**
- `id` (BIGINT, PK, AUTO_INCREMENT) - ID único del mensaje
- `remitente_id` (VARCHAR 36, FK → usuarios.id) - Usuario que envía
- `destinatario_id` (VARCHAR 36, FK → usuarios.id, NULLABLE) - Usuario destino (solo mensajes directos)
- `canal_id` (VARCHAR 36, FK → canales.id, NULLABLE) - Canal destino (solo mensajes de canal)
- `tipo` (ENUM: 'DIRECT', 'CHANNEL') - Tipo de mensaje
- `contenido` (TEXT) - Texto del mensaje
- `file_id` (VARCHAR 255, NULLABLE) - ID del archivo adjunto (si existe)
- `fecha_envio` (TIMESTAMP) - Cuándo se envió
- `estado` (VARCHAR 20) - Estado: 'ENVIADO', 'ENTREGADO', 'LEIDO'
- `fecha_entrega` (TIMESTAMP, NULLABLE) - Cuándo se entregó
- `fecha_lectura` (TIMESTAMP, NULLABLE) - Cuándo se leyó

**Restricción CHECK:**
- Si `tipo = 'DIRECT'`: `destinatario_id` debe ser NOT NULL y `canal_id` debe ser NULL
- Si `tipo = 'CHANNEL'`: `canal_id` debe ser NOT NULL y `destinatario_id` debe ser NULL

**Índices:**
- `idx_remitente` - Mensajes por remitente
- `idx_destinatario` - Mensajes por destinatario
- `idx_canal` - Mensajes por canal
- `idx_tipo` - Filtrado por tipo
- `idx_fecha_envio` - Ordenamiento cronológico
- `idx_estado` - Filtrado por estado

**Relaciones:**
- FK: `remitente_id` → `usuarios.id` (ON DELETE CASCADE)
- FK: `destinatario_id` → `usuarios.id` (ON DELETE CASCADE)
- FK: `canal_id` → `canales.id` (ON DELETE CASCADE)

**Entidad Java:** `MensajeEntity.java`

---

### 5. **archivos**
Almacena metadata de archivos multimedia (imágenes, audios, documentos).

**Campos:**
- `id` (VARCHAR 36, PK) - UUID único del archivo
- `nombre_original` (VARCHAR 255) - Nombre original del archivo
- `nombre_almacenado` (VARCHAR 255, UNIQUE) - Nombre UUID en disco
- `tipo_mime` (VARCHAR 100) - Tipo MIME (image/png, audio/mp3, etc.)
- `tipo_archivo` (ENUM: 'IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT', 'OTHER') - Categoría del archivo
- `hash_sha256` (VARCHAR 64, UNIQUE) - Hash para deduplicación
- `tamano_bytes` (BIGINT) - Tamaño en bytes
- `ruta_almacenamiento` (VARCHAR 500) - Ruta relativa en el sistema de archivos
- `usuario_id` (VARCHAR 36, FK → usuarios.id) - Usuario que subió el archivo
- `fecha_subida` (TIMESTAMP) - Cuándo se subió

**Índices:**
- `idx_hash` - Deduplicación por hash
- `idx_usuario` - Archivos por usuario
- `idx_tipo_archivo` - Filtrado por tipo

**Relaciones:**
- FK: `usuario_id` → `usuarios.id` (ON DELETE CASCADE)

**Entidad Java:** `ArchivoEntity.java`

---

## 🔧 TABLAS DE SOPORTE

### 6. **logs_sistema**
Registra eventos del sistema para auditoría y debugging.

**Campos:**
- `id` (BIGINT, PK, AUTO_INCREMENT) - ID único del log
- `timestamp` (TIMESTAMP) - Cuándo ocurrió el evento
- `tipo` (ENUM: 'LOGIN', 'LOGOUT', 'ERROR', 'INFO', 'SYSTEM') - Tipo de evento
- `usuario_id` (VARCHAR 36, FK → usuarios.id, NULLABLE) - Usuario relacionado
- `ip_address` (VARCHAR 45, NULLABLE) - IP desde donde ocurrió
- `accion` (VARCHAR 100) - Descripción corta de la acción
- `detalles` (TEXT, NULLABLE) - Detalles adicionales

**Índices:**
- `idx_timestamp` - Búsqueda cronológica
- `idx_tipo` - Filtrado por tipo
- `idx_usuario` - Logs por usuario

**Relaciones:**
- FK: `usuario_id` → `usuarios.id` (ON DELETE SET NULL)

**Entidad Java:** `LogEntity.java`

---

### 7. **notificaciones**
Almacena notificaciones para usuarios (solicitudes de amistad, invitaciones a canales, etc.).

**Campos:**
- `id` (VARCHAR 36, PK) - UUID único de la notificación
- `usuario_id` (VARCHAR 36, FK → usuarios.id) - Usuario que recibe la notificación
- `tipo` (VARCHAR 50) - Tipo: 'SOLICITUD_AMISTAD', 'INVITACION_CANAL', 'MENSAJE_DIRECTO', etc.
- `titulo` (VARCHAR 200) - Título de la notificación
- `mensaje` (TEXT) - Contenido de la notificación
- `remitente_id` (VARCHAR 36, FK → usuarios.id, NULLABLE) - Usuario que generó la notificación
- `canal_id` (VARCHAR 36, FK → canales.id, NULLABLE) - Canal relacionado (si aplica)
- `leida` (BOOLEAN) - Si fue leída o no
- `timestamp` (TIMESTAMP) - Cuándo se creó
- `accion` (VARCHAR 50, NULLABLE) - Acción asociada: 'aceptar', 'rechazar', 'ver', etc.
- `metadata` (TEXT, NULLABLE) - JSON con datos adicionales

**Índices:**
- `idx_usuario` - Notificaciones por usuario
- `idx_tipo` - Filtrado por tipo
- `idx_leida` - Filtrado por estado de lectura
- `idx_timestamp` - Ordenamiento cronológico

**Relaciones:**
- FK: `usuario_id` → `usuarios.id` (ON DELETE CASCADE)
- FK: `remitente_id` → `usuarios.id` (ON DELETE SET NULL)
- FK: `canal_id` → `canales.id` (ON DELETE SET NULL)

**Entidad Java:** `NotificacionEntity.java`

---

### 8. **contactos**
Gestiona las relaciones de amistad entre usuarios.

**Campos:**
- `id` (VARCHAR 36, PK) - UUID único de la relación
- `usuario_id` (VARCHAR 36, FK → usuarios.id) - Primer usuario
- `contacto_id` (VARCHAR 36, FK → usuarios.id) - Segundo usuario
- `estado` (ENUM: 'PENDIENTE', 'ACEPTADO', 'RECHAZADO', 'BLOQUEADO') - Estado de la relación
- `fecha_solicitud` (TIMESTAMP) - Cuándo se solicitó la amistad
- `fecha_respuesta` (TIMESTAMP, NULLABLE) - Cuándo se aceptó/rechazó
- `solicitado_por` (ENUM: 'usuario', 'contacto') - Quién envió la solicitud

**Restricción UNIQUE:** (`usuario_id`, `contacto_id`) - Evita duplicados

**Índices:**
- `idx_usuario` - Contactos de un usuario
- `idx_contacto` - Solicitudes recibidas
- `idx_estado` - Filtrado por estado

**Relaciones:**
- FK: `usuario_id` → `usuarios.id` (ON DELETE CASCADE)
- FK: `contacto_id` → `usuarios.id` (ON DELETE CASCADE)

**Entidad Java:** `ContactoEntity.java`

---

## 📊 Diagrama de Relaciones

```
usuarios (1) ──────< (N) canal_miembros (N) >────── (1) canales
    │                                                     │
    │ (1)                                           (1)  │
    │                                                     │
    ├──< (N) mensajes (remitente)                        │
    ├──< (N) mensajes (destinatario)                     │
    │        │                                            │
    │        └────────────────< (N) mensajes <───────────┘
    │
    ├──< (N) archivos
    ├──< (N) logs_sistema
    ├──< (N) notificaciones (usuario)
    ├──< (N) notificaciones (remitente)
    ├──< (N) contactos (usuario)
    └──< (N) contactos (contacto)
```

---

## ✅ Validaciones Importantes

### Mensajes:
- Un mensaje **DIRECT** debe tener `destinatario_id` y NO `canal_id`
- Un mensaje **CHANNEL** debe tener `canal_id` y NO `destinatario_id`

### Archivos:
- El `hash_sha256` es UNIQUE para implementar deduplicación
- El `nombre_almacenado` es UNIQUE para evitar colisiones en disco

### Contactos:
- La combinación (`usuario_id`, `contacto_id`) es UNIQUE
- Se debe manejar la bidireccionalidad en la lógica de negocio

---

## 🔐 Datos Iniciales

**Usuario Administrador:**
- Email: `admin@unillanos.edu.co`
- Password: `Admin123!`
- ID: `00000000-0000-0000-0000-000000000001`

---

## 📦 Entidades Java Correspondientes

| Tabla | Entidad Java | Ubicación |
|-------|-------------|-----------|
| usuarios | UsuarioEntity | `Datos/Entidades/.../entity/UsuarioEntity.java` |
| canales | CanalEntity | `Datos/Entidades/.../entity/CanalEntity.java` |
| canal_miembros | CanalMiembroEntity | `Datos/Entidades/.../entity/CanalMiembroEntity.java` |
| mensajes | MensajeEntity | `Datos/Entidades/.../entity/MensajeEntity.java` |
| archivos | ArchivoEntity | `Datos/Entidades/.../entity/ArchivoEntity.java` |
| logs_sistema | LogEntity | `Datos/Entidades/.../entity/LogEntity.java` |
| notificaciones | NotificacionEntity | `Datos/Entidades/.../entity/NotificacionEntity.java` |
| contactos | ContactoEntity | `Datos/Entidades/.../entity/ContactoEntity.java` |

---

## 🗑️ Tablas Eliminadas

**❌ chunk_sessions** - Ya no se utiliza. La subida de archivos grandes se maneja en memoria temporalmente.

---

## 📝 Notas de Implementación

1. **Timestamps:** Todos los timestamps se manejan en UTC en la BD y se convierten a `LocalDateTime` en Java
2. **UUIDs:** Los IDs tipo VARCHAR(36) almacenan UUIDs generados con `UUID.randomUUID().toString()`
3. **Enums:** Los ENUMs de MySQL se mapean a enums de Java (EstadoUsuario, TipoMensaje, etc.)
4. **Cascadas:** Los ON DELETE CASCADE garantizan integridad referencial
5. **Índices:** Los índices están optimizados para las consultas más frecuentes

---

**Última actualización:** 2025-10-17

