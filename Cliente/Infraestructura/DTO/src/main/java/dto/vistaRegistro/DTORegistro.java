package dto.vistaRegistro;

/**
 * DTO para encapsular los datos necesarios para registrar un nuevo usuario.
 * Este DTO es utilizado por las capas de Negocio y Persistencia.
 */
public final class DTORegistro {
    private final String name;
    private final String email;
    private final String password;
    private final String photoId; // ANTES: photo_base64, AHORA: photoId
    private final String ip;

    public DTORegistro(String name, String email, String password, String photoId, String ip) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.photoId = photoId;
        this.ip = ip;
    }

    // Getters para todos los campos...

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPhotoId() {
        return photoId;
    }

    public String getIp() {
        return ip;
    }
}

