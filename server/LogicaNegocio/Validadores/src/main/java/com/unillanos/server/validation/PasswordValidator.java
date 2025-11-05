package com.unillanos.server.validation;

import com.unillanos.server.config.ServerConfigProperties;
import com.unillanos.server.exception.ValidationException;

/**
 * Validador de contraseñas.
 * Valida longitud y fortaleza (debe contener al menos una letra y un número).
 */
public class PasswordValidator {
    
    private static final int MAX_LENGTH = 100;
    
    /**
     * Valida una contraseña y lanza excepción si no cumple los requisitos.
     *
     * @param password Contraseña a validar
     * @param config Configuración del servidor
     * @throws ValidationException si la contraseña no es válida
     */
    public static void validate(String password, ServerConfigProperties config) throws ValidationException {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("La contraseña es requerida", "password");
        }
        
        if (password.length() < config.getSeguridad().getLongitudMinPassword()) {
            throw new ValidationException(
                "La contraseña debe tener al menos " + config.getSeguridad().getLongitudMinPassword() + " caracteres", 
                "password"
            );
        }
        
        if (password.length() > MAX_LENGTH) {
            throw new ValidationException(
                "La contraseña es demasiado larga (máx " + MAX_LENGTH + ")", 
                "password"
            );
        }
        
        // Validar que contenga al menos una letra y un número
        if (!password.matches(".*[A-Za-z].*")) {
            throw new ValidationException(
                "La contraseña debe contener al menos una letra", 
                "password"
            );
        }
        
        if (!password.matches(".*\\d.*")) {
            throw new ValidationException(
                "La contraseña debe contener al menos un número", 
                "password"
            );
        }
    }
}

