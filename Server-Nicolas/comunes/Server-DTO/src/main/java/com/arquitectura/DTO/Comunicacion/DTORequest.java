package com.arquitectura.DTO.Comunicacion;

public final class DTORequest {
    private final String action;
    private final Object data;

    public DTORequest(String action, Object data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() {
        return action;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return "DTORequest{" +
                "action='" + action + '\'' +
                ", data=" + data +
                '}';
    }
}
