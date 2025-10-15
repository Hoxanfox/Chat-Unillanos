# Módulo Logs - Sistema de Logging y Auditoría

Este módulo proporciona el sistema de logging centralizado para el servidor Chat-Unillanos, incluyendo logging estructurado, auditoría y notificaciones en tiempo real.

## Características

- ✅ **Logging estructurado** con diferentes tipos de eventos
- ✅ **Cache en memoria** para logs recientes (últimos 1000)
- ✅ **Patrón Observer** para notificaciones en tiempo real
- ✅ **Múltiples formatos** de salida (texto, HTML, JSON, CSV)
- ✅ **Integración con Logback** para archivos de log
- ✅ **Limpieza automática** de logs antiguos
- ✅ **Thread-safe** para uso concurrente

## Componentes Principales

### LoggingService
Servicio principal que centraliza todo el logging del sistema.

```java
@Service("loggingService")
public class LoggingService {
    // Métodos para diferentes tipos de eventos
    public void logLogin(String usuarioId, String ipAddress);
    public void logLogout(String usuarioId, String ipAddress);
    public void logError(String message, Throwable throwable, String context);
    public void logSystemEvent(String event, String details);
    public void logMessageEvent(String action, String remitenteId, String destinatarioId, String canalId);
    public void logFileEvent(String action, String usuarioId, String archivoId, String nombreArchivo);
    public void logChannelEvent(String action, String usuarioId, String canalId, String canalNombre);
}
```

### LogEntry
Representa una entrada de log individual con toda la información del evento.

```java
public class LogEntry {
    private String id;                    // UUID único
    private LogType type;                 // Tipo de evento
    private String usuarioId;             // ID del usuario (opcional)
    private String ipAddress;             // IP del usuario (opcional)
    private String message;               // Mensaje del evento
    private Map<String, Object> additionalData; // Datos adicionales
    private LocalDateTime timestamp;      // Timestamp del evento
}
```

### LogType
Enum que define los tipos de logs disponibles:

- `INFO` - Información general
- `ERROR` - Errores del sistema
- `WARNING` - Advertencias
- `LOGIN` - Eventos de login
- `LOGOUT` - Eventos de logout
- `SYSTEM` - Eventos del sistema
- `MESSAGE` - Eventos de mensajería
- `FILE` - Eventos de archivos
- `CHANNEL` - Eventos de canales

### LogObserver
Interfaz para el patrón Observer, permite suscribirse a eventos de logging.

```java
public interface LogObserver {
    void onLogEvent(String tipo, Map<String, Object> datos);
}
```

### LogFormatter
Utilidades para formatear logs en diferentes formatos:

```java
public class LogFormatter {
    public static String formatAsText(LogEntry entry);
    public static String formatAsHtml(LogEntry entry);
    public static String formatAsJson(LogEntry entry);
    public static String formatAsCsv(LogEntry entry);
    public static String formatListAsText(List<LogEntry> logs);
    public static String formatListAsHtml(List<LogEntry> logs);
    public static String formatListAsCsv(List<LogEntry> logs);
    public static String formatStats(List<LogEntry> logs);
}
```

## Uso

### Inyección de Dependencias

```java
@Service
public class MiServicio {
    private final LoggingService loggingService;
    
    public MiServicio(LoggingService loggingService) {
        this.loggingService = loggingService;
    }
    
    public void miMetodo() {
        try {
            // ... lógica del método ...
            loggingService.logInfo("Operación completada exitosamente");
        } catch (Exception e) {
            loggingService.logError("Error en operación", e, "MiServicio.miMetodo");
        }
    }
}
```

### Suscripción a Eventos

```java
@Component
public class MiObserver implements LogObserver {
    @Autowired
    private LoggingService loggingService;
    
    @PostConstruct
    public void init() {
        loggingService.addObserver(this);
    }
    
    @Override
    public void onLogEvent(String tipo, Map<String, Object> datos) {
        // Procesar evento de log
        System.out.println("Evento recibido: " + tipo + " - " + datos);
    }
}
```

### Formateo de Logs

```java
// Obtener logs recientes
List<LogEntry> logs = loggingService.getRecentLogs(100);

// Formatear como HTML
String htmlLogs = LogFormatter.formatListAsHtml(logs);

// Formatear como CSV para exportar
String csvLogs = LogFormatter.formatListAsCsv(logs);

// Obtener estadísticas
String stats = LogFormatter.formatStats(logs);
```

## Configuración

### Logback (logback-spring.xml)

El sistema utiliza Logback con la siguiente configuración:

- **Console Appender**: Para desarrollo y debugging
- **File Appender**: Logs generales con rotación por tamaño y tiempo
- **Error File Appender**: Solo errores en archivo separado
- **Audit File Appender**: Logs de auditoría con retención extendida
- **Async Appender**: Para mejor rendimiento

### Archivos de Log

Los logs se almacenan en:

- `./logs/chat-unillanos.log` - Logs generales
- `./logs/chat-unillanos-error.log` - Solo errores
- `./logs/chat-unillanos-audit.log` - Logs de auditoría

### Rotación de Archivos

- **Tamaño máximo**: 10MB por archivo
- **Retención general**: 30 días
- **Retención errores**: 90 días
- **Retención auditoría**: 365 días
- **Tamaño total máximo**: 1GB (general), 500MB (errores), 2GB (auditoría)

## Integración con el Sistema

### En Servicios de Negocio

```java
@Service
public class AutenticacionService {
    private final LoggingService loggingService;
    
    public DTOResponse login(DTOLogin dto) {
        try {
            // ... lógica de autenticación ...
            
            // Log del evento
            loggingService.logLogin(usuario.getId(), dto.getIpAddress());
            
            return DTOResponse.success("login", "Login exitoso", usuarioData);
        } catch (Exception e) {
            loggingService.logError("Error en login", e, "AutenticacionService.login");
            return DTOResponse.error("login", "Error en autenticación");
        }
    }
}
```

### En GUI Controllers

```java
@Controller
public class LogsController {
    private final LoggingService loggingService;
    
    @PostConstruct
    public void init() {
        // Suscribirse a eventos de log
        loggingService.addObserver((tipo, datos) -> {
            Platform.runLater(() -> {
                // Actualizar UI con nuevo log
                actualizarLogsEnUI(datos);
            });
        });
    }
}
```

## Métricas y Monitoreo

El sistema proporciona métricas útiles:

```java
// Obtener logs por tipo
List<LogEntry> errorLogs = loggingService.getLogsByType(LogType.ERROR, 50);

// Obtener logs de usuario específico
List<LogEntry> userLogs = loggingService.getLogsByUser("user123", 100);

// Obtener estadísticas
String stats = LogFormatter.formatStats(logs);
```

## Consideraciones de Rendimiento

- **Cache en memoria**: Mantiene solo los últimos 1000 logs
- **Limpieza automática**: Elimina logs antiguos cada hora
- **Async logging**: Utiliza AsyncAppender para mejor rendimiento
- **Thread-safe**: Diseñado para uso concurrente con Virtual Threads

## Dependencias

- `org.slf4j:slf4j-api` - API de logging
- `ch.qos.logback:logback-classic` - Implementación de logging
- `org.springframework:spring-context` - Inyección de dependencias
- `com.unillanos:dtos` - DTOs del sistema

## Ejemplos de Uso

Ver las clases de ejemplo en el paquete `com.unillanos.server.logs` para más detalles sobre cómo usar cada componente.
