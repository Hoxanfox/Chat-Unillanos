# Estructura de Repositorios - Chat Unillanos

## 📂 Organización General

Los repositorios siguen el patrón **Repository Pattern** con una estructura clara de 3 capas:

```
Datos/Repositorios/src/main/java/com/unillanos/server/repository/
├── interfaces/          # Contratos de repositorios
├── impl/               # Implementaciones concretas
└── mappers/            # Mappers de ResultSet a Entidades
```

---

## 🔧 INTERFACES (Contratos)

Ubicación: `repository/interfaces/`

Define los contratos que deben implementar los repositorios. Cada interfaz representa operaciones CRUD y consultas específicas de una entidad.

### 1. **IUsuarioRepository.java**
Operaciones sobre usuarios.

**Métodos esperados:**
- `save(UsuarioEntity usuario): UsuarioEntity` - Crear/actualizar usuario
- `findById(String id): Optional<UsuarioEntity>` - Buscar por ID
- `findByEmail(String email): Optional<UsuarioEntity>` - Buscar por email
- `findAll(): List<UsuarioEntity>` - Obtener todos los usuarios
- `findByEstado(EstadoUsuario estado): List<UsuarioEntity>` - Filtrar por estado
- `updateEstado(String id, EstadoUsuario estado): boolean` - Actualizar estado
- `updateUltimoAcceso(String id, LocalDateTime ultimoAcceso): boolean` - Actualizar último acceso
- `delete(String id): boolean` - Eliminar usuario
- `existsByEmail(String email): boolean` - Verificar si existe email

---

### 2. **ICanalRepository.java**
Operaciones sobre canales.

**Métodos esperados:**
- `save(CanalEntity canal): CanalEntity` - Crear canal
- `findById(String id): Optional<CanalEntity>` - Buscar por ID
- `findByNombre(String nombre): Optional<CanalEntity>` - Buscar por nombre
- `findAll(): List<CanalEntity>` - Obtener todos los canales
- `findByCreadorId(String creadorId): List<CanalEntity>` - Canales creados por usuario
- `findActivos(): List<CanalEntity>` - Canales activos
- `update(CanalEntity canal): boolean` - Actualizar canal
- `updateActivo(String id, boolean activo): boolean` - Activar/desactivar
- `delete(String id): boolean` - Eliminar canal

---

### 3. **ICanalMiembroRepository.java**
Operaciones sobre la relación usuarios-canales.

**Métodos esperados:**
- `addMiembro(CanalMiembroEntity miembro): boolean` - Agregar miembro a canal
- `removeMiembro(String canalId, String usuarioId): boolean` - Remover miembro
- `findByCanalId(String canalId): List<CanalMiembroEntity>` - Miembros de un canal
- `findByUsuarioId(String usuarioId): List<CanalMiembroEntity>` - Canales de un usuario
- `findMiembro(String canalId, String usuarioId): Optional<CanalMiembroEntity>` - Buscar miembro específico
- `updateRol(String canalId, String usuarioId, RolCanal rol): boolean` - Cambiar rol
- `countMiembrosByCanal(String canalId): int` - Contar miembros
- `isMiembro(String canalId, String usuarioId): boolean` - Verificar membresía
- `isAdmin(String canalId, String usuarioId): boolean` - Verificar si es admin

---

### 4. **IMensajeRepository.java**
Operaciones sobre mensajes.

**Métodos esperados:**
- `save(MensajeEntity mensaje): MensajeEntity` - Guardar mensaje
- `findById(Long id): Optional<MensajeEntity>` - Buscar por ID
- `findMensajesDirectos(String usuarioId1, String usuarioId2, int limit): List<MensajeEntity>` - Mensajes entre usuarios
- `findMensajesCanal(String canalId, int limit): List<MensajeEntity>` - Mensajes de canal
- `findByRemitenteId(String remitenteId): List<MensajeEntity>` - Mensajes enviados por usuario
- `updateEstado(Long id, EstadoMensaje estado): boolean` - Actualizar estado
- `markAsEntregado(Long id): boolean` - Marcar como entregado
- `markAsLeido(Long id): boolean` - Marcar como leído
- `delete(Long id): boolean` - Eliminar mensaje
- `countNoLeidos(String usuarioId): int` - Contar mensajes no leídos

---

### 5. **IArchivoRepository.java**
Operaciones sobre archivos.

**Métodos esperados:**
- `save(ArchivoEntity archivo): ArchivoEntity` - Guardar metadata de archivo
- `findById(String id): Optional<ArchivoEntity>` - Buscar por ID
- `findByHash(String hash): Optional<ArchivoEntity>` - Buscar por hash (deduplicación)
- `findByUsuarioId(String usuarioId): List<ArchivoEntity>` - Archivos de un usuario
- `findByTipoArchivo(TipoArchivo tipo): List<ArchivoEntity>` - Filtrar por tipo
- `delete(String id): boolean` - Eliminar archivo
- `existsByHash(String hash): boolean` - Verificar si existe por hash

---

### 6. **ILogRepository.java**
Operaciones sobre logs del sistema.

**Métodos esperados:**
- `save(LogEntity log): LogEntity` - Guardar log
- `findById(Long id): Optional<LogEntity>` - Buscar por ID
- `findByTipo(String tipo): List<LogEntity>` - Filtrar por tipo
- `findByUsuarioId(String usuarioId): List<LogEntity>` - Logs de un usuario
- `findByFechaRango(LocalDateTime inicio, LocalDateTime fin): List<LogEntity>` - Logs en rango
- `findRecientes(int limit): List<LogEntity>` - Logs más recientes
- `deleteAntiguos(LocalDateTime fecha): int` - Limpiar logs antiguos

---

### 7. **INotificacionRepository.java**
Operaciones sobre notificaciones.

**Métodos esperados:**
- `save(NotificacionEntity notificacion): NotificacionEntity` - Crear notificación
- `findById(String id): Optional<NotificacionEntity>` - Buscar por ID
- `findByUsuarioId(String usuarioId): List<NotificacionEntity>` - Notificaciones de usuario
- `findNoLeidas(String usuarioId): List<NotificacionEntity>` - Notificaciones no leídas
- `markAsLeida(String id): boolean` - Marcar como leída
- `markAllAsLeida(String usuarioId): int` - Marcar todas como leídas
- `delete(String id): boolean` - Eliminar notificación
- `countNoLeidas(String usuarioId): int` - Contar no leídas

---

### 8. **IContactoRepository.java**
Operaciones sobre contactos/amistades.

**Métodos esperados:**
- `save(ContactoEntity contacto): ContactoEntity` - Crear solicitud de amistad
- `findById(String id): Optional<ContactoEntity>` - Buscar por ID
- `findByUsuarioId(String usuarioId): List<ContactoEntity>` - Contactos de usuario
- `findContacto(String usuarioId, String contactoId): Optional<ContactoEntity>` - Buscar relación específica
- `findPendientes(String usuarioId): List<ContactoEntity>` - Solicitudes pendientes
- `findAceptados(String usuarioId): List<ContactoEntity>` - Amigos aceptados
- `updateEstado(String id, String estado): boolean` - Actualizar estado
- `delete(String id): boolean` - Eliminar contacto
- `existeRelacion(String usuarioId, String contactoId): boolean` - Verificar si existe relación

---

## 🏗️ IMPLEMENTACIONES (impl/)

Ubicación: `repository/impl/`

Implementan las interfaces usando JDBC puro y conexiones desde el pool de configuración.

### Estructura de cada implementación:

```java
public class UsuarioRepositoryImpl implements IUsuarioRepository {
    
    private final ConnectionPool connectionPool;
    
    public UsuarioRepositoryImpl(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }
    
    @Override
    public UsuarioEntity save(UsuarioEntity usuario) {
        String sql = "INSERT INTO usuarios (...) VALUES (...)";
        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Implementación
        } catch (SQLException e) {
            throw new RepositoryException("Error al guardar usuario", e);
        }
    }
    
    // ... más métodos
}
```

**Archivos de implementación:**
1. `UsuarioRepositoryImpl.java`
2. `CanalRepositoryImpl.java`
3. `CanalMiembroRepositoryImpl.java`
4. `MensajeRepositoryImpl.java`
5. `ArchivoRepositoryImpl.java`
6. `LogRepositoryImpl.java`
7. `NotificacionRepositoryImpl.java`
8. `ContactoRepositoryImpl.java`

---

## 🗺️ MAPPERS (mappers/)

Ubicación: `repository/mappers/`

Convierten `ResultSet` de JDBC a entidades Java.

### Estructura de cada mapper:

```java
public class UsuarioMapper {
    
    /**
     * Mapea un ResultSet a UsuarioEntity.
     * 
     * @param rs ResultSet posicionado en la fila a mapear
     * @return UsuarioEntity con los datos del ResultSet
     * @throws SQLException si hay error en la lectura
     */
    public static UsuarioEntity mapToEntity(ResultSet rs) throws SQLException {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(rs.getString("id"));
        usuario.setNombre(rs.getString("nombre_usuario"));
        usuario.setEmail(rs.getString("email"));
        usuario.setPasswordHash(rs.getString("password_hash"));
        usuario.setPhotoId(rs.getString("photo_id"));
        usuario.setIpAddress(rs.getString("ip_address"));
        
        Timestamp fechaRegistro = rs.getTimestamp("fecha_registro");
        usuario.setFechaRegistro(fechaRegistro != null ? fechaRegistro.toLocalDateTime() : null);
        
        Timestamp ultimoAcceso = rs.getTimestamp("ultimo_acceso");
        usuario.setUltimoAcceso(ultimoAcceso != null ? ultimoAcceso.toLocalDateTime() : null);
        
        String estadoStr = rs.getString("estado");
        usuario.setEstado(EstadoUsuario.fromString(estadoStr));
        
        return usuario;
    }
}
```

**Archivos de mappers:**
1. `UsuarioMapper.java`
2. `CanalMapper.java`
3. `CanalMiembroMapper.java`
4. `MensajeMapper.java`
5. `ArchivoMapper.java`
6. `LogMapper.java`
7. `NotificacionMapper.java`
8. `ContactoMapper.java`

---

## 📦 Dependencias

Los repositorios dependen de:

1. **ConnectionPool** - Para obtener conexiones a la BD
2. **Entidades** - Módulo `Datos/Entidades`
3. **DTOs** - Para conversiones (opcional)
4. **MySQL Connector/J** - Driver JDBC

---

## 🎯 Mejores Prácticas Implementadas

### 1. **Try-with-resources**
```java
try (Connection conn = connectionPool.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    // Uso de recursos
} // Se cierran automáticamente
```

### 2. **PreparedStatements**
Siempre usar `PreparedStatement` para prevenir SQL Injection:
```java
String sql = "SELECT * FROM usuarios WHERE email = ?";
stmt.setString(1, email);
```

### 3. **Manejo de Transacciones**
Para operaciones múltiples:
```java
Connection conn = connectionPool.getConnection();
try {
    conn.setAutoCommit(false);
    // Operaciones
    conn.commit();
} catch (Exception e) {
    conn.rollback();
    throw e;
} finally {
    conn.setAutoCommit(true);
    conn.close();
}
```

### 4. **Optional para retornos únicos**
```java
public Optional<UsuarioEntity> findById(String id) {
    // Si encuentra, retorna Optional.of(entity)
    // Si no encuentra, retorna Optional.empty()
}
```

### 5. **Logging**
```java
private static final Logger logger = LoggerFactory.getLogger(UsuarioRepositoryImpl.class);

@Override
public UsuarioEntity save(UsuarioEntity usuario) {
    logger.debug("Guardando usuario: {}", usuario.getEmail());
    // Implementación
    logger.info("Usuario guardado exitosamente: {}", usuario.getId());
}
```

---

## 🚫 Archivos ELIMINADOS

**❌ IChunkSessionRepository.java**
**❌ ChunkSessionRepositoryImpl.java**
**❌ ChunkSessionMapper.java**

Estos archivos fueron eliminados porque ya no se utiliza la tabla `chunk_sessions`.

---

## 📊 Resumen de Archivos

| Tipo | Cantidad | Ubicación |
|------|----------|-----------|
| Interfaces | 8 | `repository/interfaces/` |
| Implementaciones | 8 | `repository/impl/` |
| Mappers | 8 | `repository/mappers/` |
| **TOTAL** | **24 archivos** | |

---

## 🔗 Flujo de Uso

```
Servicio
   │
   ├─> IUsuarioRepository (interfaz)
   │      │
   │      └─> UsuarioRepositoryImpl (implementación)
   │             │
   │             ├─> ConnectionPool (conexión BD)
   │             └─> UsuarioMapper (mapeo ResultSet)
   │                    │
   │                    └─> UsuarioEntity
```

---

## ✅ Checklist de Validación

- [x] Todas las entidades tienen su interfaz de repositorio
- [x] Todas las interfaces tienen su implementación
- [x] Todas las entidades tienen su mapper
- [x] Se eliminaron archivos relacionados con ChunkSession
- [x] Los repositorios usan PreparedStatement
- [x] Los repositorios usan try-with-resources
- [x] Los mappers manejan valores NULL correctamente
- [x] Se documentan todos los métodos públicos

---

**Última actualización:** 2025-10-17

