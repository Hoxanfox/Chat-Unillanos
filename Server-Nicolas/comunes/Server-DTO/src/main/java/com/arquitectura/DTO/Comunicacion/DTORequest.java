package com.arquitectura.DTO.Comunicacion;

public final class DTORequest {
    private final String action;
    private final Object payload;
    public DTORequest(String action, Object payload) {
        this.action = action;
        this.payload = payload;
    }
    public String getAction() {
        return action;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "DTORequest{" +
                "action='" + action + '\'' +
                ", payload=" + payload +
                '}';
    }
}
