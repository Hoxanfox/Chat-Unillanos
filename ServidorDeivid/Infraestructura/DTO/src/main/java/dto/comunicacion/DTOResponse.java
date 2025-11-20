package dto.comunicacion;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public final class DTOResponse {
    private final String action;
    private final String status;
    private final String message;

    @SerializedName(value = "data", alternate = {"payload"})
    private final JsonElement data; // Usamos JsonElement aquí también

    public DTOResponse(String action, String status, String message, JsonElement data) {
        this.action = action;
        this.status = status;
        this.message = message;
        this.data = data; // Corregido
    }

    public String getAction() { return action; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public JsonElement getData() { return data; }

    public boolean fueExitoso() {
        return "success".equalsIgnoreCase(status);
    }
}