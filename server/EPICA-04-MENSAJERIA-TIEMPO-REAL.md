# Épica 4: Mensajería en Tiempo Real - Plan Detallado

## Objetivo General

Implementar el sistema completo de mensajería en tiempo real: envío de mensajes directos (usuario a usuario) y mensajes de canal (usuario a grupo), con soporte para archivos adjuntos, historial de mensajes y notificaciones instantáneas. Los usuarios podrán comunicarse de forma efectiva tanto en conversaciones privadas como en canales grupales.

## Contexto Actual

✅ **Ya Implementado (Épicas 1, 2 y 3):**
- ✅ Infraestructura de excepciones personalizadas
- ✅ GlobalExceptionHandler para manejo centralizado de errores
- ✅ LoggerService con persistencia asíncrona en BD
- ✅ LogRepository para auditoría
- ✅ ConnectionManager para gestión de conexiones en tiempo real
- ✅ ActionDispatcher con enrutamiento de acciones
- ✅ Servidor Netty funcional con hilos virtuales
- ✅ DTORequest y DTOResponse base
- ✅ Sistema completo de gestión de usuarios (registro, login, perfil, estados)
- ✅ Sistema completo de gestión de canales (crear, unirse, listar, gestionar miembros)
- ✅ UsuarioRepository y CanalRepository con JDBC

⚠️ **Pendiente de Implementar:**
- ❌ DTOs específicos para mensajería
- ❌ Validadores de mensajes (contenido, archivos)
- ❌ Entidades de mensajes (MensajeEntity, TipoMensaje enum)
- ❌ MensajeRepository con operaciones JDBC
- ❌ MensajeriaService con lógica de negocio
- ❌ Acciones en ActionDispatcher: enviar mensaje directo, enviar mensaje canal, obtener historial
- ❌ Sistema de archivos adjuntos (opcional para esta épica, puede ser Épica 5)

---

## Componentes a Implementar

### 1. DTOs para Mensajería

**Ubicación:** `Infraestructura/DTOs/src/main/java/com/unillanos/server/dto/`

#### DTOEnviarMensaje.java
```java
public class DTOEnviarMensaje {
    private String remitenteId;         // Requerido - Usuario que envía
    private String destinatarioId;      // Opcional - Si es mensaje directo
    private String canalId;             // Opcional - Si es mensaje de canal
    private String contenido;           // Requerido, 1-2000 caracteres
    private String fileId;              // Opcional - ID del archivo adjunto
}
```

**Validaciones:**
- Exactamente uno de `destinatarioId` o `canalId` debe estar presente
- `contenido` no puede estar vacío
- Si es mensaje de canal, el remitente debe ser miembro del canal
- Si es mensaje directo, el remitente y destinatario deben existir

#### DTOMensaje.java (Response)
```java
public class DTOMensaje {
    private Long id;                    // ID del mensaje
    private String remitenteId;
    private String remitenteNombre;
    private String destinatarioId;      // null si es mensaje de canal
    private String destinatarioNombre;  // null si es mensaje de canal
    private String canalId;             // null si es mensaje directo
    private String canalNombre;         // null si es mensaje directo
    private String tipo;                // "DIRECT" o "CHANNEL"
    private String contenido;
    private String fileId;              // null si no tiene archivo
    private String fileName;            // null si no tiene archivo
    private String fechaEnvio;          // ISO-8601
}
```

#### DTOHistorial.java
```java
public class DTOHistorial {
    private String usuarioId;           // Requerido - Usuario que solicita
    private String destinatarioId;      // Opcional - Para historial de mensajes directos
    private String canalId;             // Opcional - Para historial de canal
    private int limit;                  // Opcional - Por defecto 50
    private int offset;                 // Opcional - Por defecto 0
}
```

**Validaciones:**
- Exactamente uno de `destinatarioId` o `canalId` debe estar presente
- Si es historial de canal, el usuario debe ser miembro del canal
- `limit` debe estar entre 1 y 100

---

### 2. Validadores de Mensajes

**Ubicación:** `LogicaNegocio/Validadores/src/main/java/com/unillanos/server/validation/`

#### ContenidoMensajeValidator.java (NUEVO)
```java
public class ContenidoMensajeValidator {
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 2000;
    
    public static void validate(String contenido) throws ValidationException {
        if (contenido == null || contenido.trim().isEmpty()) {
            throw new ValidationException("El contenido del mensaje es requerido", "contenido");
        }
        
        String contenidoTrim = contenido.trim();
        
        if (contenidoTrim.length() < MIN_LENGTH) {
            throw new ValidationException(
                "El mensaje no puede estar vacío", 
                "contenido"
            );
        }
        
        if (contenidoTrim.length() > MAX_LENGTH) {
            throw new ValidationException(
                "El mensaje es demasiado largo (máx " + MAX_LENGTH + " caracteres)", 
                "contenido"
            );
        }
    }
}
```

#### EnviarMensajeValidator.java (NUEVO - Validador compuesto)
```java
public class EnviarMensajeValidator {
    public static void validate(DTOEnviarMensaje dto) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("Los datos del mensaje son requeridos", "dto");
        }
        
        // Validar remitenteId
        if (dto.getRemitenteId() == null || dto.getRemitenteId().trim().isEmpty()) {
            throw new ValidationException("El ID del remitente es requerido", "remitenteId");
        }
        
        // Validar que haya EXACTAMENTE un destinatario (directo XOR canal)
        boolean tieneDestinatario = dto.getDestinatarioId() != null && !dto.getDestinatarioId().trim().isEmpty();
        boolean tieneCanal = dto.getCanalId() != null && !dto.getCanalId().trim().isEmpty();
        
        if (!tieneDestinatario && !tieneCanal) {
            throw new ValidationException(
                "Debe especificar un destinatario o un canal", 
                "destinatario"
            );
        }
        
        if (tieneDestinatario && tieneCanal) {
            throw new ValidationException(
                "No puede enviar a un destinatario y a un canal simultáneamente", 
                "destinatario"
            );
        }
        
        // Validar contenido
        ContenidoMensajeValidator.validate(dto.getContenido());
        
        // fileId es opcional, no se valida
    }
}
```

---

### 3. Entidades y Mappers de Mensajes

**Ubicación:** `Datos/Repositorios/src/main/java/com/unillanos/server/repository/models/`

#### TipoMensaje.java (NUEVO - Enum)
```java
public enum TipoMensaje {
    DIRECT,   // Mensaje directo (usuario a usuario)
    CHANNEL;  // Mensaje de canal (usuario a grupo)
    
    public static TipoMensaje fromString(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return DIRECT; // Por defecto
        }
        
        try {
            return TipoMensaje.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DIRECT;
        }
    }
}
```

#### MensajeEntity.java (NUEVO)
```java
public class MensajeEntity {
    private Long id;                    // ID autoincremental
    private String remitenteId;
    private String destinatarioId;      // null si es mensaje de canal
    private String canalId;             // null si es mensaje directo
    private TipoMensaje tipo;           // DIRECT o CHANNEL
    private String contenido;
    private String fileId;              // null si no tiene archivo
    private LocalDateTime fechaEnvio;
    
    // Constructores
    public MensajeEntity() {}
    
    public MensajeEntity(Long id, String remitenteId, String destinatarioId, String canalId,
                         TipoMensaje tipo, String contenido, String fileId, LocalDateTime fechaEnvio) {
        this.id = id;
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.canalId = canalId;
        this.tipo = tipo;
        this.contenido = contenido;
        this.fileId = fileId;
        this.fechaEnvio = fechaEnvio;
    }
    
    // Getters y Setters
    // ...
    
    /**
     * Convierte la entidad a DTO.
     * Requiere información adicional de Usuario y Canal para nombres.
     */
    public DTOMensaje toDTO(String remitenteNombre, String destinatarioNombre, 
                            String canalNombre, String fileName) {
        DTOMensaje dto = new DTOMensaje();
        dto.setId(this.id);
        dto.setRemitenteId(this.remitenteId);
        dto.setRemitenteNombre(remitenteNombre);
        dto.setDestinatarioId(this.destinatarioId);
        dto.setDestinatarioNombre(destinatarioNombre);
        dto.setCanalId(this.canalId);
        dto.setCanalNombre(canalNombre);
        dto.setTipo(this.tipo.name());
        dto.setContenido(this.contenido);
        dto.setFileId(this.fileId);
        dto.setFileName(fileName);
        dto.setFechaEnvio(this.fechaEnvio != null ? this.fechaEnvio.toString() : null);
        return dto;
    }
}
```

#### MensajeMapper.java (NUEVO)
```java
public class MensajeMapper {
    public static MensajeEntity mapRow(ResultSet rs) throws SQLException {
        MensajeEntity mensaje = new MensajeEntity();
        
        mensaje.setId(rs.getLong("id"));
        mensaje.setRemitenteId(rs.getString("remitente_id"));
        mensaje.setDestinatarioId(rs.getString("destinatario_id"));
        mensaje.setCanalId(rs.getString("canal_id"));
        
        // Convertir String a TipoMensaje enum
        String tipoStr = rs.getString("tipo");
        mensaje.setTipo(TipoMensaje.fromString(tipoStr));
        
        mensaje.setContenido(rs.getString("contenido"));
        mensaje.setFileId(rs.getString("file_id"));
        
        // Convertir Timestamp a LocalDateTime
        Timestamp fechaEnvio = rs.getTimestamp("fecha_envio");
        if (fechaEnvio != null) {
            mensaje.setFechaEnvio(fechaEnvio.toLocalDateTime());
        }
        
        return mensaje;
    }
}
```

---

### 4. Interfaz de Repositorio

**Ubicación:** `Datos/Repositorios/src/main/java/com/unillanos/server/repository/interfaces/`

#### IMensajeRepository.java (NUEVO)
```java
public interface IMensajeRepository {
    
    // --- MÉTODOS DE CONSULTA ---
    
    /**
     * Busca un mensaje por su ID.
     *
     * @param id ID del mensaje
     * @return Optional con el mensaje si existe
     */
    Optional<MensajeEntity> findById(Long id);
    
    /**
     * Obtiene el historial de mensajes directos entre dos usuarios.
     *
     * @param usuarioId1 ID del primer usuario
     * @param usuarioId2 ID del segundo usuario
     * @param limit Número máximo de resultados
     * @param offset Desplazamiento inicial
     * @return Lista de mensajes ordenados por fecha descendente
     */
    List<MensajeEntity> findMensajesDirectos(String usuarioId1, String usuarioId2, int limit, int offset);
    
    /**
     * Obtiene el historial de mensajes de un canal.
     *
     * @param canalId ID del canal
     * @param limit Número máximo de resultados
     * @param offset Desplazamiento inicial
     * @return Lista de mensajes ordenados por fecha descendente
     */
    List<MensajeEntity> findMensajesCanal(String canalId, int limit, int offset);
    
    /**
     * Obtiene el último mensaje entre dos usuarios (para previsualización).
     *
     * @param usuarioId1 ID del primer usuario
     * @param usuarioId2 ID del segundo usuario
     * @return Optional con el último mensaje
     */
    Optional<MensajeEntity> findUltimoMensajeDirecto(String usuarioId1, String usuarioId2);
    
    /**
     * Obtiene el último mensaje de un canal (para previsualización).
     *
     * @param canalId ID del canal
     * @return Optional con el último mensaje
     */
    Optional<MensajeEntity> findUltimoMensajeCanal(String canalId);
    
    /**
     * Cuenta el total de mensajes entre dos usuarios.
     *
     * @param usuarioId1 ID del primer usuario
     * @param usuarioId2 ID del segundo usuario
     * @return Cantidad de mensajes
     */
    int countMensajesDirectos(String usuarioId1, String usuarioId2);
    
    /**
     * Cuenta el total de mensajes de un canal.
     *
     * @param canalId ID del canal
     * @return Cantidad de mensajes
     */
    int countMensajesCanal(String canalId);
    
    // --- MÉTODOS DE ESCRITURA ---
    
    /**
     * Guarda un nuevo mensaje.
     *
     * @param mensaje Entidad del mensaje
     * @return Mensaje guardado con ID asignado
     */
    MensajeEntity save(MensajeEntity mensaje);
    
    /**
     * Elimina un mensaje por su ID.
     * (Opcional - puede ser una feature futura)
     *
     * @param id ID del mensaje
     */
    void deleteById(Long id);
}
```

---

### 5. Implementación del Repositorio

**Ubicación:** `Datos/Repositorios/src/main/java/com/unillanos/server/repository/impl/`

#### MensajeRepositoryImpl.java (NUEVO)

**Métodos a Implementar:**

```java
@Repository
public class MensajeRepositoryImpl implements IMensajeRepository {
    
    private final DataSource dataSource;
    
    @Override
    public Optional<MensajeEntity> findById(Long id) {
        // SELECT * FROM mensajes WHERE id = ?
    }
    
    @Override
    public List<MensajeEntity> findMensajesDirectos(String usuarioId1, String usuarioId2, int limit, int offset) {
        // SELECT * FROM mensajes 
        // WHERE tipo = 'DIRECT' 
        //   AND ((remitente_id = ? AND destinatario_id = ?) 
        //     OR (remitente_id = ? AND destinatario_id = ?))
        // ORDER BY fecha_envio DESC 
        // LIMIT ? OFFSET ?
    }
    
    @Override
    public List<MensajeEntity> findMensajesCanal(String canalId, int limit, int offset) {
        // SELECT * FROM mensajes 
        // WHERE tipo = 'CHANNEL' AND canal_id = ?
        // ORDER BY fecha_envio DESC 
        // LIMIT ? OFFSET ?
    }
    
    @Override
    public Optional<MensajeEntity> findUltimoMensajeDirecto(String usuarioId1, String usuarioId2) {
        // Similar a findMensajesDirectos pero con LIMIT 1
    }
    
    @Override
    public Optional<MensajeEntity> findUltimoMensajeCanal(String canalId) {
        // Similar a findMensajesCanal pero con LIMIT 1
    }
    
    @Override
    public int countMensajesDirectos(String usuarioId1, String usuarioId2) {
        // SELECT COUNT(*) FROM mensajes 
        // WHERE tipo = 'DIRECT' 
        //   AND ((remitente_id = ? AND destinatario_id = ?) 
        //     OR (remitente_id = ? AND destinatario_id = ?))
    }
    
    @Override
    public int countMensajesCanal(String canalId) {
        // SELECT COUNT(*) FROM mensajes WHERE tipo = 'CHANNEL' AND canal_id = ?
    }
    
    @Override
    public MensajeEntity save(MensajeEntity mensaje) {
        // INSERT INTO mensajes (remitente_id, destinatario_id, canal_id, tipo, contenido, file_id, fecha_envio)
        // VALUES (?, ?, ?, ?, ?, ?, ?)
        // RETURN_GENERATED_KEYS para obtener el ID asignado
    }
    
    @Override
    public void deleteById(Long id) {
        // DELETE FROM mensajes WHERE id = ?
    }
}
```

**Consideraciones:**
- Usar `PreparedStatement` en TODAS las operaciones
- Usar `MensajeMapper.mapRow()` para convertir filas a entidades
- Cerrar recursos en bloques `finally`
- Lanzar `RepositoryException` en caso de error SQL
- El ID del mensaje es autoincremental en MySQL, recuperar con `RETURN_GENERATED_KEYS`

---

### 6. MensajeriaService

**Ubicación:** `LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/MensajeriaService.java`

**Responsabilidades:**
- Enviar mensajes directos
- Enviar mensajes a canales
- Obtener historial de mensajes
- Validar permisos (miembro de canal, usuario existe)
- Integrar con ConnectionManager para notificaciones en tiempo real
- Integrar con LoggerService para auditoría

**Métodos Principales:**

```java
@Service
public class MensajeriaService {
    
    private final IMensajeRepository mensajeRepository;
    private final IUsuarioRepository usuarioRepository;
    private final ICanalMiembroRepository canalMiembroRepository;
    private final ICanalRepository canalRepository;
    private final LoggerService loggerService;
    private final ConnectionManager connectionManager;
    
    /**
     * Envía un mensaje directo de un usuario a otro.
     * 
     * @param dto Datos del mensaje a enviar
     * @return DTOResponse con el mensaje enviado
     */
    public DTOResponse enviarMensajeDirecto(DTOEnviarMensaje dto) {
        // 1. Validar datos con EnviarMensajeValidator
        // 2. Verificar que el remitente existe
        // 3. Verificar que el destinatario existe
        // 4. Crear MensajeEntity con tipo DIRECT
        // 5. Guardar mensaje en BD (recuperar ID generado)
        // 6. Registrar en logs
        // 7. Si destinatario está online, notificar en tiempo real
        // 8. Retornar DTOResponse.success con DTOMensaje
    }
    
    /**
     * Envía un mensaje a un canal.
     * 
     * @param dto Datos del mensaje a enviar
     * @return DTOResponse con el mensaje enviado
     */
    public DTOResponse enviarMensajeCanal(DTOEnviarMensaje dto) {
        // 1. Validar datos con EnviarMensajeValidator
        // 2. Verificar que el remitente existe
        // 3. Verificar que el canal existe y está activo
        // 4. Verificar que el remitente es miembro del canal
        // 5. Crear MensajeEntity con tipo CHANNEL
        // 6. Guardar mensaje en BD (recuperar ID generado)
        // 7. Registrar en logs
        // 8. Notificar a todos los miembros del canal (excepto remitente)
        // 9. Retornar DTOResponse.success con DTOMensaje
    }
    
    /**
     * Obtiene el historial de mensajes (directos o de canal).
     * 
     * @param dto Parámetros de consulta de historial
     * @return DTOResponse con lista de mensajes
     */
    public DTOResponse obtenerHistorial(DTOHistorial dto) {
        // 1. Validar que usuarioId esté presente
        // 2. Validar que haya EXACTAMENTE uno de destinatarioId o canalId
        // 3. Si es historial directo:
        //    - Verificar que ambos usuarios existen
        //    - Obtener mensajes entre los dos usuarios
        // 4. Si es historial de canal:
        //    - Verificar que el canal existe
        //    - Verificar que el usuario es miembro del canal
        //    - Obtener mensajes del canal
        // 5. Para cada mensaje, obtener nombres de usuarios y canales
        // 6. Convertir cada MensajeEntity a DTOMensaje
        // 7. Retornar DTOResponse.success con lista de DTOMensaje
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
            
            // --- ACCIONES DE CANALES ---
            case "crearCanal" -> handleCrearCanal(request);
            case "unirseCanal" -> handleUnirseCanal(request);
            case "salirCanal" -> handleSalirCanal(request);
            case "listarCanales" -> handleListarCanales(request);
            case "listarMiembros" -> handleListarMiembros(request);
            case "gestionarMiembro" -> handleGestionarMiembro(request);
            
            // --- NUEVAS ACCIONES DE MENSAJERÍA ---
            case "enviarMensajeDirecto" -> handleEnviarMensajeDirecto(request);
            case "enviarMensajeCanal" -> handleEnviarMensajeCanal(request);
            case "obtenerHistorial" -> handleObtenerHistorial(request);
            
            default -> DTOResponse.error(action, "Acción no reconocida: " + action);
        };
        
    } catch (Exception e) {
        return exceptionHandler.handleException(e, action, null, ipAddress);
    }
}

// --- MÉTODOS PRIVADOS PARA CADA ACCIÓN DE MENSAJERÍA ---

private DTOResponse handleEnviarMensajeDirecto(DTORequest request) {
    DTOEnviarMensaje dto = gson.fromJson(gson.toJson(request.getPayload()), DTOEnviarMensaje.class);
    return mensajeriaService.enviarMensajeDirecto(dto);
}

private DTOResponse handleEnviarMensajeCanal(DTORequest request) {
    DTOEnviarMensaje dto = gson.fromJson(gson.toJson(request.getPayload()), DTOEnviarMensaje.class);
    return mensajeriaService.enviarMensajeCanal(dto);
}

private DTOResponse handleObtenerHistorial(DTORequest request) {
    DTOHistorial dto = gson.fromJson(gson.toJson(request.getPayload()), DTOHistorial.class);
    return mensajeriaService.obtenerHistorial(dto);
}
```

---

## Orden de Implementación

### Fase 1: DTOs y Validadores (Base)
1. ✅ Crear DTOEnviarMensaje, DTOMensaje, DTOHistorial
2. ✅ Crear ContenidoMensajeValidator
3. ✅ Crear EnviarMensajeValidator

### Fase 2: Entidades y Repositorio
4. ✅ Crear enum TipoMensaje
5. ✅ Crear MensajeEntity con método toDTO()
6. ✅ Crear MensajeMapper
7. ✅ Crear IMensajeRepository
8. ✅ Implementar MensajeRepositoryImpl con JDBC (9 métodos)

### Fase 3: Lógica de Negocio
9. ✅ Implementar MensajeriaService completo
10. ✅ Actualizar ActionDispatcher con las 3 nuevas acciones

### Fase 4: Integración y Pruebas
11. ✅ Compilar y verificar que no hay errores
12. ✅ Probar flujo de mensaje directo
13. ✅ Probar flujo de mensaje de canal
14. ✅ Probar obtención de historial
15. ✅ Verificar logs en base de datos
16. ✅ Verificar notificaciones en tiempo real

---

## Criterios de Aceptación

✅ **Épica 4 estará completa cuando:**

1. Un usuario puede enviar un mensaje directo a otro usuario
2. El destinatario recibe una notificación en tiempo real si está conectado
3. Un usuario puede enviar un mensaje a un canal del que es miembro
4. Todos los miembros del canal reciben la notificación en tiempo real
5. Se puede obtener el historial de mensajes directos entre dos usuarios
6. Se puede obtener el historial de mensajes de un canal
7. Los mensajes se almacenan correctamente en la BD con tipo (DIRECT/CHANNEL)
8. Los mensajes incluyen timestamp de envío
9. Se valida que el usuario sea miembro del canal antes de permitir envío
10. Se valida que el contenido del mensaje no esté vacío y no exceda el límite
11. Todas las operaciones se registran en `logs_sistema`
12. Los mensajes se ordenan por fecha de envío (más recientes primero)
13. La paginación funciona correctamente en el historial
14. No hay errores de compilación (`mvn clean install`)
15. Los datos en la BD son correctos (mensajes, tipos, timestamps)

---

## Verificación Final

### Prueba 1: Enviar Mensaje Directo
```bash
telnet localhost 8080

{"action":"enviarMensajeDirecto","payload":{"remitenteId":"uuid-usuario-1","destinatarioId":"uuid-usuario-2","contenido":"Hola, ¿cómo estás?"}}

# Esperado:
{
  "action":"enviarMensajeDirecto",
  "status":"success",
  "message":"Mensaje enviado exitosamente",
  "data":{
    "id":1,
    "remitenteId":"uuid-usuario-1",
    "remitenteNombre":"Juan Pérez",
    "destinatarioId":"uuid-usuario-2",
    "destinatarioNombre":"María García",
    "tipo":"DIRECT",
    "contenido":"Hola, ¿cómo estás?",
    "fechaEnvio":"2025-10-14T..."
  }
}
```

### Prueba 2: Enviar Mensaje a Canal
```bash
{"action":"enviarMensajeCanal","payload":{"remitenteId":"uuid-usuario-1","canalId":"uuid-canal","contenido":"Buenos días a todos!"}}

# Esperado:
{
  "action":"enviarMensajeCanal",
  "status":"success",
  "message":"Mensaje enviado al canal exitosamente",
  "data":{
    "id":2,
    "remitenteId":"uuid-usuario-1",
    "remitenteNombre":"Juan Pérez",
    "canalId":"uuid-canal",
    "canalNombre":"Canal General",
    "tipo":"CHANNEL",
    "contenido":"Buenos días a todos!",
    "fechaEnvio":"2025-10-14T..."
  }
}
```

### Prueba 3: Obtener Historial de Mensajes Directos
```bash
{"action":"obtenerHistorial","payload":{"usuarioId":"uuid-usuario-1","destinatarioId":"uuid-usuario-2","limit":10,"offset":0}}

# Esperado: Lista de mensajes entre los dos usuarios, ordenados por fecha descendente
```

### Prueba 4: Obtener Historial de Canal
```bash
{"action":"obtenerHistorial","payload":{"usuarioId":"uuid-usuario-1","canalId":"uuid-canal","limit":20,"offset":0}}

# Esperado: Lista de mensajes del canal, ordenados por fecha descendente
```

### Prueba 5: Verificar BD
```sql
-- Verificar que el mensaje fue creado
SELECT id, remitente_id, destinatario_id, canal_id, tipo, contenido, fecha_envio 
FROM mensajes 
ORDER BY fecha_envio DESC 
LIMIT 10;

-- Verificar logs de mensajería
SELECT * FROM logs_sistema 
WHERE accion IN ('enviarMensajeDirecto', 'enviarMensajeCanal', 'obtenerHistorial') 
ORDER BY timestamp DESC;
```

---

## Dependencias Nuevas Requeridas

No se requieren dependencias adicionales. Todas las dependencias necesarias ya están declaradas en el `pom.xml` padre (Spring Boot, JDBC, HikariCP, Gson, etc.).

---

## Estimación de Tiempo

- **Fase 1 (DTOs y Validadores):** ~20-25 minutos
- **Fase 2 (Entidades y Repositorio):** ~40-50 minutos
- **Fase 3 (Lógica de Negocio):** ~45-55 minutos
- **Fase 4 (Integración y Pruebas):** ~20-30 minutos

**Total Estimado:** 125-160 minutos (2.1 - 2.7 horas)

---

## Notas Importantes

🔐 **Seguridad:**
- Validar que el remitente sea miembro del canal antes de permitir envío
- No permitir envío a canales inactivos
- Validar que ambos usuarios existan para mensajes directos
- Validar permisos para obtener historial (miembro del canal)

⚡ **Performance:**
- Usar índices en `mensajes` (remitente_id, destinatario_id, canal_id, fecha_envio)
- Limitar resultados con paginación en historial
- Ordenar por fecha_envio DESC para obtener más recientes primero

📊 **Notificaciones en Tiempo Real:**
- Usar `ConnectionManager.notifyUser()` para mensajes directos
- Usar `ConnectionManager.notifyChannel()` para mensajes de canal
- Enviar notificación solo si el usuario está online
- No enviar notificación al remitente del mensaje

🎯 **Reglas de Negocio:**
- Un mensaje debe tener EXACTAMENTE un destinatario (directo XOR canal)
- Los mensajes directos pueden enviarse aunque el destinatario esté offline
- Los mensajes de canal requieren membresía activa
- El historial requiere permisos (conversación propia o membresía de canal)
- Los mensajes se almacenan permanentemente (no se eliminan automáticamente)

📝 **Funcionalidades Opcionales (para Épica 5 o futuro):**
- Adjuntar archivos a mensajes
- Editar mensajes enviados
- Eliminar mensajes
- Marcar mensajes como leídos
- Búsqueda de mensajes por contenido
- Reacciones a mensajes (emoji)

---

¿Estás listo para comenzar la implementación de la Épica 4?

