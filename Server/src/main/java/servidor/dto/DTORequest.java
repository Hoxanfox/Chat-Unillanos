package servidor.dto;

import com.google.gson.JsonElement;

/**
 * DTO para encapsular una petición recibida del cliente.
 */
public class DTORequest {
    private String action;
    private JsonElement payload;

    public DTORequest() {}

    public DTORequest(String action, JsonElement payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public JsonElement getPayload() {
        return payload;
    }

    public void setPayload(JsonElement payload) {
        this.payload = payload;
    }
}

