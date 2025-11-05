package com.unillanos.server.validation;

import com.unillanos.server.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validador de emails.
 * Valida formato y longitud de direcciones de email.
 */
@Component
public class EmailValidator {
    
    private static final Pattern EMAIL_PATTERN = 
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MAX_LENGTH = 100;

    /**
     * Valida el formato de un email.
     *
     * @param email Email a validar
     * @return true si el formato es válido
     */
    public boolean isValid(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches() && email.length() <= MAX_LENGTH;
    }

    /**
     * Valida un email y lanza excepción si no es válido.
     *
     * @param email Email a validar
     * @throws ValidationException si el email no es válido
     */
    public static void validate(String email) throws ValidationException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("El email es requerido", "email");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Formato de email inválido", "email");
        }
        
        if (email.length() > MAX_LENGTH) {
            throw new ValidationException("El email es demasiado largo (máx " + MAX_LENGTH + ")", "email");
        }
    }
}

