package com.unillanos.server.exception;

/**
 * Excepción lanzada cuando falla la autenticación de un usuario.
 * Puede ser por credenciales inválidas, usuario no encontrado, etc.
 */
public class AuthenticationException extends ChatServerException {
    
    private final String reason;

    public AuthenticationException(String message) {
        this(message, "INVALID_CREDENTIALS");
    }

    public AuthenticationException(String message, String reason) {
        super(message, "AUTH_ERROR", reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return String.format("AuthenticationException[reason=%s, message=%s]", reason, getMessage());
    }
}

