package dto.comunicacion;

/**
 * DTO para encapsular una petición enviada desde el cliente al servidor.
 * Este objeto será serializado (ej. a JSON) antes de ser enviado.
 */
public final class DTORequest {
    private final String action;
    private final Object payload; // Los datos de la petición (ej. un DTOAutenticacion)

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
}

