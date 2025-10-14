package com.unillanos.server.repository.models;

/**
 * Enum que representa los posibles estados de un usuario.
 */
public enum EstadoUsuario {
    ONLINE,
    OFFLINE,
    AWAY;
    
    /**
     * Convierte un String a EstadoUsuario.
     * Si el String no es válido, retorna OFFLINE por defecto.
     *
     * @param estado String con el nombre del estado
     * @return EstadoUsuario correspondiente o OFFLINE si no es válido
     */
    public static EstadoUsuario fromString(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            return OFFLINE;
        }
        
        try {
            return EstadoUsuario.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OFFLINE; // Por defecto si el valor no es válido
        }
    }
}

