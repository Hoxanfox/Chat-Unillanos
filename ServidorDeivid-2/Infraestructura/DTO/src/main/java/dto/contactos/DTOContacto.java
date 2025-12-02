package dto.contactos;

/**
 * DTO para representar un contacto en la lista de contactos del cliente.
 * Estructura esperada por el cliente en GestionContactosImpl.
 */
public class DTOContacto {
    private String id;
    private String nombre;
    private String email;
    private String estado;  // "ONLINE", "OFFLINE"
    private String photoId;
    private String peerId;
    private String fechaRegistro;

    public DTOContacto() {}

    public DTOContacto(String id, String nombre, String email, String estado, String photoId, String peerId, String fechaRegistro) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.estado = estado;
        this.photoId = photoId;
        this.peerId = peerId;
        this.fechaRegistro = fechaRegistro;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getPhotoId() { return photoId; }
    public void setPhotoId(String photoId) { this.photoId = photoId; }

    public String getPeerId() { return peerId; }
    public void setPeerId(String peerId) { this.peerId = peerId; }

    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}

