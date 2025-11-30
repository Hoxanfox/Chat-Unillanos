package dto.p2p;

/**
 * Usado para identificarse al intentar unirse (Input).
 */
public class DTOPeerIdentity {
    private String ip;
    private int puerto;

    public DTOPeerIdentity(String ip, int puerto) {
        this.ip = ip;
        this.puerto = puerto;
    }

    public String getIp() { return ip; }
    public int getPuerto() { return puerto; }
}
