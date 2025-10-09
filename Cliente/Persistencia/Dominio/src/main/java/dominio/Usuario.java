package dominio;

import java.util.Date;
import java.util.UUID;

/**
 * Clase de Dominio que representa la entidad 'Usuario'.
 * Contiene tanto los bytes de la foto para uso local como el ID del servidor.
 */
public class Usuario {

    private final UUID idUsuario;
    private final String nombre;
    private final String email;
    private final String password; // En una app real, esto sería un hash
    private final byte[] foto;       // Para caché y visualización local rápida.
    private final String photoId;    // El identificador del archivo en el servidor.
    private final String ip;
    private final Date fechaRegistro;

    public Usuario(UUID idUsuario, String nombre, String email, String password, byte[] foto, String photoId, String ip, Date fechaRegistro) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.foto = foto;
        this.photoId = photoId;
        this.ip = ip;
        this.fechaRegistro = fechaRegistro;
    }

    // Getters para todos los campos

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public byte[] getFoto() {
        return foto;
    }

    public String getPhotoId() {
        return photoId;
    }

    public String getIp() {
        return ip;
    }

    public Date getFechaRegistro() {
        return fechaRegistro;
    }
}

