package com.unillanos.server.logs;

/**
 * Enum que define los tipos de logs disponibles en el sistema.
 * 
 * Cada tipo representa una categoría específica de eventos que pueden
 * ser registrados en el sistema de logging.
 * 
 * @author Chat-Unillanos Team
 * @version 1.0.0
 */
public enum LogType {
    
    /**
     * Logs de información general del sistema
     */
    INFO("Información", "I"),
    
    /**
     * Logs de errores del sistema
     */
    ERROR("Error", "E"),
    
    /**
     * Logs de advertencias
     */
    WARNING("Advertencia", "W"),
    
    /**
     * Logs de eventos de login de usuarios
     */
    LOGIN("Login", "L"),
    
    /**
     * Logs de eventos de logout de usuarios
     */
    LOGOUT("Logout", "O"),
    
    /**
     * Logs de eventos del sistema (inicio, parada, etc.)
     */
    SYSTEM("Sistema", "S"),
    
    /**
     * Logs de eventos de mensajería
     */
    MESSAGE("Mensaje", "M"),
    
    /**
     * Logs de eventos de archivos
     */
    FILE("Archivo", "F"),
    
    /**
     * Logs de eventos de canales
     */
    CHANNEL("Canal", "C");
    
    private final String description;
    private final String shortCode;
    
    LogType(String description, String shortCode) {
        this.description = description;
        this.shortCode = shortCode;
    }
    
    /**
     * Obtiene la descripción legible del tipo de log
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Obtiene el código corto del tipo de log
     */
    public String getShortCode() {
        return shortCode;
    }
    
    /**
     * Convierte un string a LogType
     */
    public static LogType fromString(String typeString) {
        if (typeString == null) {
            return INFO;
        }
        
        try {
            return LogType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Si no se encuentra el tipo, devolver INFO por defecto
            return INFO;
        }
    }
    
    /**
     * Verifica si el tipo es un error
     */
    public boolean isError() {
        return this == ERROR;
    }
    
    /**
     * Verifica si el tipo es una advertencia
     */
    public boolean isWarning() {
        return this == WARNING;
    }
    
    /**
     * Verifica si el tipo es información
     */
    public boolean isInfo() {
        return this == INFO || this == LOGIN || this == LOGOUT || 
               this == SYSTEM || this == MESSAGE || this == FILE || this == CHANNEL;
    }
    
    /**
     * Obtiene el color asociado al tipo para interfaces gráficas
     */
    public String getColor() {
        return switch (this) {
            case ERROR -> "#FF4444";      // Rojo
            case WARNING -> "#FF8800";    // Naranja
            case INFO -> "#4488FF";       // Azul
            case LOGIN -> "#44FF44";      // Verde
            case LOGOUT -> "#888888";     // Gris
            case SYSTEM -> "#8844FF";     // Morado
            case MESSAGE -> "#44FFFF";    // Cian
            case FILE -> "#FF44FF";       // Magenta
            case CHANNEL -> "#FFFF44";    // Amarillo
        };
    }
    
    /**
     * Obtiene el ícono asociado al tipo para interfaces gráficas
     */
    public String getIcon() {
        return switch (this) {
            case ERROR -> "⚠️";
            case WARNING -> "⚠️";
            case INFO -> "ℹ️";
            case LOGIN -> "🔐";
            case LOGOUT -> "🚪";
            case SYSTEM -> "⚙️";
            case MESSAGE -> "💬";
            case FILE -> "📁";
            case CHANNEL -> "📢";
        };
    }
    
    @Override
    public String toString() {
        return description;
    }
}
