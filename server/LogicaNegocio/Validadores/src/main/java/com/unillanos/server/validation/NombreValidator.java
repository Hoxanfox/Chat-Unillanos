package com.unillanos.server.validation;

import com.unillanos.server.exception.ValidationException;

/**
 * Validador de nombres de usuario.
 * Valida longitud del nombre.
 */
public class NombreValidator {
    
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;
    
    /**
     * Valida un nombre y lanza excepción si no cumple los requisitos.
     *
     * @param nombre Nombre a validar
     * @throws ValidationException si el nombre no es válido
     */
    public static void validate(String nombre) throws ValidationException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ValidationException("El nombre es requerido", "nombre");
        }
        
        String nombreTrim = nombre.trim();
        
        if (nombreTrim.length() < MIN_LENGTH) {
            throw new ValidationException(
                "El nombre debe tener al menos " + MIN_LENGTH + " caracteres", 
                "nombre"
            );
        }
        
        if (nombreTrim.length() > MAX_LENGTH) {
            throw new ValidationException(
                "El nombre es demasiado largo (máx " + MAX_LENGTH + ")", 
                "nombre"
            );
        }
    }
}

