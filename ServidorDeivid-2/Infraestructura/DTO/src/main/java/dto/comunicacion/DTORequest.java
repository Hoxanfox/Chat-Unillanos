package dto.comunicacion;

import com.google.gson.JsonElement;

public final class DTORequest {
    private final String action;
    private final JsonElement payload;

    public DTORequest(String action, JsonElement payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() { return action; }
    public JsonElement getPayload() { return payload; }
}