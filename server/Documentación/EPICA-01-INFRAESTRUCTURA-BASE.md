# Épica 1: Infraestructura Base - Plan Detallado

## Objetivo General

Implementar la infraestructura fundamental del servidor que soportará todas las funcionalidades futuras: gestión de conexiones, sistema de notificaciones en tiempo real, logging persistente y manejo de errores robusto.

## Contexto Actual

✅ **Ya Implementado:**
- Servidor Netty con hilos virtuales (Java 21)
- DTORequest y DTOResponse base
- IActionDispatcher y ActionDispatcherImpl (stub)
- Configuración de base de datos (DatabaseConfig, NettyConfig)
- LoggerService básico (solo consola)
- IUsuarioRepository (stub)

## Componentes a Implementar

### 1. ConnectionManager (Gestión de Conexiones)

**Ubicación:** `LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/ConnectionManager.java`

**Responsabilidades:**
- Mantener un mapa thread-safe de usuarios conectados: `Map<String userId, ChannelHandlerContext ctx>`
- Registrar nuevas conexiones cuando un usuario se autentica
- Eliminar conexiones cuando un usuario se desconecta
- Proporcionar métodos para enviar notificaciones a usuarios específicos
- Proporcionar métodos para enviar broadcast a todos los usuarios conectados
- Proporcionar métodos para enviar mensajes a todos los miembros de un canal

**Métodos Clave:**
```java
void registerConnection(String userId, ChannelHandlerContext ctx)
void removeConnection(String userId)
boolean isUserOnline(String userId)
void notifyUser(String userId, DTOResponse notification)
void notifyChannel(String channelId, DTOResponse notification, Set<String> memberIds)
void broadcast(DTOResponse notification)
Map<String, ChannelHandlerContext> getAllConnections() // Para GUI
```

**Características:**
- Usa `ConcurrentHashMap` para thread-safety
- Usa hilos virtuales de Java 21 para envío paralelo de notificaciones
- Maneja desconexiones inesperadas (limpieza automática)

---

### 2. LoggerService Mejorado (Logging Persistente)

**Ubicación:** `Infraestructura/Logs/src/main/java/com/unillanos/server/logs/LoggerService.java` (actualizar)

**Responsabilidades:**
- Registrar eventos en consola (SLF4J) Y en base de datos
- Soportar diferentes tipos de logs: LOGIN, LOGOUT, ERROR, INFO, SYSTEM
- Proporcionar métodos específicos por tipo de evento
- No bloquear el hilo principal (usar hilos virtuales para escritura en BD)

**Métodos a Implementar:**
```java
void logLogin(String usuarioId, String ipAddress, String detalles)
void logLogout(String usuarioId, String ipAddress, String detalles)
void logError(String accion, String detalles, String usuarioId)
void logInfo(String accion, String detalles, String usuarioId)
void logSystem(String accion, String detalles)
```

**Tabla BD:** `logs_sistema`
```sql
CREATE TABLE logs_sistema (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tipo ENUM('LOGIN', 'LOGOUT', 'ERROR', 'INFO', 'SYSTEM'),
    usuario_id VARCHAR(36),
    ip_address VARCHAR(45),
    accion VARCHAR(100),
    detalles TEXT
);
```

**Implementación:**
- Repositorio: `ILogRepository` + `LogRepositoryImpl`
- Modelo: `LogEntity`
- Escritura asíncrona con hilos virtuales

---

### 3. Sistema de Excepciones Personalizadas

**Ubicación:** `Infraestructura/DTOs/src/main/java/com/unillanos/server/exception/`

**Excepciones a Crear:**

```java
// Excepción base
public class ChatServerException extends RuntimeException {
    private final String code;
    private final Object details;
}

// Excepciones específicas
public class ValidationException extends ChatServerException
public class AuthenticationException extends ChatServerException
public class RepositoryException extends ChatServerException
public class NotFoundException extends ChatServerException
public class DuplicateResourceException extends ChatServerException
```

**Uso:**
- Lanzar excepciones específicas en servicios
- Capturarlas en ActionDispatcher
- Convertirlas a DTOResponse con mensaje apropiado
- Registrarlas en LoggerService

---

### 4. Manejador Global de Excepciones

**Ubicación:** `LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/ExceptionHandler.java`

**Responsabilidad:**
- Capturar excepciones en ActionDispatcher
- Convertir excepciones a DTOResponse apropiados
- Registrar errores en LoggerService
- No exponer detalles internos al cliente

**Método Principal:**
```java
DTOResponse handleException(Exception e, String action, String userId)
```

---

### 5. Actualizar ClientRequestHandler (Netty)

**Ubicación:** `Infraestructura/Netty/src/main/java/com/unillanos/server/netty/handler/ClientRequestHandler.java`

**Mejoras a Implementar:**
- Extraer IP del cliente desde el contexto
- Pasar IP al ActionDispatcher para logging
- Manejar desconexiones abruptas
- Notificar a ConnectionManager cuando un canal se cierra

**Nuevos Métodos:**
```java
@Override
public void channelInactive(ChannelHandlerContext ctx) {
    // Obtener userId del contexto (si está autenticado)
    // Notificar a ConnectionManager
    // Registrar desconexión en logs
}

@Override
public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    // Registrar error
    // Cerrar conexión de forma segura
}
```

---

### 6. Actualizar ActionDispatcher

**Ubicación:** `LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/ActionDispatcherImpl.java`

**Mejoras:**
- Integrar ExceptionHandler
- Registrar todas las acciones en LoggerService
- Extraer IP del ChannelHandlerContext
- Preparar enrutamiento para acciones futuras

**Estructura Mejorada:**
```java
@Override
public DTOResponse dispatch(DTORequest request, ChannelHandlerContext ctx) {
    String ipAddress = extractIpAddress(ctx);
    
    try {
        logger.info("Acción recibida: {} desde IP: {}", request.getAction(), ipAddress);
        
        // Enrutamiento según acción
        return switch (request.getAction()) {
            case "ping" -> handlePing(request);
            // Más acciones se añadirán en épicas posteriores
            default -> DTOResponse.error(request.getAction(), "Acción no reconocida");
        };
        
    } catch (Exception e) {
        return exceptionHandler.handleException(e, request.getAction(), null);
    }
}
```

---

### 7. Repositorio de Logs

**Ubicación:** `Datos/Repositorios/src/main/java/com/unillanos/server/repository/`

**Archivos a Crear:**

1. **interfaces/ILogRepository.java**
```java
public interface ILogRepository {
    void save(LogEntity log);
    List<LogEntity> findByType(String type, int limit);
    List<LogEntity> findByUsuarioId(String usuarioId, int limit);
    List<LogEntity> findRecent(int limit);
}
```

2. **models/LogEntity.java**
```java
public class LogEntity {
    private Long id;
    private LocalDateTime timestamp;
    private String tipo;
    private String usuarioId;
    private String ipAddress;
    private String accion;
    private String detalles;
}
```

3. **impl/LogRepositoryImpl.java**
- Implementar con JDBC puro
- Usar PreparedStatement
- Usar HikariCP para conexiones

4. **mappers/LogMapper.java**
- Convertir ResultSet a LogEntity

---

### 8. Acción de Prueba: PING

**Objetivo:** Verificar que toda la infraestructura funciona correctamente

**Implementación:**
- Cliente envía: `{"action": "ping", "payload": {}}`
- Servidor responde: `{"action": "ping", "status": "success", "message": "pong", "data": {"timestamp": "..."}}`
- Se registra en logs como tipo INFO
- Se prueba que ConnectionManager funciona (si el cliente está "conectado")

---

## Orden de Implementación

### Fase 1: Excepciones y Manejo de Errores
1. ✅ Crear excepciones personalizadas (ChatServerException, ValidationException, etc.)
2. ✅ Crear ExceptionHandler
3. ✅ Actualizar ActionDispatcher para usar ExceptionHandler

### Fase 2: Logging Persistente
4. ✅ Crear LogEntity
5. ✅ Crear ILogRepository e implementación
6. ✅ Crear LogMapper
7. ✅ Actualizar LoggerService para persistir en BD

### Fase 3: Gestión de Conexiones
8. ✅ Crear ConnectionManager
9. ✅ Actualizar ClientRequestHandler para integrar ConnectionManager
10. ✅ Probar registro/desregistro de conexiones

### Fase 4: Integración y Pruebas
11. ✅ Implementar acción PING en ActionDispatcher
12. ✅ Probar flujo completo: Cliente → Netty → Dispatcher → Logs → Response
13. ✅ Verificar que todo se registra correctamente en BD

---

## Estructura de Archivos Final

```
LogicaNegocio/Servicios/
├── service/
│   ├── interfaces/
│   │   └── IActionDispatcher.java (ya existe)
│   └── impl/
│       ├── ActionDispatcherImpl.java (actualizar)
│       ├── ConnectionManager.java (NUEVO)
│       └── ExceptionHandler.java (NUEVO)

Infraestructura/DTOs/
├── dto/
│   ├── DTORequest.java (ya existe)
│   └── DTOResponse.java (ya existe)
└── exception/ (NUEVO)
    ├── ChatServerException.java
    ├── ValidationException.java
    ├── AuthenticationException.java
    ├── RepositoryException.java
    ├── NotFoundException.java
    └── DuplicateResourceException.java

Infraestructura/Logs/
└── logs/
    └── LoggerService.java (actualizar)

Infraestructura/Netty/
└── netty/
    └── handler/
        └── ClientRequestHandler.java (actualizar)

Datos/Repositorios/
├── repository/
│   ├── interfaces/
│   │   ├── IUsuarioRepository.java (ya existe)
│   │   └── ILogRepository.java (NUEVO)
│   ├── impl/
│   │   ├── UsuarioRepositoryImpl.java (ya existe)
│   │   └── LogRepositoryImpl.java (NUEVO)
│   ├── models/
│   │   ├── UsuarioEntity.java (ya existe)
│   │   └── LogEntity.java (NUEVO)
│   └── mappers/
│       └── LogMapper.java (NUEVO)
```

---

## Criterios de Aceptación

✅ **Épica 1 estará completa cuando:**

1. El ConnectionManager puede registrar y desregistrar usuarios
2. El ConnectionManager puede enviar notificaciones a usuarios específicos
3. Todos los eventos se registran en la base de datos (tabla logs_sistema)
4. Las excepciones se manejan de forma consistente y se convierten a DTOResponse
5. El servidor responde correctamente a la acción "ping"
6. No hay errores de compilación
7. El servidor arranca sin errores y escucha en el puerto 8080
8. Los logs se guardan correctamente en MySQL

---

## Verificación Final

### Prueba 1: Servidor Arranca Correctamente
```bash
mvn -pl Presentacion/Main spring-boot:run
```
**Esperado:** Servidor inicia sin errores, Netty escucha en puerto 8080

### Prueba 2: Base de Datos
```sql
SELECT * FROM logs_sistema ORDER BY timestamp DESC LIMIT 10;
```
**Esperado:** Ver logs de inicio del sistema

### Prueba 3: Acción PING (usando telnet o cliente de prueba)
```bash
# Conectar al servidor
telnet localhost 8080

# Enviar
{"action":"ping","payload":{}}

# Esperado:
{"action":"ping","status":"success","message":"pong","data":{"timestamp":"2025-10-14T..."}}
```

### Prueba 4: Logs en Base de Datos
```sql
SELECT * FROM logs_sistema WHERE accion = 'ping';
```
**Esperado:** Ver registro de la acción ping

---

## Estimación de Tiempo

- **Fase 1 (Excepciones):** ~15-20 minutos
- **Fase 2 (Logging):** ~25-30 minutos
- **Fase 3 (ConnectionManager):** ~20-25 minutos
- **Fase 4 (Integración y Pruebas):** ~15-20 minutos

**Total Estimado:** 75-95 minutos

---

## Dependencias para Épicas Futuras

Esta infraestructura base será utilizada por:

- **Épica 2 (Usuarios):** ConnectionManager para actualizar estado online/offline, LoggerService para auditoría
- **Épica 3 (Canales):** ConnectionManager para notificar a miembros del canal
- **Épica 4 (Mensajería):** ConnectionManager para enviar mensajes en tiempo real
- **Épica 5 (Archivos):** LoggerService para registrar subidas/descargas
- **Épica 6 (GUI):** ConnectionManager para mostrar usuarios conectados

---

## Notas Importantes

🔥 **Hilos Virtuales (Java 21):** 
- ConnectionManager usa hilos virtuales para enviar notificaciones en paralelo
- LoggerService usa hilos virtuales para escritura asíncrona en BD
- Esto permite manejar miles de conexiones sin overhead

⚠️ **Thread Safety:**
- ConnectionManager usa ConcurrentHashMap
- Todas las operaciones de escritura en BD son thread-safe
- No compartir estado mutable entre hilos

📊 **Performance:**
- El envío de notificaciones es no bloqueante
- La escritura de logs no bloquea el flujo principal
- El servidor puede manejar múltiples peticiones simultáneas

---

¿Estás listo para comenzar? Podemos proceder fase por fase, verificando cada componente antes de continuar con el siguiente.

