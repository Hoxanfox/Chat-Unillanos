package com.unillanos.server.entity;

/**
 * Enum que representa los estados posibles de una sesión de chunking.
 */
public enum EstadoSesion {
    ACTIVA,      // Sesión activa, recibiendo chunks
    COMPLETADA,  // Todos los chunks recibidos y archivo ensamblado
    EXPIRADA,    // Sesión expirada por inactividad
    CANCELADA;   // Sesión cancelada por el usuario o error

    /**
     * Convierte un String a EstadoSesion.
     *
     * @param estado String con el estado
     * @return EstadoSesion correspondiente
     * @throws IllegalArgumentException si el estado no es válido
     */
    public static EstadoSesion fromString(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            throw new IllegalArgumentException("Estado no puede ser nulo o vacío");
        }
        
        return switch (estado.toUpperCase().trim()) {
            case "ACTIVA" -> ACTIVA;
            case "COMPLETADA" -> COMPLETADA;
            case "EXPIRADA" -> EXPIRADA;
            case "CANCELADA" -> CANCELADA;
            default -> throw new IllegalArgumentException("Estado de sesión no válido: " + estado);
        };
    }

    /**
     * Convierte el enum a String para almacenamiento en base de datos.
     *
     * @return String con el nombre del estado
     */
    @Override
    public String toString() {
        return this.name();
    }
}
