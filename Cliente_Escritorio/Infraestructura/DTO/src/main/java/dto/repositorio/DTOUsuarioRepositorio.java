package dto.repositorio;

import java.util.Date;
import java.util.UUID;

/**
 * DTO para transferir los datos de un usuario a la capa de Repositorio.
 */
public final class DTOUsuarioRepositorio {
    private final UUID userId;
    private final String name;
    private final String email;
    private final String password;
    private final byte[] fotoBytes;
    private final String photoId; // Campo añadido
    private final String ip;
    private final Date fechaRegistro;

    public DTOUsuarioRepositorio(UUID userId, String name, String email, String password, byte[] fotoBytes, String photoId, String ip, Date fechaRegistro) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.fotoBytes = fotoBytes;
        this.photoId = photoId;
        this.ip = ip;
        this.fechaRegistro = fechaRegistro;
    }

    // Getters
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public byte[] getFotoBytes() { return fotoBytes; }
    public String getPhotoId() { return photoId; }
    public String getIp() { return ip; }
    public Date getFechaRegistro() { return fechaRegistro; }
}

