package com.unillanos.server.exception;

/**
 * Excepción base para todas las excepciones del servidor Chat-Unillanos.
 * Proporciona un código de error y detalles adicionales opcionales.
 */
public class ChatServerException extends RuntimeException {
    
    private final String code;
    private final Object details;

    public ChatServerException(String message) {
        this(message, "GENERAL_ERROR", null);
    }

    public ChatServerException(String message, String code) {
        this(message, code, null);
    }

    public ChatServerException(String message, String code, Object details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public ChatServerException(String message, Throwable cause) {
        this(message, "GENERAL_ERROR", null, cause);
    }

    public ChatServerException(String message, String code, Object details, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public Object getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return String.format("ChatServerException[code=%s, message=%s, details=%s]", 
                code, getMessage(), details);
    }
}

