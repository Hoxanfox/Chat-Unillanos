package dto.vistaLobby;

/**
 * DTO para solicitar el cierre de sesión de un usuario.
 */
public class DTOLogout {
    private final String usuarioId;

    public DTOLogout(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }
}

