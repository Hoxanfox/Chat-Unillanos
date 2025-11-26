package dto.canales;

/**
 * DTO para invitar a un miembro a un canal.
 * Usado en la acción: invitarmiembro
 */
public class DTOInvitarMiembro {
    private String canalId;
    private String contactoId; // ID del usuario a invitar

    public DTOInvitarMiembro() {}

    public DTOInvitarMiembro(String canalId, String contactoId) {
        this.canalId = canalId;
        this.contactoId = contactoId;
    }

    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    public String getContactoId() {
        return contactoId;
    }

    public void setContactoId(String contactoId) {
        this.contactoId = contactoId;
    }

    @Override
    public String toString() {
        return "DTOInvitarMiembro{" +
                "canalId='" + canalId + '\'' +
                ", contactoId='" + contactoId + '\'' +
                '}';
    }
}

