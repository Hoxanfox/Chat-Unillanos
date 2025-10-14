package com.unillanos.server.validation;

import com.unillanos.server.exception.ValidationException;

/**
 * Validador de nombres de canales.
 * Valida longitud y caracteres permitidos.
 */
public class NombreCanalValidator {
    
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;
    
    /**
     * Valida un nombre de canal y lanza excepción si no cumple los requisitos.
     *
     * @param nombre Nombre del canal a validar
     * @throws ValidationException si el nombre no es válido
     */
    public static void validate(String nombre) throws ValidationException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ValidationException("El nombre del canal es requerido", "nombre");
        }
        
        String nombreTrim = nombre.trim();
        
        if (nombreTrim.length() < MIN_LENGTH) {
            throw new ValidationException(
                "El nombre del canal debe tener al menos " + MIN_LENGTH + " caracteres", 
                "nombre"
            );
        }
        
        if (nombreTrim.length() > MAX_LENGTH) {
            throw new ValidationException(
                "El nombre del canal es demasiado largo (máx " + MAX_LENGTH + ")", 
                "nombre"
            );
        }
        
        // Validar que no contenga caracteres especiales prohibidos
        // Permite: letras (incluyendo acentos), números, espacios, guiones y guiones bajos
        if (!nombreTrim.matches("^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s\\-_]+$")) {
            throw new ValidationException(
                "El nombre del canal solo puede contener letras, números, espacios, guiones y guiones bajos", 
                "nombre"
            );
        }
    }
}

