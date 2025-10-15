package com.unillanos.server.logs;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Representa una entrada de log en el sistema.
 * 
 * Esta clase encapsula toda la información de un evento de logging,
 * incluyendo tipo, usuario, timestamp, mensaje y datos adicionales.
 * 
 * @author Chat-Unillanos Team
 * @version 1.0.0
 */
public class LogEntry {
    
    private final String id;
    private final LogType type;
    private final String usuarioId;
    private final String ipAddress;
    private final String message;
    private final Map<String, Object> additionalData;
    private final LocalDateTime timestamp;
    
    /**
     * Constructor principal para crear una entrada de log
     */
    public LogEntry(LogType type, String usuarioId, String ipAddress, String message, Map<String, Object> additionalData) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.usuarioId = usuarioId;
        this.ipAddress = ipAddress;
        this.message = message;
        this.additionalData = additionalData;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Constructor para logs del sistema (sin usuario)
     */
    public LogEntry(LogType type, String message, Map<String, Object> additionalData) {
        this(type, null, null, message, additionalData);
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public LogType getType() {
        return type;
    }
    
    public String getUsuarioId() {
        return usuarioId;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Obtiene el nivel de log para SLF4J
     */
    public String getLogLevel() {
        return switch (type) {
            case ERROR -> "ERROR";
            case WARNING -> "WARN";
            case INFO, LOGIN, LOGOUT, SYSTEM, MESSAGE, FILE, CHANNEL -> "INFO";
        };
    }
    
    /**
     * Formatea la entrada como string legible
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(timestamp).append("] ");
        sb.append("[").append(type).append("] ");
        
        if (usuarioId != null) {
            sb.append("Usuario: ").append(usuarioId).append(" ");
        }
        
        if (ipAddress != null) {
            sb.append("IP: ").append(ipAddress).append(" ");
        }
        
        sb.append("Mensaje: ").append(message);
        
        if (additionalData != null && !additionalData.isEmpty()) {
            sb.append(" Datos: ").append(additionalData);
        }
        
        return sb.toString();
    }
    
    /**
     * Convierte la entrada a formato JSON simple
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(id).append("\",");
        sb.append("\"type\":\"").append(type).append("\",");
        sb.append("\"timestamp\":\"").append(timestamp).append("\",");
        
        if (usuarioId != null) {
            sb.append("\"usuarioId\":\"").append(usuarioId).append("\",");
        }
        
        if (ipAddress != null) {
            sb.append("\"ipAddress\":\"").append(ipAddress).append("\",");
        }
        
        sb.append("\"message\":\"").append(message.replace("\"", "\\\"")).append("\"");
        
        if (additionalData != null && !additionalData.isEmpty()) {
            sb.append(",\"additionalData\":").append(mapToJson(additionalData));
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Convierte un Map a JSON simple
     */
    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            
            sb.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
            } else {
                sb.append(value);
            }
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LogEntry logEntry = (LogEntry) obj;
        return id.equals(logEntry.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
