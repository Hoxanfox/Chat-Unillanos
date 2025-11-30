package dto.canales;

/**
 * DTO para que un usuario se una a un canal (aceptar invitación).
 * Usado en la acción: unirseCanal
 */
public class DTOUnirseCanal {
    private String usuarioId;
    private String canalId;

    public DTOUnirseCanal() {}

    public DTOUnirseCanal(String usuarioId, String canalId) {
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
        return "DTOUnirseCanal{" +
                "usuarioId='" + usuarioId + '\'' +
                ", canalId='" + canalId + '\'' +
                '}';
    }
}

