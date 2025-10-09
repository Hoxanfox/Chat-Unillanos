package servidor.dto;

/**
 * DTO para encapsular una respuesta enviada al cliente.
 */
public class DTOResponse {
    private String action;
    private String status;
    private String message;
    private Object data;

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
}

