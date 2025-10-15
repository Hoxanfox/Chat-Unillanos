package com.unillanos.server.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para notificaciones push del servidor a clientes GUI.
 * Utilizado por el patrón Observer para notificaciones en tiempo real.
 */
public class DTONotificacion {
    
    private String tipo; // LOG, CONEXION, DESCONEXION, MENSAJE_NUEVO, CANAL_NUEVO
    private LocalDateTime timestamp;
    private Map<String, Object> datos;

    public DTONotificacion() {
    }

    public DTONotificacion(String tipo, LocalDateTime timestamp, Map<String, Object> datos) {
        this.tipo = tipo;
        this.timestamp = timestamp;
        this.datos = datos;
    }

    // Getters y Setters
    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getDatos() {
        return datos;
    }

    public void setDatos(Map<String, Object> datos) {
        this.datos = datos;
    }

    @Override
    public String toString() {
        return "DTONotificacion{" +
                "tipo='" + tipo + '\'' +
                ", timestamp=" + timestamp +
                ", datos=" + datos +
                '}';
    }
}
