package com.unillanos.server.validation;

import com.unillanos.server.exception.ValidationException;

/**
 * Validador de descripciones de canales.
 * La descripción es opcional, pero si se provee debe cumplir con la longitud máxima.
 */
public class DescripcionCanalValidator {
    
    private static final int MAX_LENGTH = 200;
    
    /**
     * Valida una descripción de canal y lanza excepción si no cumple los requisitos.
     * La descripción es opcional, si es null o vacía no se valida.
     *
     * @param descripcion Descripción del canal a validar
     * @throws ValidationException si la descripción excede la longitud máxima
     */
    public static void validate(String descripcion) throws ValidationException {
        // La descripción es opcional, si es null o vacía no se valida
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return;
        }
        
        if (descripcion.length() > MAX_LENGTH) {
            throw new ValidationException(
                "La descripción es demasiado larga (máx " + MAX_LENGTH + " caracteres)", 
                "descripcion"
            );
        }
    }
}

