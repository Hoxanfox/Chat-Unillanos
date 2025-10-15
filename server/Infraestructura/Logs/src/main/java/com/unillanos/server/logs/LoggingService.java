package com.unillanos.server.logs;

import com.unillanos.server.dto.DTONotificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servicio de logging centralizado para el sistema Chat-Unillanos.
 * 
 * Este servicio proporciona:
 * - Logging estructurado a base de datos
 * - Logging a archivos con Logback
 * - Sistema de notificaciones en tiempo real
 * - Cache en memoria para logs recientes
 * 
 * @author Chat-Unillanos Team
 * @version 1.0.0
 */
@Service("loggingService")
public class LoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Cache en memoria para logs recientes (últimos 1000 logs)
    private final List<LogEntry> recentLogs = new CopyOnWriteArrayList<>();
    private final Map<String, LogEntry> logCache = new ConcurrentHashMap<>();
    
    // Observadores para notificaciones en tiempo real
    private final List<LogObserver> observers = new CopyOnWriteArrayList<>();
    
    // Scheduler para limpiar logs antiguos
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
        Thread.ofVirtual().factory()
    );
    
    public LoggingService() {
        // Limpiar logs antiguos cada hora
        scheduler.scheduleAtFixedRate(this::cleanupOldLogs, 1, 1, TimeUnit.HOURS);
        logger.info("LoggingService inicializado correctamente");
    }
    
    /**
     * Registra un evento de login en el sistema
     */
    public void logLogin(String usuarioId, String ipAddress) {
        LogEntry entry = new LogEntry(
            LogType.LOGIN,
            usuarioId,
            ipAddress,
            "Usuario inició sesión",
            Map.of("action", "login", "ip", ipAddress)
        );
        
        logEvent(entry);
        
        // Notificar a observadores
        notifyObservers("LOG", Map.of(
            "tipo", "LOGIN",
            "usuarioId", usuarioId,
            "ipAddress", ipAddress,
            "timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        ));
    }
    
    /**
     * Registra un evento de logout en el sistema
     */
    public void logLogout(String usuarioId, String ipAddress) {
        LogEntry entry = new LogEntry(
            LogType.LOGOUT,
            usuarioId,
            ipAddress,
            "Usuario cerró sesión",
            Map.of("action", "logout", "ip", ipAddress)
        );
        
        logEvent(entry);
        
        // Notificar a observadores
        notifyObservers("LOG", Map.of(
            "tipo", "LOGOUT",
            "usuarioId", usuarioId,
            "ipAddress", ipAddress,
            "timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        ));
    }
    
    /**
     * Registra un evento de registro de usuario
     */
    public void logUserRegistration(String usuarioId, String email) {
        LogEntry entry = new LogEntry(
            LogType.INFO,
            usuarioId,
            null,
            "Usuario registrado: " + email,
            Map.of("action", "registration", "email", email)
        );
        
        logEvent(entry);
        
        // Notificar a observadores
        notifyObservers("LOG", Map.of(
            "tipo", "REGISTRATION",
            "usuarioId", usuarioId,
            "email", email,
            "timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        ));
    }
    
    /**
     * Registra un error del sistema
     */
    public void logError(String message, Throwable throwable, String context) {
        LogEntry entry = new LogEntry(
            LogType.ERROR,
            null,
            null,
            message,
            Map.of(
                "error", throwable != null ? throwable.getMessage() : "N/A",
                "context", context,
                "stackTrace", throwable != null ? getStackTrace(throwable) : "N/A"
            )
        );
        
        logEvent(entry);
        
        // Log también con SLF4J para archivos
        logger.error("Error en {}: {}", context, message, throwable);
        
        // Notificar a observadores
        notifyObservers("LOG", Map.of(
            "tipo", "ERROR",
            "message", message,
            "context", context,
            "timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        ));
    }
    
    /**
     * Registra eventos del sistema (inicio, parada, etc.)
     */
    public void logSystemEvent(String event, String details) {
        LogEntry entry = new LogEntry(
            LogType.SYSTEM,
            null,
            null,
            event,
            Map.of("details", details)
        );
        
        logEvent(entry);
        
        // Log también con SLF4J
        logger.info("Evento del sistema: {} - {}", event, details);
        
        // Notificar a observadores
        notifyObservers("LOG", Map.of(
            "tipo", "SYSTEM",
            "event", event,
            "details", details,
            "timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        ));
    }
    
    /**
     * Registra eventos de mensajería
     */
    public void logMessageEvent(String action, String remitenteId, String destinatarioId, String canalId) {
        LogEntry entry = new LogEntry(
            LogType.INFO,
            remitenteId,
            null,
            "Evento de mensajería: " + action,
            Map.of(
                "action", action,
                "remitenteId", remitenteId,
                "destinatarioId", destinatarioId,
                "canalId", canalId
            )
        );
        
        logEvent(entry);
        
        // Notificar a observadores
        notifyObservers("LOG", Map.of(
            "tipo", "MESSAGE",
            "action", action,
            "remitenteId", remitenteId,
            "destinatarioId", destinatarioId,
            "canalId", canalId,
            "timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        ));
    }
    
    /**
     * Registra eventos de gestión de archivos
     */
    public void logFileEvent(String action, String usuarioId, String archivoId, String nombreArchivo) {
        LogEntry entry = new LogEntry(
            LogType.INFO,
            usuarioId,
            null,
            "Evento de archivo: " + action,
            Map.of(
                "action", action,
                "archivoId", archivoId,
                "nombreArchivo", nombreArchivo
            )
        );
        
        logEvent(entry);
        
        // Notificar a observadores
        notifyObservers("LOG", Map.of(
            "tipo", "FILE",
            "action", action,
            "usuarioId", usuarioId,
            "archivoId", archivoId,
            "nombreArchivo", nombreArchivo,
            "timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        ));
    }
    
    /**
     * Registra eventos de gestión de canales
     */
    public void logChannelEvent(String action, String usuarioId, String canalId, String canalNombre) {
        LogEntry entry = new LogEntry(
            LogType.INFO,
            usuarioId,
            null,
            "Evento de canal: " + action,
            Map.of(
                "action", action,
                "canalId", canalId,
                "canalNombre", canalNombre
            )
        );
        
        logEvent(entry);
        
        // Notificar a observadores
        notifyObservers("LOG", Map.of(
            "tipo", "CHANNEL",
            "action", action,
            "usuarioId", usuarioId,
            "canalId", canalId,
            "canalNombre", canalNombre,
            "timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        ));
    }
    
    /**
     * Obtiene los logs recientes del sistema
     */
    public List<LogEntry> getRecentLogs(int limit) {
        List<LogEntry> result = new ArrayList<>();
        int startIndex = Math.max(0, recentLogs.size() - limit);
        
        for (int i = startIndex; i < recentLogs.size(); i++) {
            result.add(recentLogs.get(i));
        }
        
        return result;
    }
    
    /**
     * Obtiene logs filtrados por tipo
     */
    public List<LogEntry> getLogsByType(LogType type, int limit) {
        return recentLogs.stream()
            .filter(log -> log.getType() == type)
            .limit(limit)
            .toList();
    }
    
    /**
     * Obtiene logs de un usuario específico
     */
    public List<LogEntry> getLogsByUser(String usuarioId, int limit) {
        return recentLogs.stream()
            .filter(log -> usuarioId.equals(log.getUsuarioId()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Registra un evento genérico en el sistema
     */
    private void logEvent(LogEntry entry) {
        // Agregar a cache en memoria
        recentLogs.add(entry);
        
        // Mantener solo los últimos 1000 logs en memoria
        if (recentLogs.size() > 1000) {
            recentLogs.remove(0);
        }
        
        // Cache individual por ID
        logCache.put(entry.getId(), entry);
        
        // Log con SLF4J para archivos
        String logMessage = String.format("[%s] %s - %s", 
            entry.getType(), 
            entry.getUsuarioId() != null ? entry.getUsuarioId() : "SYSTEM",
            entry.getMessage());
            
        switch (entry.getType()) {
            case ERROR -> logger.error(logMessage);
            case WARNING -> logger.warn(logMessage);
            case INFO, LOGIN, LOGOUT, SYSTEM, MESSAGE, FILE, CHANNEL -> logger.info(logMessage);
        }
    }
    
    /**
     * Notifica a todos los observadores registrados
     */
    private void notifyObservers(String tipo, Map<String, Object> datos) {
        observers.forEach(observer -> {
            try {
                observer.onLogEvent(tipo, datos);
            } catch (Exception e) {
                logger.error("Error al notificar observador: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Agrega un observador para eventos de log
     */
    public void addObserver(LogObserver observer) {
        observers.add(observer);
    }
    
    /**
     * Remueve un observador
     */
    public void removeObserver(LogObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Limpia logs antiguos del cache
     */
    private void cleanupOldLogs() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
            
            recentLogs.removeIf(log -> log.getTimestamp().isBefore(cutoff));
            logCache.entrySet().removeIf(entry -> 
                entry.getValue().getTimestamp().isBefore(cutoff));
                
            logger.debug("Limpieza de logs completada. Logs en memoria: {}", recentLogs.size());
        } catch (Exception e) {
            logger.error("Error durante limpieza de logs: {}", e.getMessage());
        }
    }
    
    /**
     * Obtiene el stack trace como string
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) return "N/A";
        
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Cierra el servicio y libera recursos
     */
    public void shutdown() {
        scheduler.shutdown();
        observers.clear();
        recentLogs.clear();
        logCache.clear();
        logger.info("LoggerService cerrado correctamente");
    }
}
