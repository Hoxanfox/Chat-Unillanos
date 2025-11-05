package com.unillanos.server.dto;

/**
 * DTO para cambiar el estado de un usuario (ONLINE, OFFLINE, AWAY).
 */
public class DTOCambiarEstado {
    
    private String userId;       // Requerido
    private String nuevoEstado;  // ONLINE, OFFLINE, AWAY

    public DTOCambiarEstado() {
    }

    public DTOCambiarEstado(String userId, String nuevoEstado) {
        this.userId = userId;
        this.nuevoEstado = nuevoEstado;
    }

    // Getters y Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNuevoEstado() {
        return nuevoEstado;
    }

    public void setNuevoEstado(String nuevoEstado) {
        this.nuevoEstado = nuevoEstado;
    }

    @Override
    public String toString() {
        return "DTOCambiarEstado{" +
                "userId='" + userId + '\'' +
                ", nuevoEstado='" + nuevoEstado + '\'' +
                '}';
    }
}

