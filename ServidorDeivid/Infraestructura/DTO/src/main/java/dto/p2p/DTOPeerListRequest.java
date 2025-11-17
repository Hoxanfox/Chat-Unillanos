package dto.p2p;

/**
 * DTO para la petición de lista de peers (peer.list)
 */
public final class DTOPeerListRequest {
    private String requestId;
    private String ip;
    private Integer port;

    public DTOPeerListRequest() {}

    public String getRequestId() { return requestId; }
    public String getIp() { return ip; }
    public Integer getPort() { return port; }

    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setIp(String ip) { this.ip = ip; }
    public void setPort(Integer port) { this.port = port; }
}

