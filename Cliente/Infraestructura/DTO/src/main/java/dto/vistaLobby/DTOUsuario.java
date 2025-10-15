package dto.vistaLobby;

/**
 * DTO para transportar información del usuario a la vista del Lobby.
 */
public final class DTOUsuario {
    private final String id;
    private final String nombre;
    private final String email;
    private final String avatarUrl;

    public DTOUsuario(String id, String nombre, String email, String avatarUrl) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean esValido() {
        return id != null && !id.isEmpty() 
            && nombre != null && !nombre.isEmpty() 
            && email != null && !email.isEmpty();
    }

    @Override
    public String toString() {
        return "DTOUsuario{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                '}';
    }
}

