package dto.p2p;

/**
 * Representación completa de un Peer para ser enviada en listas.
 * DTO: Data Transfer Object (Estructura inmutable de datos).
 */
public class DTOPeerDetails {
    private String id;
    private String ip;
    private int puerto;
    private String estado;      // "ONLINE", "OFFLINE"
    private String fechaCreacion;

    public DTOPeerDetails(String id, String ip, int puerto, String estado, String fechaCreacion) {
        this.id = id;
        this.ip = ip;
        this.puerto = puerto;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    public String getId() { return id; }
    public String getIp() { return ip; }
    public int getPuerto() { return puerto; }
    public String getEstado() { return estado; }
    public String getFechaCreacion() { return fechaCreacion; }
}