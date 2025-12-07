package dominio;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clase de Dominio que representa la entidad 'Usuario'.
 * Coincide con la tabla 'usuarios' del esquema H2.
 */
public class Usuario {

    private UUID idUsuario;
    private String nombre;
    private String email;
    private String estado; // 'activo', 'inactivo', 'baneado'
    private byte[] foto;
    private String ip;
    private LocalDateTime fechaRegistro;
    private String photoIdServidor; // ID del archivo en el servidor
    private String rutaFotoLocal;   // Ruta local del archivo descargado

    public Usuario() {
    }

    public Usuario(UUID idUsuario, String nombre, String email, String estado,
                   byte[] foto, String ip, LocalDateTime fechaRegistro, String photoIdServidor) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.email = email;
        this.estado = estado;
        this.foto = foto;
        this.ip = ip;
        this.fechaRegistro = fechaRegistro;
        this.photoIdServidor = photoIdServidor;
    }

    // Getters y Setters
    public UUID getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(UUID idUsuario) {
        this.idUsuario = idUsuario;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public byte[] getFoto() {
        return foto;
    }

    public void setFoto(byte[] foto) {
        this.foto = foto;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getPhotoIdServidor() {
        return photoIdServidor;
    }

    public void setPhotoIdServidor(String photoIdServidor) {
        this.photoIdServidor = photoIdServidor;
    }

    public String getRutaFotoLocal() {
        return rutaFotoLocal;
    }

    public void setRutaFotoLocal(String rutaFotoLocal) {
        this.rutaFotoLocal = rutaFotoLocal;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "idUsuario=" + idUsuario +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", estado='" + estado + '\'' +
                ", ip='" + ip + '\'' +
                ", fechaRegistro=" + fechaRegistro +
                ", photoIdServidor='" + photoIdServidor + '\'' +
                ", rutaFotoLocal='" + rutaFotoLocal + '\'' +
                '}';
    }
}
