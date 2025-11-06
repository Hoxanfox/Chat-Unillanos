package dominio;

import java.util.UUID;

/**
 * Entidad de Dominio: Contacto
 * Representa un contacto en el sistema.
 */
public class Contacto {
    private UUID idContacto;
    private String nombre;
    private String email;
    private boolean estado;
    private String photoId;        // ID de la foto en el servidor
    private String peerId;         // ID del peer WebRTC
    private String fechaRegistro;  // Fecha de registro del contacto

    public Contacto() {
    }

    public Contacto(UUID idContacto, String nombre, boolean estado) {
        this.idContacto = idContacto;
        this.nombre = nombre;
        this.estado = estado;
    }

    public Contacto(UUID idContacto, String nombre, String email, boolean estado,
                   String photoId, String peerId, String fechaRegistro) {
        this.idContacto = idContacto;
        this.nombre = nombre;
        this.email = email;
        this.estado = estado;
        this.photoId = photoId;
        this.peerId = peerId;
        this.fechaRegistro = fechaRegistro;
    }

    // Getters y Setters
    public UUID getIdContacto() {
        return idContacto;
    }

    public void setIdContacto(UUID idContacto) {
        this.idContacto = idContacto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    @Override
    public String toString() {
        return "Contacto{" +
                "idContacto=" + idContacto +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", estado=" + estado +
                ", photoId='" + photoId + '\'' +
                ", peerId='" + peerId + '\'' +
                ", fechaRegistro='" + fechaRegistro + '\'' +
                '}';
    }
}
