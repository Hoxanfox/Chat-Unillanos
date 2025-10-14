package com.unillanos.server.exception;

/**
 * Excepción lanzada cuando la validación de datos falla.
 * Utilizada por los validadores para indicar que los datos de entrada no son válidos.
 */
public class ValidationException extends ChatServerException {
    
    private final String field;

    public ValidationException(String message) {
        this(message, null);
    }

    public ValidationException(String message, String field) {
        super(message, "VALIDATION_ERROR", field);
        this.field = field;
    }

    public ValidationException(String message, String field, Object details) {
        super(message, "VALIDATION_ERROR", details);
        this.field = field;
    }

    public String getField() {
        return field;
    }

    @Override
    public String toString() {
        return String.format("ValidationException[field=%s, message=%s]", field, getMessage());
    }
}

