package com.unillanos.server.repository.models;

/**
 * Enum que representa el tipo de mensaje.
 */
public enum TipoMensaje {
    DIRECT,   // Mensaje directo (usuario a usuario)
    CHANNEL;  // Mensaje de canal (usuario a grupo)
    
    /**
     * Convierte un String a TipoMensaje.
     * 
     * @param tipo String a convertir
     * @return TipoMensaje correspondiente, o DIRECT por defecto
     */
    public static TipoMensaje fromString(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return DIRECT; // Por defecto
        }
        
        try {
            return TipoMensaje.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DIRECT;
        }
    }
}

