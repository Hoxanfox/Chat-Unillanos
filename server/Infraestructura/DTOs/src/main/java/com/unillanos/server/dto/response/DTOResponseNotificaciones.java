package com.unillanos.server.dto.response;

import java.util.List;

/**
 * DTO para la respuesta de notificaciones.
 * Estructura que espera recibir el cliente.
 */
public class DTOResponseNotificaciones {
    private List<DTONotificacionResponse> notificaciones;
    private int totalNoLeidas;
    private int totalNotificaciones;

    public DTOResponseNotificaciones() {}

    public DTOResponseNotificaciones(List<DTONotificacionResponse> notificaciones, int totalNoLeidas, int totalNotificaciones) {
        this.notificaciones = notificaciones;
        this.totalNoLeidas = totalNoLeidas;
        this.totalNotificaciones = totalNotificaciones;
    }

    // Getters y Setters
    public List<DTONotificacionResponse> getNotificaciones() {
        return notificaciones;
    }

    public void setNotificaciones(List<DTONotificacionResponse> notificaciones) {
        this.notificaciones = notificaciones;
    }

    public int getTotalNoLeidas() {
        return totalNoLeidas;
    }

    public void setTotalNoLeidas(int totalNoLeidas) {
        this.totalNoLeidas = totalNoLeidas;
    }

    public int getTotalNotificaciones() {
        return totalNotificaciones;
    }

    public void setTotalNotificaciones(int totalNotificaciones) {
        this.totalNotificaciones = totalNotificaciones;
    }
}
