package dto.p2p;

/**
 * DTO que representa un Peer en las respuestas/paquetes de P2P.
 */
public final class DTOPeer {
    private String id; // uuid string
    private String ip;
    private String socketInfo;
    private String estado; // "ONLINE" | "OFFLINE"
    private String fechaCreacion; // ISO-8601 string

    public DTOPeer() {}

    public String getId() { return id; }
    public String getIp() { return ip; }
    public String getSocketInfo() { return socketInfo; }
    public String getEstado() { return estado; }
    public String getFechaCreacion() { return fechaCreacion; }

    public void setId(String id) { this.id = id; }
    public void setIp(String ip) { this.ip = ip; }
    public void setSocketInfo(String socketInfo) { this.socketInfo = socketInfo; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}

