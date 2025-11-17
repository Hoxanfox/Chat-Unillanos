package dto.p2p;

/**
 * DTO para notificación de nuevo peer (push).
 */
public final class DTOPeerPush {
    private String id; // uuid opcional
    private String ip;
    private int port;
    private String socketInfo;
    private String timestamp;

    public DTOPeerPush() {}

    public String getId() { return id; }
    public String getIp() { return ip; }
    public int getPort() { return port; }
    public String getSocketInfo() { return socketInfo; }
    public String getTimestamp() { return timestamp; }

    public void setId(String id) { this.id = id; }
    public void setIp(String ip) { this.ip = ip; }
    public void setPort(int port) { this.port = port; }
    public void setSocketInfo(String socketInfo) { this.socketInfo = socketInfo; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}

