package dto.p2p;

public class DTOPeerDetails {
    private String id;
    private String ip;
    private int puerto; // Puerto Físico (Canal Netty)
    private int puertoServidor; // NUEVO: Puerto Lógico (Donde escucha el peer)
    private String estado;
    private String fechaCreacion;

    public DTOPeerDetails(String id, String ip, int puerto, String estado, String fechaCreacion) {
        this.id = id;
        this.ip = ip;
        this.puerto = puerto;
        this.puertoServidor = puerto; // Por defecto igual al físico hasta que se identifique
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    public void setPuertoServidor(int puertoServidor) {
        this.puertoServidor = puertoServidor;
    }

    public int getPuertoServidor() {
        return puertoServidor;
    }

    public String getId() { return id; }
    public String getIp() { return ip; }
    public int getPuerto() { return puerto; }
    public String getEstado() { return estado; }
    public String getFechaCreacion() { return fechaCreacion; }
}