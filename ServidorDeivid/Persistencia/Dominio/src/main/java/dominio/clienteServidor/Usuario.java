package dominio.clienteServidor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String nombre;
    private String email;
    private String foto; // puede ser URL o ruta
    private UUID peerPadre;
    private String contrasena;
    private String ip;

    public enum Estado { OFFLINE, ONLINE }

    private Estado estado = Estado.OFFLINE;
    private Instant fechaCreacion;

    public Usuario() {
        this.id = UUID.randomUUID();
        this.fechaCreacion = Instant.now();
        this.estado = Estado.OFFLINE;
    }

    public Usuario(UUID id, String nombre, String email, String foto, UUID peerPadre, String contrasena, String ip, Estado estado, Instant fechaCreacion) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.nombre = nombre;
        this.email = email;
        this.foto = foto;
        this.peerPadre = peerPadre;
        this.contrasena = contrasena;
        this.ip = ip;
        this.estado = estado == null ? Estado.OFFLINE : estado;
        this.fechaCreacion = fechaCreacion == null ? Instant.now() : fechaCreacion;
    }

    // Getters y setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public UUID getPeerPadre() {
        return peerPadre;
    }

    public void setPeerPadre(UUID peerPadre) {
        this.peerPadre = peerPadre;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", ip='" + ip + '\'' +
                ", estado=" + estado +
                '}';
    }
}
