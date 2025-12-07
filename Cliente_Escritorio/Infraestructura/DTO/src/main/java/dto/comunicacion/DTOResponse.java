package dto.comunicacion;

/**
 * DTO para encapsular una respuesta recibida del servidor.
 * Este objeto será deserializado (ej. desde JSON) al ser recibido.
 * Soporta tanto el campo 'action' como 'type' para diferentes tipos de mensajes.
 */
public final class DTOResponse {
    private final String action; // Acción de la petición original
    private final String type;   // Tipo de señal (ej. SIGNAL_UPDATE)
    private final String resource; // Recurso asociado (ej. USUARIO_ONLINE)
    private final String status;
    private final String message;
    private final Object data;

    // Constructor completo
    public DTOResponse(String action, String type, String resource, String status, String message, Object data) {
        this.action = action;
        this.type = type;
        this.resource = resource;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Constructor de compatibilidad con versión anterior
    public DTOResponse(String action, String status, String message, Object data) {
        this(action, null, null, status, message, data);
    }

    public String getAction() {
        return action;
    }

    public String getType() {
        return type;
    }

    public String getResource() {
        return resource;
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
     * Obtiene el identificador de la respuesta, que puede ser action o type.
     * @return action si existe, sino type, sino null
     */
    public String getIdentificador() {
        if (action != null && !action.isEmpty()) {
            return action;
        }
        if (type != null && !type.isEmpty()) {
            return type;
        }
        return null;
    }

    /**
     * Método de utilidad para verificar rápidamente si la operación fue exitosa.
     * @return true si el estado es "success", false en caso contrario.
     */
    public boolean fueExitoso() {
        return "success".equalsIgnoreCase(status);
    }
}
