package controlador.p2p;

/**
 * DTO usado por el controlador para exponer información de peers a la UI.
 */
public class PeerDTO {
    private final String uuid;
    private final String label;
    private final String ip;
    private final int port;
    private final boolean online;

    public PeerDTO(String uuid, String label, String ip, int port, boolean online) {
        this.uuid = uuid;
        this.label = label;
        this.ip = ip;
        this.port = port;
        this.online = online;
    }

    public String getUuid() { return uuid; }
    public String getLabel() { return label; }
    public String getIp() { return ip; }
    public int getPort() { return port; }
    public boolean isOnline() { return online; }
}

