package dto.p2p;

/**
 * DTO para representar la sección `data` de la petición peer.join
 */
public final class DTOJoinRequest {
    private String requestId;
    private String ip;
    private Integer port;
    private String socketInfo;

    public DTOJoinRequest() {}

    public String getRequestId() { return requestId; }
    public String getIp() { return ip; }
    public Integer getPort() { return port; }
    public String getSocketInfo() { return socketInfo; }

    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setIp(String ip) { this.ip = ip; }
    public void setPort(Integer port) { this.port = port; }
    public void setSocketInfo(String socketInfo) { this.socketInfo = socketInfo; }
}

