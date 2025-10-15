# Módulo de Repositorio - Estructura CRUD Completa

## 📁 Estructura Organizada

```
Persistencia/Repositorio/src/main/java/repositorio/
├── conexion/
│   └── GestorConexionH2.java          ← Singleton con 12 tablas
├── usuario/
│   ├── IRepositorioUsuario.java       ← Interfaz CRUD
│   └── RepositorioUsuarioImpl.java    ← Implementación
├── canal/
│   ├── IRepositorioCanal.java         ← Interfaz CRUD
│   └── RepositorioCanalImpl.java      ← Implementación
├── contacto/
│   ├── IRepositorioContacto.java      ← Interfaz CRUD
│   └── RepositorioContactoImpl.java   ← Implementación
└── mensaje/
    ├── IRepositorioMensajeEnviadoContacto.java
    ├── RepositorioMensajeEnviadoContactoImpl.java
    ├── IRepositorioMensajeRecibidoContacto.java
    └── RepositorioMensajeRecibidoContactoImpl.java
```

---

## ✅ Repositorios Implementados

### 1. **RepositorioUsuario** (Completo)

**Operaciones CRUD:**
```java
// CREATE
void guardar(Usuario usuario)

// READ
Usuario obtenerPorId(UUID idUsuario)
Usuario obtenerPorEmail(String email)
List<Usuario> obtenerTodos()
boolean existePorEmail(String email)

// UPDATE
void actualizar(Usuario usuario)

// DELETE
void eliminar(UUID idUsuario)
```

**Características:**
- ✅ Usa entidades de Dominio (no DTOs)
- ✅ Maneja BLOB para fotos
- ✅ UUID como identificadores
- ✅ LocalDateTime para fechas
- ✅ Validación de email único
- ✅ Estados: activo, inactivo, baneado

---

### 2. **RepositorioCanal** (Completo)

**Operaciones CRUD:**
```java
void guardar(Canal canal)
Canal obtenerPorId(UUID idCanal)
Canal obtenerPorNombre(String nombre)
void actualizar(Canal canal)
void eliminar(UUID idCanal)
List<Canal> obtenerTodos()
List<Canal> obtenerPorAdministrador(UUID idAdministrador)
```

**Características:**
- ✅ Nombres únicos de canales
- ✅ Relación con usuario administrador
- ✅ Búsqueda por nombre y administrador

---

### 3. **RepositorioContacto** (Completo)

**Operaciones CRUD:**
```java
void guardar(Contacto contacto)
Contacto obtenerPorId(UUID idContacto)
void actualizar(Contacto contacto)
void eliminar(UUID idContacto)
List<Contacto> obtenerTodos()
List<Contacto> obtenerActivos()
```

**Características:**
- ✅ Estado boolean (activo/inactivo)
- ✅ Filtrado por estado activo

---

### 4. **RepositorioMensajeEnviadoContacto** (Completo)

**Operaciones CRUD:**
```java
void guardar(MensajeEnviadoContacto mensaje)
MensajeEnviadoContacto obtenerPorId(UUID idMensaje)
List<MensajeEnviadoContacto> obtenerPorRemitente(UUID idRemitente)
List<MensajeEnviadoContacto> obtenerPorDestinatario(UUID idDestinatario)
List<MensajeEnviadoContacto> obtenerConversacion(UUID idRemitente, UUID idDestinatario)
void eliminar(UUID idMensaje)
```

**Características:**
- ✅ Contenido como BLOB (texto/audio/imagen)
- ✅ Tipo de mensaje (texto, audio, imagen)
- ✅ Consulta de conversaciones completas
- ✅ Ordenamiento cronológico

---

### 5. **RepositorioMensajeRecibidoContacto** (Completo)

**Operaciones CRUD:**
```java
void guardar(MensajeRecibidoContacto mensaje)
MensajeRecibidoContacto obtenerPorId(UUID idMensaje)
List<MensajeRecibidoContacto> obtenerPorDestinatario(UUID idDestinatario)
List<MensajeRecibidoContacto> obtenerPorRemitente(UUID idRemitente)
List<MensajeRecibidoContacto> obtenerConversacion(UUID idRemitente, UUID idDestinatario)
void eliminar(UUID idMensaje)
```

---

## 🗄️ Base de Datos H2

### Tablas Creadas Automáticamente

1. **usuarios** - Información de usuarios
2. **canales** - Canales de comunicación
3. **contactos** - Lista de contactos
4. **invitaciones** - Invitaciones a canales
5. **mensaje_enviado_canal** - Mensajes enviados a canales
6. **mensaje_recibido_canal** - Mensajes recibidos de canales
7. **mensaje_enviado_contacto** - Mensajes enviados a contactos
8. **mensaje_recibido_contacto** - Mensajes recibidos de contactos
9. **administrador** - Relación Usuario-Canal (admin)
10. **invitacion_usuario** - Relación Usuario-Invitación
11. **canal_invitacion** - Relación Canal-Invitación
12. **canal_contacto** - Relación Canal-Contacto

---

## 🎯 Patrones Utilizados

### 1. **Repository Pattern**
Cada entidad de dominio tiene su repositorio dedicado con operaciones CRUD.

### 2. **Singleton Pattern**
`GestorConexionH2` mantiene una única conexión a la BD durante toda la ejecución.

### 3. **Separation of Concerns**
- **Dominio**: Entidades puras (POJOs)
- **Repositorio**: Lógica de persistencia
- **Conexión**: Gestión de BD

### 4. **Data Mapper Pattern**
Métodos privados `mapearResultSet()` convierten `ResultSet` a entidades de Dominio.

---

## 📝 Ejemplo de Uso

### Guardar un Usuario
```java
// Crear entidad de dominio
Usuario usuario = new Usuario();
usuario.setIdUsuario(UUID.randomUUID());
usuario.setNombre("Juan Pérez");
usuario.setEmail("juan@mail.com");
usuario.setEstado("activo");
usuario.setFechaRegistro(LocalDateTime.now());

// Guardar en BD
IRepositorioUsuario repo = new RepositorioUsuarioImpl();
repo.guardar(usuario);
```

### Obtener Usuario por Email
```java
IRepositorioUsuario repo = new RepositorioUsuarioImpl();
Usuario usuario = repo.obtenerPorEmail("juan@mail.com");

if (usuario != null) {
    System.out.println("Usuario encontrado: " + usuario.getNombre());
}
```

### Guardar Mensaje
```java
MensajeEnviadoContacto mensaje = new MensajeEnviadoContacto();
mensaje.setIdMensajeEnviadoContacto(UUID.randomUUID());
mensaje.setContenido("Hola!".getBytes());
mensaje.setFechaEnvio(LocalDateTime.now());
mensaje.setTipo("texto");
mensaje.setIdRemitente(uuidRemitente);
mensaje.setIdDestinatarioUsuario(uuidDestinatario);

IRepositorioMensajeEnviadoContacto repo = new RepositorioMensajeEnviadoContactoImpl();
repo.guardar(mensaje);
```

### Obtener Conversación
```java
IRepositorioMensajeEnviadoContacto repoEnviados = new RepositorioMensajeEnviadoContactoImpl();
IRepositorioMensajeRecibidoContacto repoRecibidos = new RepositorioMensajeRecibidoContactoImpl();

// Mensajes que YO envié
List<MensajeEnviadoContacto> enviados = repoEnviados.obtenerConversacion(miId, contactoId);

// Mensajes que YO recibí
List<MensajeRecibidoContacto> recibidos = repoRecibidos.obtenerConversacion(contactoId, miId);

// Combinar ambas listas y ordenar por fecha para tener el historial completo
```

---

## ✅ Características Implementadas

### Manejo de Tipos SQL
- ✅ **UUID** - `stmt.setObject(1, uuid)`
- ✅ **String** - `stmt.setString(2, texto)`
- ✅ **Boolean** - `stmt.setBoolean(3, estado)`
- ✅ **BLOB** - `stmt.setBytes(4, bytes)`
- ✅ **Timestamp** - `stmt.setTimestamp(5, Timestamp.valueOf(localDateTime))`

### Manejo de Nulos
```java
if (usuario.getFoto() != null) {
    stmt.setBytes(5, usuario.getFoto());
} else {
    stmt.setNull(5, Types.BLOB);
}
```

### Manejo de Errores
```java
try {
    // Operación de BD
} catch (SQLException e) {
    System.err.println("❌ [Repositorio]: Error: " + e.getMessage());
    throw new RuntimeException("Mensaje descriptivo", e);
}
```

### Try-with-Resources
```java
try (Connection conn = gestorConexion.getConexion();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    // Código
} // Cierre automático
```

---

## 🔒 Integridad Referencial

### Foreign Keys Implementadas
- `canales.id_administrador` → `usuarios.id_usuario`
- `mensaje_enviado_contacto.id_remitente` → `usuarios.id_usuario`
- `mensaje_enviado_contacto.id_destinatario_usuario` → `contactos.id_contacto`
- Y todas las demás según el esquema SQL

### Cascade Operations
- **ON DELETE CASCADE**: Eliminar usuario elimina sus mensajes
- **ON DELETE SET NULL**: Eliminar admin no elimina el canal

---

## 🚀 Inicialización Automática

La base de datos se inicializa automáticamente al crear el primer repositorio:

```java
// Primera instancia de cualquier repositorio
IRepositorioUsuario repo = new RepositorioUsuarioImpl();
// ↓
// Constructor llama a GestorConexionH2.getInstancia()
// ↓
// Gestor crea conexión y ejecuta CREATE TABLE IF NOT EXISTS para todas las tablas
// ↓
// ✅ BD lista para usar
```

**No se requiere configuración manual.**

---

## 📂 Ubicación de la Base de Datos

```
Cliente/
└── data/
    ├── chat_unillanos.mv.db    ← Base de datos H2
    └── chat_unillanos.trace.db ← Logs (si hay errores)
```

---

## ✅ Resumen

- ✅ **5 Repositorios** implementados con CRUD completo
- ✅ **12 Tablas** creadas automáticamente
- ✅ **Organización por paquetes** (usuario, canal, contacto, mensaje)
- ✅ **Uso de entidades de Dominio** (no DTOs)
- ✅ **UUID** para todos los IDs
- ✅ **LocalDateTime** para fechas
- ✅ **BLOB** para contenido binario
- ✅ **Manejo de errores robusto**
- ✅ **Operaciones específicas** (conversaciones, filtros, etc.)
- ✅ **Sin dependencias externas** (solo H2 y JDBC)

¡Módulo de Repositorio completamente funcional y listo para usar! 🎯

