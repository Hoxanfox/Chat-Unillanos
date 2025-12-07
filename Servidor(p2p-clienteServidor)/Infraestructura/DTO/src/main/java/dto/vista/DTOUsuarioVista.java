package dto.vista;

import java.io.Serializable;

/**
 * DTO para representar un usuario en la vista de administración
 * Incluye información del usuario y su peer padre
 */
public class DTOUsuarioVista implements Serializable {

    private String id;
    private String nombre;
    private String email;
    private String estado; // "ONLINE" o "OFFLINE"
    private String fechaCreacion;
    private String peerPadreId; // UUID del peer al que pertenece

    public DTOUsuarioVista() {
    }

    public DTOUsuarioVista(String id, String nombre, String email, String estado,
                          String fechaCreacion, String peerPadreId) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.peerPadreId = peerPadreId;
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

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getPeerPadreId() { return peerPadreId; }
    public void setPeerPadreId(String peerPadreId) { this.peerPadreId = peerPadreId; }
}

