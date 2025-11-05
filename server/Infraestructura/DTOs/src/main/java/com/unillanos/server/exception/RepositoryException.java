package com.unillanos.server.exception;

/**
 * Excepción lanzada cuando ocurre un error en la capa de repositorio.
 * Encapsula errores de base de datos, conexión, etc.
 */
public class RepositoryException extends ChatServerException {
    
    private final String operation;

    public RepositoryException(String message, String operation) {
        super(message, "REPOSITORY_ERROR", operation);
        this.operation = operation;
    }

    public RepositoryException(String message, String operation, Throwable cause) {
        super(message, "REPOSITORY_ERROR", operation, cause);
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }

    @Override
    public String toString() {
        return String.format("RepositoryException[operation=%s, message=%s]", operation, getMessage());
    }
}

