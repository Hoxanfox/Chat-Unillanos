package com.unillanos.server.service.impl;

import com.unillanos.server.dto.DTOResponse;
import com.unillanos.server.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Manejador global de excepciones del servidor.
 * Convierte excepciones en respuestas DTOResponse apropiadas y registra los errores.
 */
@Component
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final LoggerService loggerService;

    public GlobalExceptionHandler(LoggerService loggerService) {
        this.loggerService = loggerService;
    }

    /**
     * Maneja cualquier excepción y la convierte en un DTOResponse apropiado.
     *
     * @param exception Excepción a manejar
     * @param action Acción que causó la excepción
     * @param userId ID del usuario (puede ser null si no está autenticado)
     * @param ipAddress Dirección IP del cliente
     * @return DTOResponse con el error apropiado
     */
    public DTOResponse handleException(Exception exception, String action, String userId, String ipAddress) {
        logger.error("Manejando excepción para acción: {}", action, exception);
        
        // Registrar el error en el sistema de logging
        String detalles = String.format("Exception: %s, Message: %s, UserId: %s, IP: %s", 
                exception.getClass().getSimpleName(), exception.getMessage(), 
                userId != null ? userId : "anonymous", ipAddress);
        loggerService.logError(action, detalles);

        // Convertir la excepción a DTOResponse según su tipo
        if (exception instanceof ValidationException) {
            return handleValidationException((ValidationException) exception, action);
        } else if (exception instanceof AuthenticationException) {
            return handleAuthenticationException((AuthenticationException) exception, action);
        } else if (exception instanceof NotFoundException) {
            return handleNotFoundException((NotFoundException) exception, action);
        } else if (exception instanceof DuplicateResourceException) {
            return handleDuplicateResourceException((DuplicateResourceException) exception, action);
        } else if (exception instanceof RepositoryException) {
            return handleRepositoryException((RepositoryException) exception, action);
        } else if (exception instanceof ChatServerException) {
            return handleChatServerException((ChatServerException) exception, action);
        } else {
            return handleGenericException(exception, action);
        }
    }

    private DTOResponse handleValidationException(ValidationException ex, String action) {
        String message = ex.getField() != null 
                ? String.format("Error de validación en el campo '%s': %s", ex.getField(), ex.getMessage())
                : String.format("Error de validación: %s", ex.getMessage());
        return DTOResponse.error(action, message);
    }

    private DTOResponse handleAuthenticationException(AuthenticationException ex, String action) {
        // No exponer detalles de autenticación por seguridad
        return DTOResponse.error(action, "Error de autenticación: " + ex.getMessage());
    }

    private DTOResponse handleNotFoundException(NotFoundException ex, String action) {
        return DTOResponse.error(action, ex.getMessage());
    }

    private DTOResponse handleDuplicateResourceException(DuplicateResourceException ex, String action) {
        return DTOResponse.error(action, ex.getMessage());
    }

    private DTOResponse handleRepositoryException(RepositoryException ex, String action) {
        // No exponer detalles internos de la base de datos
        logger.error("Error en repositorio - Operación: {}", ex.getOperation(), ex);
        return DTOResponse.error(action, "Error al procesar la solicitud. Intente nuevamente.");
    }

    private DTOResponse handleChatServerException(ChatServerException ex, String action) {
        return DTOResponse.error(action, ex.getMessage());
    }

    private DTOResponse handleGenericException(Exception ex, String action) {
        // No exponer detalles internos de excepciones no controladas
        logger.error("Excepción no controlada", ex);
        return DTOResponse.error(action, "Error interno del servidor");
    }
}

