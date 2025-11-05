package com.unillanos.server.dto;

/**
 * DTO base para las respuestas del servidor al cliente.
 * Contiene el estado de la operación, mensaje descriptivo y datos de respuesta.
 */
public class DTOResponse {
    private String action;
    private String status; // "success" | "error"
    private String message;
    private Object data;

    public DTOResponse() {
    }

    public DTOResponse(String action, String status, String message, Object data) {
        this.action = action;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Métodos de conveniencia
    public static DTOResponse success(String action, String message, Object data) {
        return new DTOResponse(action, "success", message, data);
    }

    public static DTOResponse error(String action, String message) {
        return new DTOResponse(action, "error", message, null);
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "DTOResponse{" +
                "action='" + action + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}

