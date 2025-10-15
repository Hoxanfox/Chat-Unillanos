package com.unillanos.server.dto;

import java.util.List;

/**
 * DTO para suscribirse a notificaciones push del servidor.
 */
public class DTOSuscribirNotificaciones {
    
    private String clienteId;
    private List<String> tiposInteres; // ["LOG", "CONEXION", etc.]

    public DTOSuscribirNotificaciones() {
    }

    public DTOSuscribirNotificaciones(String clienteId, List<String> tiposInteres) {
        this.clienteId = clienteId;
        this.tiposInteres = tiposInteres;
    }

    // Getters y Setters
    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public List<String> getTiposInteres() {
        return tiposInteres;
    }

    public void setTiposInteres(List<String> tiposInteres) {
        this.tiposInteres = tiposInteres;
    }

    @Override
    public String toString() {
        return "DTOSuscribirNotificaciones{" +
                "clienteId='" + clienteId + '\'' +
                ", tiposInteres=" + tiposInteres +
                '}';
    }
}
