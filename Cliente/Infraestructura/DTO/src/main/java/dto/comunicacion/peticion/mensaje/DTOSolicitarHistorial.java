package dto.comunicacion.peticion.mensaje;

/**
 * DTO para el payload de la petición 'solicitarHistorialPrivado'.
 * Incluye el usuarioId del usuario activo y el contactoId del chat.
 */
public class DTOSolicitarHistorial {
    private final String usuarioId;   // UUID del usuario activo (quien solicita)
    private final String contactoId;  // UUID del contacto del chat

    public DTOSolicitarHistorial(String usuarioId, String contactoId) {
        this.usuarioId = usuarioId;
        this.contactoId = contactoId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public String getContactoId() {
        return contactoId;
    }

    @Override
    public String toString() {
        return "DTOSolicitarHistorial{" +
                "usuarioId='" + usuarioId + '\'' +
                ", contactoId='" + contactoId + '\'' +
                '}';
    }
}
