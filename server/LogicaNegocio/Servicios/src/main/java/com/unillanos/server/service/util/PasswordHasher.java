package com.unillanos.server.service.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.unillanos.server.config.ServerConfigProperties;

/**
 * Utilidad para hashear y verificar contraseñas usando BCrypt.
 * BCrypt es un algoritmo de hashing seguro diseñado específicamente para contraseñas.
 * 
 * Factor de trabajo: 12 (balance entre seguridad y performance)
 */
public class PasswordHasher {
    
    private static final BCrypt.Hasher hasher = BCrypt.withDefaults();
    private static final BCrypt.Verifyer verifyer = BCrypt.verifyer();
    
    /**
     * Hashea una contraseña en texto plano usando BCrypt.
     *
     * @param plainPassword Contraseña en texto plano
     * @param config Configuración del servidor
     * @return Hash de la contraseña (empieza con $2a$ o $2b$)
     */
    public static String hash(String plainPassword, ServerConfigProperties config) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede ser nula o vacía");
        }
        return hasher.hashToString(config.getSeguridad().getBcryptStrength(), plainPassword.toCharArray());
    }
    
    /**
     * Verifica si una contraseña en texto plano coincide con un hash.
     *
     * @param plainPassword Contraseña en texto plano
     * @param hashedPassword Hash de la contraseña almacenado en BD
     * @return true si la contraseña coincide con el hash
     */
    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        
        try {
            BCrypt.Result result = verifyer.verify(plainPassword.toCharArray(), hashedPassword);
            return result.verified;
        } catch (Exception e) {
            // Si hay algún error en la verificación, retornar false
            return false;
        }
    }
}

