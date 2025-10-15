package com.unillanos.server.repository.models;

/**
 * Enum que representa los estados posibles de un mensaje.
 */
public enum EstadoMensaje {
    ENVIADO,      // Mensaje guardado en BD
    ENTREGADO,    // Notificación enviada al destinatario
    LEIDO;        // Destinatario confirmó lectura

    /**
     * Convierte un String a EstadoMensaje.
     *
     * @param estado String con el estado
     * @return EstadoMensaje correspondiente
     * @throws IllegalArgumentException si el estado no es válido
     */
    public static EstadoMensaje fromString(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            throw new IllegalArgumentException("Estado no puede ser nulo o vacío");
        }
        
        return switch (estado.toUpperCase().trim()) {
            case "ENVIADO" -> ENVIADO;
            case "ENTREGADO" -> ENTREGADO;
            case "LEIDO" -> LEIDO;
            default -> throw new IllegalArgumentException("Estado de mensaje no válido: " + estado);
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
