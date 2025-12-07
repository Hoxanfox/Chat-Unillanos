package dto.vista;

/**
 * DTO para representar un canal en la vista de administración
 */
public class DTOCanalVista {
    private String id;
    private String nombre;
    private String tipo;
    private String creadorId;
    private String creadorNombre;
    private int numeroMiembros;
    private String fechaCreacion;
    private String peerPadreId;

    public DTOCanalVista() {}

    public DTOCanalVista(String id, String nombre, String tipo, String creadorId, 
                         String creadorNombre, int numeroMiembros, String fechaCreacion, 
                         String peerPadreId) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.creadorId = creadorId;
        this.creadorNombre = creadorNombre;
        this.numeroMiembros = numeroMiembros;
        this.fechaCreacion = fechaCreacion;
        this.peerPadreId = peerPadreId;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getCreadorId() { return creadorId; }
    public void setCreadorId(String creadorId) { this.creadorId = creadorId; }

    public String getCreadorNombre() { return creadorNombre; }
    public void setCreadorNombre(String creadorNombre) { this.creadorNombre = creadorNombre; }

    public int getNumeroMiembros() { return numeroMiembros; }
    public void setNumeroMiembros(int numeroMiembros) { this.numeroMiembros = numeroMiembros; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getPeerPadreId() { return peerPadreId; }
    public void setPeerPadreId(String peerPadreId) { this.peerPadreId = peerPadreId; }
}

