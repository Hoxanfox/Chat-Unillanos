package dto.usuario;

import java.io.Serializable;

/**
 * DTO para actualizar datos de un usuario existente
 */
public class DTOActualizarUsuario implements Serializable {

    private String id; // UUID del usuario a actualizar
    private String nombre;
    private String email;
    private String foto;
    private String contrasena; // Solo si se desea cambiar
    private String estado; // "ONLINE" o "OFFLINE"

    public DTOActualizarUsuario() {
    }

    public DTOActualizarUsuario(String id, String nombre, String email, String foto,
                                String contrasena, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.foto = foto;
        this.contrasena = contrasena;
        this.estado = estado;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "DTOActualizarUsuario{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}

