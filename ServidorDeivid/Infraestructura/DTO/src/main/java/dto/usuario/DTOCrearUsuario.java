package dto.usuario;

import java.io.Serializable;

/**
 * DTO para crear un nuevo usuario
 * Contiene los datos necesarios para registrar un usuario en el sistema
 */
public class DTOCrearUsuario implements Serializable {

    private String nombre;
    private String email;
    private String contrasena;
    private String foto; // URL o path de la foto (opcional)
    private String peerPadreId; // ID del peer al que pertenece

    public DTOCrearUsuario() {
    }

    public DTOCrearUsuario(String nombre, String email, String contrasena, String foto, String peerPadreId) {
        this.nombre = nombre;
        this.email = email;
        this.contrasena = contrasena;
        this.foto = foto;
        this.peerPadreId = peerPadreId;
    }

    // Getters y Setters
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

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getPeerPadreId() {
        return peerPadreId;
    }

    public void setPeerPadreId(String peerPadreId) {
        this.peerPadreId = peerPadreId;
    }

    @Override
    public String toString() {
        return "DTOCrearUsuario{" +
                "nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", peerPadreId='" + peerPadreId + '\'' +
                '}';
    }
}