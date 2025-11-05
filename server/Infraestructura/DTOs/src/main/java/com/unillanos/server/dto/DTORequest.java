package com.unillanos.server.dto;

/**
 * DTO base para las peticiones del cliente al servidor.
 * Contiene el tipo de acción y el payload con los datos específicos.
 */
public class DTORequest {
    private String action;
    private Object payload;

    public DTORequest() {
    }

    public DTORequest(String action, Object payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "DTORequest{" +
                "action='" + action + '\'' +
                ", payload=" + payload +
                '}';
    }
}

