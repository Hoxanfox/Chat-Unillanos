package dto.comunicacion.request.canal;

/** Wrapper reorganizado: Request invitar miembro. Usa composición. */
public class DTOInvitarMiembro {
    private final dto.peticion.DTOInvitarMiembro original;

    public DTOInvitarMiembro(dto.peticion.DTOInvitarMiembro original) { this.original = original; }
    public dto.peticion.DTOInvitarMiembro getOriginal() { return original; }

    // Delegar llamando a los getters reales del DTO original
    public String getChannelId() { return original.getChannelId(); }
    public String getUserIdToInvite() { return original.getUserIdToInvite(); }
}
