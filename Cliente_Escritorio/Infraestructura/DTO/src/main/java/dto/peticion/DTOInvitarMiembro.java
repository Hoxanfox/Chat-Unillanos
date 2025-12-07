package dto.peticion;

/**
 * DTO para encapsular los datos de la petición de invitación de un miembro a un canal.
 * Corresponde a la acción "invitarmiembro" o "invitarMiembro".
 */
public class DTOInvitarMiembro {
    private final String channelId;
    private final String userIdToInvite;

    /**
     * Constructor para la invitación de un miembro.
     *
     * @param channelId      UUID del canal al que se está invitando.
     * @param userIdToInvite UUID del usuario que recibirá la invitación.
     */
    public DTOInvitarMiembro(String channelId, String userIdToInvite) {
        this.channelId = channelId;
        this.userIdToInvite = userIdToInvite;
    }

    // Getters para la serialización a JSON
    public String getChannelId() {
        return channelId;
    }

    public String getUserIdToInvite() {
        return userIdToInvite;
    }
}

