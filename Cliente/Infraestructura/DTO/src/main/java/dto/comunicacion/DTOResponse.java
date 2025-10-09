package dto.comunicacion;

/**
 * DTO para encapsular una respuesta recibida del servidor.
 * Este objeto será deserializado (ej. desde JSON) al ser recibido.
 */
public final class DTOResponse {
    private final String action; // Acción de la petición original
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

    /**
     * Método de utilidad para verificar rápidamente si la operación fue exitosa.
     * @return true si el estado es "success", false en caso contrario.
     */
    public boolean fueExitoso() {
        return "success".equalsIgnoreCase(status);
    }
}

