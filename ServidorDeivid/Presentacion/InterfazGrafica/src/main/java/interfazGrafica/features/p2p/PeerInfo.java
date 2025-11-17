package interfazGrafica.features.p2p;

/**
 * Modelo simple que representa un Peer en la red P2P para la UI.
 */
public class PeerInfo {
    private final String uuid;
    private final String label;
    private final String ip;
    private final int port;
    private final boolean online;

    public PeerInfo(String uuid, String label, String ip, int port, boolean online) {
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

    @Override
    public String toString() {
        return "PeerInfo{" +
                "uuid='" + uuid + '\'' +
                ", label='" + label + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", online=" + online +
                '}';
    }
}
