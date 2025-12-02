package dto.canales;

import com.google.gson.annotations.SerializedName;

/**
 * DTO para invitar a un miembro a un canal.
 * Usado en la acción: invitarmiembro
 */
public class DTOInvitarMiembro {
    @SerializedName(value = "canalId", alternate = {"channelId"})
    private String canalId;

    @SerializedName(value = "contactoId", alternate = {"userIdToInvite"})
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
