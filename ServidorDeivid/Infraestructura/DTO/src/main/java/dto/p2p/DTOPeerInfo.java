package dto.p2p;

public class DTOPeerInfo {
    private String id;
    private String ip;
    private int puerto;
    private String estado;        // "OFFLINE", "ONLINE"
    private double fechaCreacion; // Timestamp
    private String fechaRegistro; // String formateado

    // Constructor vacío (necesario para algunas librerías de serialización como Gson/Jackson)
    public DTOPeerInfo() {
    }

    // Constructor completo
    public DTOPeerInfo(String id, String ip, int puerto, String estado, double fechaCreacion, String fechaRegistro) {
        this.id = id;
        this.ip = ip;
        this.puerto = puerto;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaRegistro = fechaRegistro;
    }

    // ==========================================
    // GETTERS (Necesarios para leer los datos)
    // ==========================================

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPuerto() {
        return puerto;
    }

    public String getEstado() {
        return estado;
    }

    public double getFechaCreacion() {
        return fechaCreacion;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    // ==========================================
    // SETTERS (Opcionales, pero útiles)
    // ==========================================

    public void setId(String id) {
        this.id = id;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPuerto(int puerto) {
        this.puerto = puerto;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setFechaCreacion(double fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}