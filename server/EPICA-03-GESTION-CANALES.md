# Épica 3: Gestión de Canales - Plan Detallado

## Objetivo General

Implementar el sistema completo de gestión de canales grupales: creación de canales, unirse/salir de canales, gestión de miembros, roles de administrador y listado de canales. Los usuarios podrán crear canales de comunicación grupal, invitar a otros usuarios, gestionar permisos y visualizar información de los canales.

## Contexto Actual

✅ **Ya Implementado (Épicas 1 y 2):**
- ✅ Infraestructura de excepciones personalizadas
- ✅ GlobalExceptionHandler para manejo centralizado de errores
- ✅ LoggerService con persistencia asíncrona en BD
- ✅ LogRepository para auditoría
- ✅ ConnectionManager para gestión de conexiones en tiempo real
- ✅ ActionDispatcher con enrutamiento de acciones
- ✅ Servidor Netty funcional con hilos virtuales
- ✅ DTORequest y DTOResponse base
- ✅ Sistema completo de gestión de usuarios (registro, login, perfil, estados)
- ✅ UsuarioRepository completo con JDBC
- ✅ AutenticacionService funcional
- ✅ Validadores de usuario (email, password, nombre)

⚠️ **Pendiente de Implementar:**
- ❌ DTOs específicos para canales
- ❌ Validadores de canales (nombre, descripción)
- ❌ Entidades de canales y relaciones (CanalEntity, CanalMiembroEntity)
- ❌ CanalRepository con operaciones JDBC
- ❌ CanalMiembroRepository para gestionar la relación N:M
- ❌ CanalService con lógica de negocio
- ❌ Acciones en ActionDispatcher: crear canal, unirse, listar, gestionar miembros

---

## Componentes a Implementar

### 1. DTOs para Gestión de Canales

**Ubicación:** `Infraestructura/DTOs/src/main/java/com/unillanos/server/dto/`

#### DTOCrearCanal.java
```java
public class DTOCrearCanal {
    private String creadorId;       // Requerido - Usuario que crea el canal
    private String nombre;          // Requerido, único, 3-50 caracteres
    private String descripcion;     // Opcional, máximo 200 caracteres
}
```

#### DTOCanal.java (Response)
```java
public class DTOCanal {
    private String id;              // UUID del canal
    private String nombre;
    private String descripcion;
    private String creadorId;
    private String fechaCreacion;   // ISO-8601
    private boolean activo;
    private int cantidadMiembros;   // Cantidad de miembros en el canal
}
```

#### DTOUnirseCanal.java
```java
public class DTOUnirseCanal {
    private String usuarioId;       // Requerido
    private String canalId;         // Requerido
}
```

#### DTOSalirCanal.java
```java
public class DTOSalirCanal {
    private String usuarioId;       // Requerido
    private String canalId;         // Requerido
}
```

#### DTOListarCanales.java
```java
public class DTOListarCanales {
    private String usuarioId;       // Opcional - Si se provee, lista solo los canales del usuario
    private int limit;              // Opcional - Por defecto 50
    private int offset;             // Opcional - Por defecto 0
}
```

#### DTOGestionarMiembro.java
```java
public class DTOGestionarMiembro {
    private String adminId;         // Requerido - Usuario que realiza la acción (debe ser admin)
    private String canalId;         // Requerido
    private String usuarioId;       // Requerido - Usuario a agregar/remover/cambiar rol
    private String accion;          // "AGREGAR", "REMOVER", "CAMBIAR_ROL"
    private String nuevoRol;        // Opcional - "admin" o "member" (solo para CAMBIAR_ROL)
}
```

#### DTOMiembroCanal.java (Response)
```java
public class DTOMiembroCanal {
    private String usuarioId;
    private String nombreUsuario;
    private String rol;             // "admin" o "member"
    private String fechaUnion;      // ISO-8601
}
```

---

### 2. Validadores de Canales

**Ubicación:** `LogicaNegocio/Validadores/src/main/java/com/unillanos/server/validation/`

#### NombreCanalValidator.java (NUEVO)
```java
public class NombreCanalValidator {
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;
    
    public static void validate(String nombre) throws ValidationException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ValidationException("El nombre del canal es requerido", "nombre");
        }
        
        String nombreTrim = nombre.trim();
        
        if (nombreTrim.length() < MIN_LENGTH) {
            throw new ValidationException(
                "El nombre del canal debe tener al menos " + MIN_LENGTH + " caracteres", 
                "nombre"
            );
        }
        
        if (nombreTrim.length() > MAX_LENGTH) {
            throw new ValidationException(
                "El nombre del canal es demasiado largo (máx " + MAX_LENGTH + ")", 
                "nombre"
            );
        }
        
        // Validar que no contenga caracteres especiales prohibidos
        if (!nombreTrim.matches("^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s\\-_]+$")) {
            throw new ValidationException(
                "El nombre del canal solo puede contener letras, números, espacios, guiones y guiones bajos", 
                "nombre"
            );
        }
    }
}
```

#### DescripcionCanalValidator.java (NUEVO)
```java
public class DescripcionCanalValidator {
    private static final int MAX_LENGTH = 200;
    
    public static void validate(String descripcion) throws ValidationException {
        // La descripción es opcional, si es null o vacía no se valida
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return;
        }
        
        if (descripcion.length() > MAX_LENGTH) {
            throw new ValidationException(
                "La descripción es demasiado larga (máx " + MAX_LENGTH + " caracteres)", 
                "descripcion"
            );
        }
    }
}
```

#### CrearCanalValidator.java (NUEVO - Validador compuesto)
```java
public class CrearCanalValidator {
    public static void validate(DTOCrearCanal dto) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("Los datos del canal son requeridos", "dto");
        }
        
        // Validar creadorId
        if (dto.getCreadorId() == null || dto.getCreadorId().trim().isEmpty()) {
            throw new ValidationException("El ID del creador es requerido", "creadorId");
        }
        
        // Validar nombre
        NombreCanalValidator.validate(dto.getNombre());
        
        // Validar descripción (opcional)
        DescripcionCanalValidator.validate(dto.getDescripcion());
    }
}
```

---

### 3. Entidades y Mappers de Canales

**Ubicación:** `Datos/Repositorios/src/main/java/com/unillanos/server/repository/models/`

#### RolCanal.java (NUEVO - Enum)
```java
public enum RolCanal {
    ADMIN,    // Administrador del canal (puede agregar/remover miembros)
    MEMBER;   // Miembro regular del canal
    
    public static RolCanal fromString(String rol) {
        if (rol == null || rol.trim().isEmpty()) {
            return MEMBER; // Por defecto
        }
        
        try {
            return RolCanal.valueOf(rol.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEMBER;
        }
    }
}
```

#### CanalEntity.java (NUEVO)
```java
public class CanalEntity {
    private String id;              // UUID
    private String nombre;
    private String descripcion;
    private String creadorId;
    private LocalDateTime fechaCreacion;
    private boolean activo;
    
    // Constructores
    public CanalEntity() {}
    
    public CanalEntity(String id, String nombre, String descripcion, String creadorId,
                       LocalDateTime fechaCreacion, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.creadorId = creadorId;
        this.fechaCreacion = fechaCreacion;
        this.activo = activo;
    }
    
    // Getters y Setters
    // ...
    
    /**
     * Convierte la entidad a DTO.
     */
    public DTOCanal toDTO(int cantidadMiembros) {
        DTOCanal dto = new DTOCanal();
        dto.setId(this.id);
        dto.setNombre(this.nombre);
        dto.setDescripcion(this.descripcion);
        dto.setCreadorId(this.creadorId);
        dto.setFechaCreacion(this.fechaCreacion != null ? this.fechaCreacion.toString() : null);
        dto.setActivo(this.activo);
        dto.setCantidadMiembros(cantidadMiembros);
        return dto;
    }
}
```

#### CanalMiembroEntity.java (NUEVO)
```java
public class CanalMiembroEntity {
    private String canalId;
    private String usuarioId;
    private LocalDateTime fechaUnion;
    private RolCanal rol;
    
    // Constructores
    public CanalMiembroEntity() {}
    
    public CanalMiembroEntity(String canalId, String usuarioId, LocalDateTime fechaUnion, RolCanal rol) {
        this.canalId = canalId;
        this.usuarioId = usuarioId;
        this.fechaUnion = fechaUnion;
        this.rol = rol;
    }
    
    // Getters y Setters
    // ...
}
```

#### CanalMapper.java (NUEVO)
```java
public class CanalMapper {
    public static CanalEntity mapRow(ResultSet rs) throws SQLException {
        CanalEntity canal = new CanalEntity();
        canal.setId(rs.getString("id"));
        canal.setNombre(rs.getString("nombre"));
        canal.setDescripcion(rs.getString("descripcion"));
        canal.setCreadorId(rs.getString("creador_id"));
        
        Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
        if (fechaCreacion != null) {
            canal.setFechaCreacion(fechaCreacion.toLocalDateTime());
        }
        
        canal.setActivo(rs.getBoolean("activo"));
        
        return canal;
    }
}
```

#### CanalMiembroMapper.java (NUEVO)
```java
public class CanalMiembroMapper {
    public static CanalMiembroEntity mapRow(ResultSet rs) throws SQLException {
        CanalMiembroEntity miembro = new CanalMiembroEntity();
        miembro.setCanalId(rs.getString("canal_id"));
        miembro.setUsuarioId(rs.getString("usuario_id"));
        
        Timestamp fechaUnion = rs.getTimestamp("fecha_union");
        if (fechaUnion != null) {
            miembro.setFechaUnion(fechaUnion.toLocalDateTime());
        }
        
        String rolStr = rs.getString("rol");
        miembro.setRol(RolCanal.fromString(rolStr));
        
        return miembro;
    }
}
```

---

### 4. Interfaces de Repositorios

**Ubicación:** `Datos/Repositorios/src/main/java/com/unillanos/server/repository/interfaces/`

#### ICanalRepository.java (NUEVO)
```java
public interface ICanalRepository {
    
    // --- MÉTODOS DE CONSULTA ---
    
    Optional<CanalEntity> findById(String id);
    Optional<CanalEntity> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
    List<CanalEntity> findAll(int limit, int offset);
    List<CanalEntity> findByUsuario(String usuarioId, int limit, int offset);
    int countMiembros(String canalId);
    
    // --- MÉTODOS DE ESCRITURA ---
    
    CanalEntity save(CanalEntity canal);
    void update(CanalEntity canal);
    void updateActivo(String id, boolean activo);
    void deleteById(String id);
}
```

#### ICanalMiembroRepository.java (NUEVO)
```java
public interface ICanalMiembroRepository {
    
    // --- MÉTODOS DE CONSULTA ---
    
    Optional<CanalMiembroEntity> findByUsuarioAndCanal(String usuarioId, String canalId);
    List<CanalMiembroEntity> findMiembrosByCanal(String canalId);
    List<CanalEntity> findCanalesByUsuario(String usuarioId, int limit, int offset);
    boolean esAdministrador(String usuarioId, String canalId);
    boolean esMiembro(String usuarioId, String canalId);
    int countMiembros(String canalId);
    
    // --- MÉTODOS DE ESCRITURA ---
    
    void agregarMiembro(String canalId, String usuarioId, RolCanal rol);
    void removerMiembro(String canalId, String usuarioId);
    void actualizarRol(String canalId, String usuarioId, RolCanal nuevoRol);
}
```

---

### 5. Implementaciones de Repositorios

**Ubicación:** `Datos/Repositorios/src/main/java/com/unillanos/server/repository/impl/`

#### CanalRepositoryImpl.java (NUEVO)

**Métodos a Implementar:**

```java
@Repository
public class CanalRepositoryImpl implements ICanalRepository {
    
    private final DataSource dataSource;
    
    @Override
    public Optional<CanalEntity> findById(String id) {
        // SELECT * FROM canales WHERE id = ?
    }
    
    @Override
    public Optional<CanalEntity> findByNombre(String nombre) {
        // SELECT * FROM canales WHERE nombre = ?
    }
    
    @Override
    public boolean existsByNombre(String nombre) {
        // SELECT COUNT(*) FROM canales WHERE nombre = ?
    }
    
    @Override
    public List<CanalEntity> findAll(int limit, int offset) {
        // SELECT * FROM canales WHERE activo = true ORDER BY fecha_creacion DESC LIMIT ? OFFSET ?
    }
    
    @Override
    public List<CanalEntity> findByUsuario(String usuarioId, int limit, int offset) {
        // SELECT c.* FROM canales c
        // INNER JOIN canal_miembros cm ON c.id = cm.canal_id
        // WHERE cm.usuario_id = ? AND c.activo = true
        // ORDER BY cm.fecha_union DESC LIMIT ? OFFSET ?
    }
    
    @Override
    public int countMiembros(String canalId) {
        // SELECT COUNT(*) FROM canal_miembros WHERE canal_id = ?
    }
    
    @Override
    public CanalEntity save(CanalEntity canal) {
        // INSERT INTO canales (id, nombre, descripcion, creador_id, fecha_creacion, activo)
        // VALUES (?, ?, ?, ?, ?, ?)
    }
    
    @Override
    public void update(CanalEntity canal) {
        // UPDATE canales SET nombre = ?, descripcion = ? WHERE id = ?
    }
    
    @Override
    public void updateActivo(String id, boolean activo) {
        // UPDATE canales SET activo = ? WHERE id = ?
    }
    
    @Override
    public void deleteById(String id) {
        // DELETE FROM canales WHERE id = ?
    }
}
```

#### CanalMiembroRepositoryImpl.java (NUEVO)

**Métodos a Implementar:**

```java
@Repository
public class CanalMiembroRepositoryImpl implements ICanalMiembroRepository {
    
    private final DataSource dataSource;
    
    @Override
    public Optional<CanalMiembroEntity> findByUsuarioAndCanal(String usuarioId, String canalId) {
        // SELECT * FROM canal_miembros WHERE usuario_id = ? AND canal_id = ?
    }
    
    @Override
    public List<CanalMiembroEntity> findMiembrosByCanal(String canalId) {
        // SELECT * FROM canal_miembros WHERE canal_id = ? ORDER BY fecha_union ASC
    }
    
    @Override
    public List<CanalEntity> findCanalesByUsuario(String usuarioId, int limit, int offset) {
        // SELECT c.* FROM canales c
        // INNER JOIN canal_miembros cm ON c.id = cm.canal_id
        // WHERE cm.usuario_id = ? ORDER BY cm.fecha_union DESC LIMIT ? OFFSET ?
    }
    
    @Override
    public boolean esAdministrador(String usuarioId, String canalId) {
        // SELECT COUNT(*) FROM canal_miembros
        // WHERE usuario_id = ? AND canal_id = ? AND rol = 'ADMIN'
    }
    
    @Override
    public boolean esMiembro(String usuarioId, String canalId) {
        // SELECT COUNT(*) FROM canal_miembros WHERE usuario_id = ? AND canal_id = ?
    }
    
    @Override
    public int countMiembros(String canalId) {
        // SELECT COUNT(*) FROM canal_miembros WHERE canal_id = ?
    }
    
    @Override
    public void agregarMiembro(String canalId, String usuarioId, RolCanal rol) {
        // INSERT INTO canal_miembros (canal_id, usuario_id, fecha_union, rol)
        // VALUES (?, ?, ?, ?)
    }
    
    @Override
    public void removerMiembro(String canalId, String usuarioId) {
        // DELETE FROM canal_miembros WHERE canal_id = ? AND usuario_id = ?
    }
    
    @Override
    public void actualizarRol(String canalId, String usuarioId, RolCanal nuevoRol) {
        // UPDATE canal_miembros SET rol = ? WHERE canal_id = ? AND usuario_id = ?
    }
}
```

**Consideraciones:**
- Usar `PreparedStatement` en TODAS las operaciones
- Usar mappers para convertir filas a entidades
- Cerrar recursos en bloques `finally`
- Lanzar `RepositoryException` en caso de error SQL
- El ID del canal se genera con `UUID.randomUUID().toString()` antes de insertar

---

### 6. CanalService

**Ubicación:** `LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/CanalService.java`

**Responsabilidades:**
- Crear nuevos canales
- Unirse/salir de canales
- Listar canales (todos o del usuario)
- Gestionar miembros (agregar, remover, cambiar rol)
- Validar permisos de administrador
- Integrar con ConnectionManager para notificaciones en tiempo real
- Integrar con LoggerService para auditoría

**Métodos Principales:**

```java
@Service
public class CanalService {
    
    private final ICanalRepository canalRepository;
    private final ICanalMiembroRepository canalMiembroRepository;
    private final IUsuarioRepository usuarioRepository;
    private final LoggerService loggerService;
    private final ConnectionManager connectionManager;
    
    /**
     * Crea un nuevo canal.
     * 
     * @param dto Datos del canal a crear
     * @return DTOResponse con el canal creado
     */
    public DTOResponse crearCanal(DTOCrearCanal dto) {
        // 1. Validar datos con CrearCanalValidator
        // 2. Verificar que el creador existe
        // 3. Verificar que el nombre del canal no exista
        // 4. Crear CanalEntity con ID UUID y activo = true
        // 5. Guardar canal en BD
        // 6. Agregar al creador como ADMIN en canal_miembros
        // 7. Registrar en logs
        // 8. Retornar DTOResponse.success con DTOCanal
    }
    
    /**
     * Permite a un usuario unirse a un canal.
     * 
     * @param dto Datos de unión al canal
     * @return DTOResponse confirmando la unión
     */
    public DTOResponse unirseCanal(DTOUnirseCanal dto) {
        // 1. Validar que usuarioId y canalId no estén vacíos
        // 2. Verificar que el usuario existe
        // 3. Verificar que el canal existe y está activo
        // 4. Verificar que el usuario NO sea ya miembro
        // 5. Agregar usuario al canal con rol MEMBER
        // 6. Registrar en logs
        // 7. Notificar a todos los miembros del canal sobre el nuevo miembro
        // 8. Retornar DTOResponse.success
    }
    
    /**
     * Permite a un usuario salir de un canal.
     * 
     * @param dto Datos de salida del canal
     * @return DTOResponse confirmando la salida
     */
    public DTOResponse salirCanal(DTOSalirCanal dto) {
        // 1. Validar que usuarioId y canalId no estén vacíos
        // 2. Verificar que el usuario es miembro del canal
        // 3. Si el usuario es el creador Y el único admin, no permitir salida
        // 4. Remover usuario del canal
        // 5. Registrar en logs
        // 6. Notificar a miembros del canal
        // 7. Retornar DTOResponse.success
    }
    
    /**
     * Lista todos los canales o los canales de un usuario específico.
     * 
     * @param dto Parámetros de listado
     * @return DTOResponse con lista de canales
     */
    public DTOResponse listarCanales(DTOListarCanales dto) {
        // 1. Si usuarioId está presente, verificar que el usuario existe
        // 2. Si usuarioId está presente, obtener canales del usuario
        // 3. Si usuarioId NO está presente, obtener todos los canales activos
        // 4. Para cada canal, obtener la cantidad de miembros
        // 5. Convertir cada CanalEntity a DTOCanal
        // 6. Retornar DTOResponse.success con lista de DTOCanal
    }
    
    /**
     * Lista los miembros de un canal específico.
     * 
     * @param canalId ID del canal
     * @param solicitanteId ID del usuario que solicita la información
     * @return DTOResponse con lista de miembros
     */
    public DTOResponse listarMiembros(String canalId, String solicitanteId) {
        // 1. Validar que canalId y solicitanteId no estén vacíos
        // 2. Verificar que el canal existe
        // 3. Verificar que el solicitante es miembro del canal
        // 4. Obtener lista de miembros del canal
        // 5. Para cada miembro, obtener información del usuario
        // 6. Crear DTOMiembroCanal con usuario y rol
        // 7. Retornar DTOResponse.success con lista de DTOMiembroCanal
    }
    
    /**
     * Gestiona miembros de un canal (agregar, remover, cambiar rol).
     * Solo puede ser ejecutado por un administrador del canal.
     * 
     * @param dto Datos de gestión de miembro
     * @return DTOResponse confirmando la acción
     */
    public DTOResponse gestionarMiembro(DTOGestionarMiembro dto) {
        // 1. Validar que todos los campos requeridos estén presentes
        // 2. Verificar que el admin existe y es administrador del canal
        // 3. Verificar que el canal existe
        // 4. Verificar que el usuario objetivo existe
        // 5. Según la acción:
        //    - AGREGAR: Agregar usuario como MEMBER si no es miembro
        //    - REMOVER: Remover usuario del canal (no permitir si es creador)
        //    - CAMBIAR_ROL: Actualizar rol del usuario
        // 6. Registrar en logs
        // 7. Notificar al usuario afectado
        // 8. Notificar a todos los miembros del canal
        // 9. Retornar DTOResponse.success
    }
}
```

---

### 7. Actualizar ActionDispatcher

**Ubicación:** `LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/ActionDispatcherImpl.java`

**Nuevas Acciones a Enrutar:**

```java
@Override
public DTOResponse dispatch(DTORequest request, ChannelHandlerContext ctx) {
    String ipAddress = extractIpAddress(ctx);
    String action = request.getAction();
    
    try {
        // Validar acción
        if (action == null || action.isEmpty()) {
            throw new ValidationException("La acción no puede estar vacía", "action");
        }

        // Registrar la acción
        loggerService.logInfo(action, "Acción recibida desde IP: " + ipAddress);

        // Enrutamiento de acciones
        return switch (action) {
            case "ping" -> handlePing();
            
            // --- ACCIONES DE USUARIOS ---
            case "registro" -> handleRegistro(request, ipAddress);
            case "login" -> handleLogin(request, ctx, ipAddress);
            case "logout" -> handleLogout(request, ipAddress);
            case "actualizarPerfil" -> handleActualizarPerfil(request);
            case "cambiarEstado" -> handleCambiarEstado(request);
            
            // --- NUEVAS ACCIONES DE CANALES ---
            case "crearCanal" -> handleCrearCanal(request);
            case "unirseCanal" -> handleUnirseCanal(request);
            case "salirCanal" -> handleSalirCanal(request);
            case "listarCanales" -> handleListarCanales(request);
            case "listarMiembros" -> handleListarMiembros(request);
            case "gestionarMiembro" -> handleGestionarMiembro(request);
            
            default -> DTOResponse.error(action, "Acción no reconocida: " + action);
        };
        
    } catch (Exception e) {
        return exceptionHandler.handleException(e, action, null, ipAddress);
    }
}

// --- MÉTODOS PRIVADOS PARA CADA ACCIÓN DE CANALES ---

private DTOResponse handleCrearCanal(DTORequest request) {
    DTOCrearCanal dto = gson.fromJson(gson.toJson(request.getPayload()), DTOCrearCanal.class);
    return canalService.crearCanal(dto);
}

private DTOResponse handleUnirseCanal(DTORequest request) {
    DTOUnirseCanal dto = gson.fromJson(gson.toJson(request.getPayload()), DTOUnirseCanal.class);
    return canalService.unirseCanal(dto);
}

private DTOResponse handleSalirCanal(DTORequest request) {
    DTOSalirCanal dto = gson.fromJson(gson.toJson(request.getPayload()), DTOSalirCanal.class);
    return canalService.salirCanal(dto);
}

private DTOResponse handleListarCanales(DTORequest request) {
    DTOListarCanales dto = gson.fromJson(gson.toJson(request.getPayload()), DTOListarCanales.class);
    return canalService.listarCanales(dto);
}

private DTOResponse handleListarMiembros(DTORequest request) {
    @SuppressWarnings("unchecked")
    Map<String, String> payload = (Map<String, String>) request.getPayload();
    String canalId = payload.get("canalId");
    String solicitanteId = payload.get("solicitanteId");
    return canalService.listarMiembros(canalId, solicitanteId);
}

private DTOResponse handleGestionarMiembro(DTORequest request) {
    DTOGestionarMiembro dto = gson.fromJson(gson.toJson(request.getPayload()), DTOGestionarMiembro.class);
    return canalService.gestionarMiembro(dto);
}
```

---

## Orden de Implementación

### Fase 1: DTOs y Validadores (Base)
1. ✅ Crear DTOCrearCanal, DTOCanal, DTOUnirseCanal, DTOSalirCanal, DTOListarCanales, DTOGestionarMiembro, DTOMiembroCanal
2. ✅ Crear NombreCanalValidator
3. ✅ Crear DescripcionCanalValidator
4. ✅ Crear CrearCanalValidator

### Fase 2: Entidades y Repositorios
5. ✅ Crear enum RolCanal
6. ✅ Crear CanalEntity con método toDTO()
7. ✅ Crear CanalMiembroEntity
8. ✅ Crear CanalMapper
9. ✅ Crear CanalMiembroMapper
10. ✅ Crear ICanalRepository
11. ✅ Crear ICanalMiembroRepository
12. ✅ Implementar CanalRepositoryImpl con JDBC
13. ✅ Implementar CanalMiembroRepositoryImpl con JDBC

### Fase 3: Lógica de Negocio
14. ✅ Implementar CanalService completo
15. ✅ Actualizar ActionDispatcher con las 6 nuevas acciones

### Fase 4: Integración y Pruebas
16. ✅ Compilar y verificar que no hay errores
17. ✅ Probar flujo de creación de canal
18. ✅ Probar flujo de unirse a canal
19. ✅ Probar flujo de listar canales
20. ✅ Probar flujo de listar miembros
21. ✅ Probar gestión de miembros (agregar, remover, cambiar rol)
22. ✅ Verificar logs en base de datos
23. ✅ Verificar notificaciones en tiempo real a miembros del canal

---

## Criterios de Aceptación

✅ **Épica 3 estará completa cuando:**

1. Un usuario puede crear un canal y es automáticamente asignado como administrador
2. Los usuarios pueden unirse a canales existentes con rol MEMBER
3. Los usuarios pueden salir de canales (excepto el creador si es el único admin)
4. Se puede listar todos los canales activos con paginación
5. Se puede listar los canales de un usuario específico
6. Se puede listar los miembros de un canal con sus roles
7. Un administrador puede agregar usuarios a un canal
8. Un administrador puede remover usuarios del canal (excepto el creador)
9. Un administrador puede cambiar el rol de un miembro (ADMIN/MEMBER)
10. Los nombres de canal son únicos y validados
11. Todas las operaciones se registran en `logs_sistema`
12. Los miembros del canal reciben notificaciones en tiempo real cuando:
    - Se une un nuevo miembro
    - Sale un miembro
    - Se agrega/remueve un miembro
    - Cambia el rol de un miembro
13. No hay errores de compilación (`mvn clean install`)
14. Los datos en la BD son correctos (canales, miembros, roles)

---

## Verificación Final

### Prueba 1: Crear Canal
```bash
telnet localhost 8080

{"action":"crearCanal","payload":{"creadorId":"uuid-usuario","nombre":"Canal General","descripcion":"Canal principal del servidor"}}

# Esperado:
{
  "action":"crearCanal",
  "status":"success",
  "message":"Canal creado exitosamente",
  "data":{
    "id":"uuid-canal",
    "nombre":"Canal General",
    "descripcion":"Canal principal del servidor",
    "creadorId":"uuid-usuario",
    "fechaCreacion":"2025-10-14T...",
    "activo":true,
    "cantidadMiembros":1
  }
}
```

### Prueba 2: Unirse a Canal
```bash
{"action":"unirseCanal","payload":{"usuarioId":"uuid-usuario-2","canalId":"uuid-canal"}}

# Esperado:
{
  "action":"unirseCanal",
  "status":"success",
  "message":"Te has unido al canal exitosamente",
  "data":null
}
```

### Prueba 3: Listar Canales del Usuario
```bash
{"action":"listarCanales","payload":{"usuarioId":"uuid-usuario","limit":10,"offset":0}}

# Esperado: Lista de canales a los que pertenece el usuario
```

### Prueba 4: Listar Miembros de un Canal
```bash
{"action":"listarMiembros","payload":{"canalId":"uuid-canal","solicitanteId":"uuid-usuario"}}

# Esperado:
{
  "action":"listarMiembros",
  "status":"success",
  "message":"Miembros listados exitosamente",
  "data":[
    {
      "usuarioId":"uuid-usuario",
      "nombreUsuario":"Usuario Admin",
      "rol":"ADMIN",
      "fechaUnion":"2025-10-14T..."
    },
    {
      "usuarioId":"uuid-usuario-2",
      "nombreUsuario":"Usuario Member",
      "rol":"MEMBER",
      "fechaUnion":"2025-10-14T..."
    }
  ]
}
```

### Prueba 5: Gestionar Miembro (Cambiar Rol)
```bash
{"action":"gestionarMiembro","payload":{"adminId":"uuid-usuario","canalId":"uuid-canal","usuarioId":"uuid-usuario-2","accion":"CAMBIAR_ROL","nuevoRol":"ADMIN"}}

# Esperado: Rol cambiado exitosamente
```

### Prueba 6: Verificar BD
```sql
-- Verificar que el canal fue creado
SELECT id, nombre, descripcion, creador_id, activo FROM canales;

-- Verificar miembros del canal
SELECT cm.canal_id, cm.usuario_id, u.nombre, cm.rol, cm.fecha_union
FROM canal_miembros cm
INNER JOIN usuarios u ON cm.usuario_id = u.id
ORDER BY cm.fecha_union;

-- Verificar logs de canales
SELECT * FROM logs_sistema 
WHERE accion IN ('crearCanal', 'unirseCanal', 'gestionarMiembro') 
ORDER BY timestamp DESC;
```

---

## Dependencias Nuevas Requeridas

No se requieren dependencias adicionales. Todas las dependencias necesarias ya están declaradas en el `pom.xml` padre (Spring Boot, JDBC, HikariCP, Gson, etc.).

---

## Estimación de Tiempo

- **Fase 1 (DTOs y Validadores):** ~25-30 minutos
- **Fase 2 (Entidades y Repositorios):** ~45-55 minutos
- **Fase 3 (Lógica de Negocio):** ~50-60 minutos
- **Fase 4 (Integración y Pruebas):** ~25-30 minutos

**Total Estimado:** 145-175 minutos (2.4 - 2.9 horas)

---

## Notas Importantes

🔐 **Seguridad:**
- Solo administradores pueden agregar/remover miembros
- El creador del canal siempre es administrador
- No se puede remover al creador del canal
- Validar que el usuario sea miembro antes de permitir acciones

⚡ **Performance:**
- Usar índices en `canal_miembros` (canal_id, usuario_id)
- Limitar resultados con paginación en listados
- Cachear cantidad de miembros si es necesario

📊 **Notificaciones en Tiempo Real:**
- Usar `ConnectionManager.notifyChannel()` para notificar a todos los miembros
- Enviar notificaciones cuando:
  - Nuevo miembro se une
  - Miembro sale del canal
  - Administrador agrega/remueve miembro
  - Cambia rol de miembro

🎯 **Reglas de Negocio:**
- Un canal debe tener al menos un administrador
- El creador no puede salir del canal si es el único administrador
- Los nombres de canal son únicos en todo el sistema
- Un usuario no puede unirse dos veces al mismo canal

---

¿Estás listo para comenzar la implementación de la Épica 3?

