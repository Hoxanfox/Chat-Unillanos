# Épica 2: Gestión de Usuarios - Plan Detallado

## Objetivo General

Implementar el sistema completo de gestión de usuarios: registro, autenticación, actualización de perfil y gestión de estado (online/offline). Los usuarios podrán crear cuentas, iniciar sesión de forma segura y actualizar su información personal. El sistema registrará todas las actividades en logs y gestionará las conexiones activas.

## Contexto Actual

✅ **Ya Implementado (Épica 1):**
- ✅ Infraestructura de excepciones personalizadas
- ✅ GlobalExceptionHandler para manejo centralizado de errores
- ✅ LoggerService con persistencia asíncrona en BD
- ✅ LogRepository para auditoría
- ✅ ConnectionManager para gestión de conexiones en tiempo real
- ✅ ActionDispatcher con enrutamiento básico
- ✅ Servidor Netty funcional con hilos virtuales
- ✅ DTORequest y DTOResponse base

⚠️ **Pendiente de Implementar:**
- ❌ DTOs específicos para usuarios (registro, login, actualización)
- ❌ Validadores de email, contraseña y datos de usuario
- ❌ UsuarioRepositoryImpl completo con todas las operaciones JDBC
- ❌ AutenticacionService con lógica de negocio
- ❌ Integración con ConnectionManager para estado online/offline
- ❌ Acciones en ActionDispatcher: registro, login, logout, actualizar perfil

## Componentes a Implementar

### 1. DTOs para Gestión de Usuarios

**Ubicación:** `Infraestructura/DTOs/src/main/java/com/unillanos/server/dto/`

#### DTORegistro.java
```java
public class DTORegistro {
    private String nombre;           // Requerido, 3-50 caracteres
    private String email;            // Requerido, formato válido, único
    private String password;         // Requerido, mínimo 8 caracteres
    private String photoId;          // Opcional, referencia a archivo
}
```

#### DTOLogin.java
```java
public class DTOLogin {
    private String email;            // Requerido
    private String password;         // Requerido
}
```

#### DTOUsuario.java (Response)
```java
public class DTOUsuario {
    private String id;               // UUID
    private String nombre;
    private String email;
    private String photoId;
    private String estado;           // ONLINE, OFFLINE, AWAY
    private String fechaRegistro;    // ISO-8601
}
```

#### DTOActualizarPerfil.java
```java
public class DTOActualizarPerfil {
    private String userId;           // Requerido (del usuario autenticado)
    private String nombre;           // Opcional
    private String photoId;          // Opcional
    private String passwordActual;   // Requerido si se cambia password
    private String passwordNueva;    // Opcional
}
```

#### DTOCambiarEstado.java
```java
public class DTOCambiarEstado {
    private String userId;           // Requerido
    private String nuevoEstado;      // ONLINE, OFFLINE, AWAY
}
```

---

### 2. Validadores de Usuario

**Ubicación:** `LogicaNegocio/Validadores/src/main/java/com/unillanos/server/validation/`

#### EmailValidator.java (Actualizar existente)
```java
public class EmailValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    public static void validate(String email) throws ValidationException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("El email es requerido", "email");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Formato de email inválido", "email");
        }
        if (email.length() > 100) {
            throw new ValidationException("El email es demasiado largo (máx 100)", "email");
        }
    }
}
```

#### PasswordValidator.java (NUEVO)
```java
public class PasswordValidator {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 100;
    
    public static void validate(String password) throws ValidationException {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("La contraseña es requerida", "password");
        }
        if (password.length() < MIN_LENGTH) {
            throw new ValidationException(
                "La contraseña debe tener al menos " + MIN_LENGTH + " caracteres", 
                "password"
            );
        }
        if (password.length() > MAX_LENGTH) {
            throw new ValidationException(
                "La contraseña es demasiado larga (máx " + MAX_LENGTH + ")", 
                "password"
            );
        }
        // Validar que contenga al menos una letra y un número
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new ValidationException(
                "La contraseña debe contener al menos una letra y un número", 
                "password"
            );
        }
    }
}
```

#### NombreValidator.java (NUEVO)
```java
public class NombreValidator {
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;
    
    public static void validate(String nombre) throws ValidationException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ValidationException("El nombre es requerido", "nombre");
        }
        
        String nombreTrim = nombre.trim();
        if (nombreTrim.length() < MIN_LENGTH) {
            throw new ValidationException(
                "El nombre debe tener al menos " + MIN_LENGTH + " caracteres", 
                "nombre"
            );
        }
        if (nombreTrim.length() > MAX_LENGTH) {
            throw new ValidationException(
                "El nombre es demasiado largo (máx " + MAX_LENGTH + ")", 
                "nombre"
            );
        }
    }
}
```

#### RegistroValidator.java (NUEVO - Validador compuesto)
```java
public class RegistroValidator {
    public static void validate(DTORegistro dto) throws ValidationException {
        // Validar nombre
        NombreValidator.validate(dto.getNombre());
        
        // Validar email
        EmailValidator.validate(dto.getEmail());
        
        // Validar contraseña
        PasswordValidator.validate(dto.getPassword());
        
        // photoId es opcional, no se valida si es null
    }
}
```

---

### 3. Completar UsuarioRepositoryImpl

**Ubicación:** `Datos/Repositorios/src/main/java/com/unillanos/server/repository/impl/UsuarioRepositoryImpl.java`

**Métodos a Implementar:**

```java
@Repository
public class UsuarioRepositoryImpl implements IUsuarioRepository {
    
    private final DataSource dataSource;
    
    // --- MÉTODOS DE CONSULTA ---
    
    @Override
    public Optional<UsuarioEntity> findById(String id) {
        // SELECT * FROM usuarios WHERE id = ?
    }
    
    @Override
    public Optional<UsuarioEntity> findByEmail(String email) {
        // SELECT * FROM usuarios WHERE email = ?
    }
    
    @Override
    public boolean existsByEmail(String email) {
        // SELECT COUNT(*) FROM usuarios WHERE email = ?
    }
    
    @Override
    public List<UsuarioEntity> findAll(int limit, int offset) {
        // SELECT * FROM usuarios ORDER BY fecha_registro DESC LIMIT ? OFFSET ?
    }
    
    // --- MÉTODOS DE ESCRITURA ---
    
    @Override
    public UsuarioEntity save(UsuarioEntity usuario) {
        // INSERT INTO usuarios (id, nombre, email, password_hash, photo_id, ip_address, fecha_registro, estado)
        // VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        // Retorna el usuario con el ID generado
    }
    
    @Override
    public void update(UsuarioEntity usuario) {
        // UPDATE usuarios SET nombre = ?, photo_id = ?, password_hash = ? WHERE id = ?
    }
    
    @Override
    public void updateEstado(String id, String estado) {
        // UPDATE usuarios SET estado = ? WHERE id = ?
    }
    
    @Override
    public void updateIpAddress(String id, String ipAddress) {
        // UPDATE usuarios SET ip_address = ? WHERE id = ?
    }
    
    @Override
    public void delete(String id) {
        // DELETE FROM usuarios WHERE id = ?
        // (Probablemente no se use, pero se incluye por completitud)
    }
}
```

**Consideraciones:**
- Usar `PreparedStatement` en TODAS las operaciones
- Usar `UsuarioMapper.mapRow(ResultSet)` para convertir filas a entidades
- Cerrar recursos en bloques `finally`
- Lanzar `RepositoryException` en caso de error SQL
- El ID se genera con `UUID.randomUUID().toString()` antes de insertar
- `password_hash` se almacena hasheado con BCrypt (el hash se hace en el servicio)

---

### 4. AutenticacionService

**Ubicación:** `LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/AutenticacionService.java`

**Responsabilidades:**
- Registrar nuevos usuarios
- Autenticar usuarios (login)
- Actualizar perfiles
- Gestionar estado online/offline
- Integrar con ConnectionManager y LoggerService

**Métodos Principales:**

```java
@Service
public class AutenticacionService {
    
    private final IUsuarioRepository usuarioRepository;
    private final LoggerService loggerService;
    private final ConnectionManager connectionManager;
    
    /**
     * Registra un nuevo usuario en el sistema.
     * 
     * @param dto Datos del usuario a registrar
     * @param ipAddress IP del cliente
     * @return DTOResponse con el usuario creado
     */
    public DTOResponse registrarUsuario(DTORegistro dto, String ipAddress) {
        // 1. Validar datos con RegistroValidator
        // 2. Verificar que el email no exista (usuarioRepository.existsByEmail)
        // 3. Hashear la contraseña con BCrypt
        // 4. Crear UsuarioEntity con ID UUID y estado OFFLINE
        // 5. Guardar en BD (usuarioRepository.save)
        // 6. Registrar en logs (loggerService.logInfo)
        // 7. Retornar DTOResponse.success con DTOUsuario (SIN password_hash)
    }
    
    /**
     * Autentica un usuario (login).
     * 
     * @param dto Credenciales de login
     * @param ctx Contexto de Netty para registrar conexión
     * @param ipAddress IP del cliente
     * @return DTOResponse con el usuario autenticado
     */
    public DTOResponse autenticarUsuario(DTOLogin dto, ChannelHandlerContext ctx, String ipAddress) {
        // 1. Validar que email y password no estén vacíos
        // 2. Buscar usuario por email (usuarioRepository.findByEmail)
        // 3. Verificar que el usuario existe
        // 4. Verificar contraseña con BCrypt.verifyer().verify()
        // 5. Actualizar estado a ONLINE (usuarioRepository.updateEstado)
        // 6. Actualizar IP del usuario (usuarioRepository.updateIpAddress)
        // 7. Registrar conexión en ConnectionManager
        // 8. Registrar login en logs (loggerService.logLogin)
        // 9. Retornar DTOResponse.success con DTOUsuario
    }
    
    /**
     * Cierra la sesión de un usuario (logout).
     * 
     * @param userId ID del usuario
     * @param ipAddress IP del cliente
     * @return DTOResponse confirmando el logout
     */
    public DTOResponse logout(String userId, String ipAddress) {
        // 1. Verificar que el usuario existe
        // 2. Actualizar estado a OFFLINE
        // 3. Eliminar del ConnectionManager
        // 4. Registrar logout en logs
        // 5. Retornar DTOResponse.success
    }
    
    /**
     * Actualiza el perfil de un usuario.
     * 
     * @param dto Datos a actualizar
     * @return DTOResponse con el usuario actualizado
     */
    public DTOResponse actualizarPerfil(DTOActualizarPerfil dto) {
        // 1. Validar que userId no esté vacío
        // 2. Buscar usuario por ID
        // 3. Si se cambia el nombre, validar con NombreValidator
        // 4. Si se cambia la contraseña:
        //    - Validar passwordActual con BCrypt
        //    - Validar passwordNueva con PasswordValidator
        //    - Hashear nueva contraseña
        // 5. Actualizar entidad con los nuevos valores
        // 6. Guardar en BD (usuarioRepository.update)
        // 7. Registrar en logs
        // 8. Retornar DTOResponse.success con DTOUsuario actualizado
    }
    
    /**
     * Cambia el estado de un usuario (ONLINE, OFFLINE, AWAY).
     * 
     * @param dto Datos del cambio de estado
     * @return DTOResponse confirmando el cambio
     */
    public DTOResponse cambiarEstado(DTOCambiarEstado dto) {
        // 1. Validar que el estado sea válido (ONLINE, OFFLINE, AWAY)
        // 2. Actualizar estado en BD
        // 3. Si cambia a OFFLINE, eliminar del ConnectionManager
        // 4. Registrar en logs
        // 5. Retornar DTOResponse.success
    }
}
```

**Clase de Utilidad para BCrypt:**

```java
public class PasswordHasher {
    private static final BCrypt.Hasher hasher = BCrypt.withDefaults();
    private static final BCrypt.Verifyer verifyer = BCrypt.verifyer();
    
    public static String hash(String plainPassword) {
        return hasher.hashToString(12, plainPassword.toCharArray());
    }
    
    public static boolean verify(String plainPassword, String hashedPassword) {
        return verifyer.verify(plainPassword.toCharArray(), hashedPassword).verified;
    }
}
```

---

### 5. Actualizar ActionDispatcher

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
            
            // --- NUEVAS ACCIONES DE USUARIOS ---
            case "registro" -> handleRegistro(request, ipAddress);
            case "login" -> handleLogin(request, ctx, ipAddress);
            case "logout" -> handleLogout(request, ipAddress);
            case "actualizarPerfil" -> handleActualizarPerfil(request);
            case "cambiarEstado" -> handleCambiarEstado(request);
            
            default -> DTOResponse.error(action, "Acción no reconocida: " + action);
        };
        
    } catch (Exception e) {
        return exceptionHandler.handleException(e, action, null, ipAddress);
    }
}

// --- MÉTODOS PRIVADOS PARA CADA ACCIÓN ---

private DTOResponse handleRegistro(DTORequest request, String ipAddress) {
    DTORegistro dto = gson.fromJson(gson.toJson(request.getPayload()), DTORegistro.class);
    return autenticacionService.registrarUsuario(dto, ipAddress);
}

private DTOResponse handleLogin(DTORequest request, ChannelHandlerContext ctx, String ipAddress) {
    DTOLogin dto = gson.fromJson(gson.toJson(request.getPayload()), DTOLogin.class);
    return autenticacionService.autenticarUsuario(dto, ctx, ipAddress);
}

private DTOResponse handleLogout(DTORequest request, String ipAddress) {
    // El userId puede venir en el payload o extraerse del contexto
    String userId = extractUserIdFromPayload(request);
    return autenticacionService.logout(userId, ipAddress);
}

private DTOResponse handleActualizarPerfil(DTORequest request) {
    DTOActualizarPerfil dto = gson.fromJson(gson.toJson(request.getPayload()), DTOActualizarPerfil.class);
    return autenticacionService.actualizarPerfil(dto);
}

private DTOResponse handleCambiarEstado(DTORequest request) {
    DTOCambiarEstado dto = gson.fromJson(gson.toJson(request.getPayload()), DTOCambiarEstado.class);
    return autenticacionService.cambiarEstado(dto);
}
```

---

### 6. Enum EstadoUsuario

**Ubicación:** `Datos/Repositorios/src/main/java/com/unillanos/server/repository/models/EstadoUsuario.java`

```java
public enum EstadoUsuario {
    ONLINE,
    OFFLINE,
    AWAY;
    
    public static EstadoUsuario fromString(String estado) {
        try {
            return EstadoUsuario.valueOf(estado.toUpperCase());
        } catch (Exception e) {
            return OFFLINE; // Por defecto
        }
    }
}
```

---

### 7. Actualizar UsuarioEntity

**Ubicación:** `Datos/Repositorios/src/main/java/com/unillanos/server/repository/models/UsuarioEntity.java`

**Agregar:**
- Usar el enum `EstadoUsuario` en lugar de String para el campo `estado`
- Agregar método `toDTO()` para convertir a DTOUsuario

```java
public class UsuarioEntity {
    private String id;
    private String nombre;
    private String email;
    private String passwordHash;
    private String photoId;
    private String ipAddress;
    private LocalDateTime fechaRegistro;
    private EstadoUsuario estado;
    
    // ... getters y setters ...
    
    /**
     * Convierte la entidad a DTO (sin exponer el password_hash).
     */
    public DTOUsuario toDTO() {
        DTOUsuario dto = new DTOUsuario();
        dto.setId(this.id);
        dto.setNombre(this.nombre);
        dto.setEmail(this.email);
        dto.setPhotoId(this.photoId);
        dto.setEstado(this.estado.name());
        dto.setFechaRegistro(this.fechaRegistro.toString());
        return dto;
    }
}
```

---

## Orden de Implementación

### Fase 1: DTOs y Validadores (Base)
1. ✅ Crear DTORegistro, DTOLogin, DTOUsuario, DTOActualizarPerfil, DTOCambiarEstado
2. ✅ Actualizar EmailValidator
3. ✅ Crear PasswordValidator
4. ✅ Crear NombreValidator
5. ✅ Crear RegistroValidator

### Fase 2: Entidades y Repositorio
6. ✅ Crear enum EstadoUsuario
7. ✅ Actualizar UsuarioEntity (agregar toDTO(), usar enum)
8. ✅ Actualizar UsuarioMapper (mapear estado como enum)
9. ✅ Implementar todos los métodos de UsuarioRepositoryImpl con JDBC

### Fase 3: Lógica de Negocio
10. ✅ Crear clase PasswordHasher (BCrypt utility)
11. ✅ Implementar AutenticacionService completo
12. ✅ Actualizar ActionDispatcher con las 5 nuevas acciones

### Fase 4: Integración y Pruebas
13. ✅ Compilar y verificar que no hay errores
14. ✅ Probar flujo de registro (crear usuario en BD)
15. ✅ Probar flujo de login (verificar hash, registrar en ConnectionManager)
16. ✅ Probar logout (desconectar de ConnectionManager)
17. ✅ Probar actualización de perfil
18. ✅ Verificar logs en base de datos

---

## Criterios de Aceptación

✅ **Épica 2 estará completa cuando:**

1. Un cliente puede registrarse enviando `{"action":"registro","payload":{...}}` y recibir su usuario creado
2. Un usuario puede autenticarse con email/password y su estado cambia a ONLINE
3. El ConnectionManager registra correctamente las conexiones al hacer login
4. Un usuario puede actualizar su nombre, foto de perfil y/o contraseña
5. Un usuario puede cambiar su estado entre ONLINE, OFFLINE, AWAY
6. El logout desconecta al usuario del ConnectionManager y cambia su estado a OFFLINE
7. Todos los eventos se registran en `logs_sistema` con los tipos apropiados
8. Las contraseñas se almacenan hasheadas con BCrypt (factor 12)
9. Los emails duplicados se rechazan con error descriptivo
10. Las validaciones de contraseña (mínimo 8, letra+número) funcionan correctamente
11. No hay errores de compilación (`mvn clean install`)
12. Los datos en la BD son correctos (nombres, emails, hashes)

---

## Verificación Final

### Prueba 1: Registro de Usuario
```bash
# Conectar al servidor
telnet localhost 8080

# Enviar petición de registro
{"action":"registro","payload":{"nombre":"Juan Pérez","email":"juan@test.com","password":"Password123"}}

# Esperado:
{
  "action":"registro",
  "status":"success",
  "message":"Usuario registrado exitosamente",
  "data":{
    "id":"...",
    "nombre":"Juan Pérez",
    "email":"juan@test.com",
    "photoId":null,
    "estado":"OFFLINE",
    "fechaRegistro":"2025-10-14T..."
  }
}
```

### Prueba 2: Login
```bash
{"action":"login","payload":{"email":"juan@test.com","password":"Password123"}}

# Esperado:
{
  "action":"login",
  "status":"success",
  "message":"Autenticación exitosa",
  "data":{
    "id":"...",
    "nombre":"Juan Pérez",
    "email":"juan@test.com",
    "photoId":null,
    "estado":"ONLINE",
    "fechaRegistro":"2025-10-14T..."
  }
}
```

### Prueba 3: Verificar BD
```sql
-- Verificar que el usuario fue creado
SELECT id, nombre, email, estado, fecha_registro FROM usuarios;

-- Verificar que el password está hasheado (debe empezar con $2a$ o $2b$)
SELECT password_hash FROM usuarios WHERE email = 'juan@test.com';

-- Verificar logs de registro y login
SELECT * FROM logs_sistema WHERE accion IN ('registro', 'login') ORDER BY timestamp DESC;
```

### Prueba 4: Actualizar Perfil
```bash
{"action":"actualizarPerfil","payload":{"userId":"...","nombre":"Juan Carlos Pérez"}}

# Esperado: Usuario con nombre actualizado
```

### Prueba 5: Logout
```bash
{"action":"logout","payload":{"userId":"..."}}

# Esperado: Estado cambia a OFFLINE, desconexión del ConnectionManager
```

---

## Dependencias Nuevas Requeridas

### pom.xml (Padre o Servicios)
```xml
<!-- BCrypt para hash de contraseñas -->
<dependency>
    <groupId>at.favre.lib</groupId>
    <artifactId>bcrypt</artifactId>
    <version>0.10.2</version>
</dependency>
```

---

## Estimación de Tiempo

- **Fase 1 (DTOs y Validadores):** ~20-25 minutos
- **Fase 2 (Entidades y Repositorio):** ~30-35 minutos
- **Fase 3 (Lógica de Negocio):** ~40-45 minutos
- **Fase 4 (Integración y Pruebas):** ~20-25 minutos

**Total Estimado:** 110-130 minutos (1.8 - 2.2 horas)

---

## Notas Importantes

🔒 **Seguridad:**
- NUNCA retornar `password_hash` en ningún DTOResponse
- Usar BCrypt con factor de trabajo 12 (balance seguridad/performance)
- Validar contraseñas tanto en formato como en fortaleza
- Registrar todos los intentos de login (exitosos y fallidos) en logs

⚡ **Performance:**
- BCrypt es intencionalmente lento (es una feature de seguridad)
- Las operaciones de hash NO deben bloquearse, pero son síncronas
- El login puede tardar ~100-200ms debido al hash, esto es normal

📊 **Datos de Ejemplo:**
- Usuario: `admin@unillanos.com` / `Admin123`
- Usuario: `test@test.com` / `Test1234`

---

¿Estás listo para comenzar la implementación de la Épica 2?

