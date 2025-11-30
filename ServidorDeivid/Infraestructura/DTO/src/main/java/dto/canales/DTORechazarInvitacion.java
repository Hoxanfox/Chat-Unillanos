package dto.canales;

/**
 * DTO para rechazar una invitación a un canal.
 * Usado en la acción: rechazarInvitacion
 */
public class DTORechazarInvitacion {
    private String usuarioId;
    private String canalId;

    public DTORechazarInvitacion() {}

    public DTORechazarInvitacion(String usuarioId, String canalId) {
        this.usuarioId = usuarioId;
        this.canalId = canalId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    @Override
    public String toString() {
        return "DTORechazarInvitacion{" +
                "usuarioId='" + usuarioId + '\'' +
                ", canalId='" + canalId + '\'' +
                '}';
    }
}
