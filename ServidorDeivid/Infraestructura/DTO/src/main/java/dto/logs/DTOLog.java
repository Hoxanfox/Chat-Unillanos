package dto.logs;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO para transferir información de logs entre capas
 */
public class DTOLog {

    private String timestamp;
    private String level;
    private String source;
    private String message;

    public DTOLog() {
    }

    public DTOLog(String level, String source, String message) {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.level = level;
        this.source = source;
        this.message = message;
    }

    public DTOLog(String timestamp, String level, String source, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.source = source;
        this.message = message;
    }

    // Getters y Setters
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s: %s", timestamp, level, source, message);
    }

    /**
     * Convierte el log a un array de objetos para la tabla de la interfaz
     */
    public Object[] toTableRow() {
        return new Object[]{timestamp, level, source, message};
    }
}

