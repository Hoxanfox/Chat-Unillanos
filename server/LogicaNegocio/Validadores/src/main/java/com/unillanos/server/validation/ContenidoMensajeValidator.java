package com.unillanos.server.validation;

import com.unillanos.server.exception.ValidationException;

/**
 * Validador del contenido de mensajes.
 * Valida longitud y que no esté vacío.
 */
public class ContenidoMensajeValidator {
    
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 2000;
    
    /**
     * Valida el contenido de un mensaje y lanza excepción si no cumple los requisitos.
     *
     * @param contenido Contenido del mensaje a validar
     * @throws ValidationException si el contenido no es válido
     */
    public static void validate(String contenido) throws ValidationException {
        if (contenido == null || contenido.trim().isEmpty()) {
            throw new ValidationException("El contenido del mensaje es requerido", "contenido");
        }
        
        String contenidoTrim = contenido.trim();
        
        if (contenidoTrim.length() < MIN_LENGTH) {
            throw new ValidationException(
                "El mensaje no puede estar vacío", 
                "contenido"
            );
        }
        
        if (contenidoTrim.length() > MAX_LENGTH) {
            throw new ValidationException(
                "El mensaje es demasiado largo (máx " + MAX_LENGTH + " caracteres)", 
                "contenido"
            );
        }
    }
}

