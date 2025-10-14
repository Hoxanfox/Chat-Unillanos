package com.unillanos.server.validation;

import com.unillanos.server.exception.ValidationException;

/**
 * Validador de contraseñas.
 * Valida longitud y fortaleza (debe contener al menos una letra y un número).
 */
public class PasswordValidator {
    
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 100;
    
    /**
     * Valida una contraseña y lanza excepción si no cumple los requisitos.
     *
     * @param password Contraseña a validar
     * @throws ValidationException si la contraseña no es válida
     */
    public static void validate(String password) throws ValidationException {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("La contraseña es requerida", "password");
        }
        
        if (password.length() < MIN_LENGTH) {
            throw new ValidationException(
                "La contraseña debe tener al menos " + MIN_LENGTH + " caracteres", 
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

