package dto.repositorio;

/**
 * DTO que representa la entidad de un usuario como se almacena
 * en la capa de persistencia local (Repositorio).
 */
public final class DTOUsuario {
    private final String userId;
    private final String name;
    private final String email;
    private final String photoId;

    public DTOUsuario(String userId, String name, String email, String photoId) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.photoId = photoId;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhotoId() { return photoId; }
}
