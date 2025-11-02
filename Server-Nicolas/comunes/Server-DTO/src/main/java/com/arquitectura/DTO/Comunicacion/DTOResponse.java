package com.arquitectura.DTO.Comunicacion;

public final class DTOResponse {
    private final String action;
    private final String status;
    private final String message;
    private final Object data;
    public DTOResponse(String action, String status, String message, Object data) {
        this.action = action;
        this.status = status;
        this.message = message;
        this.data = data;
    }
    public String getAction() {
        return action;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public boolean fueExitoso() {
        return "success".equalsIgnoreCase(status);
    }


}
