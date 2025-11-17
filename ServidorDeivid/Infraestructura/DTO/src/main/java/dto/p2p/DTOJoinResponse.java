package dto.p2p;

/**
 * DTO para representar la sección `data` de la respuesta a una petición peer.join
 */
public final class DTOJoinResponse {
    private String requestId;
    private String id; // UUID asignado al peer (puede venir como "id" o "uuid")
    private String uuid;

    public DTOJoinResponse() {}

    public String getRequestId() { return requestId; }
    public String getId() { return id; }
    public String getUuid() { return uuid; }

    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setId(String id) { this.id = id; }
    public void setUuid(String uuid) { this.uuid = uuid; }
}

