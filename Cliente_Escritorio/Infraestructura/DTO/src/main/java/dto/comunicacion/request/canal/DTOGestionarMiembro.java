package dto.comunicacion.request.canal;

/** Wrapper reorganizado: Request gestionar miembro. Usa composición. */
public class DTOGestionarMiembro {
    private final dto.peticion.DTOGestionarMiembro original;

    public DTOGestionarMiembro(dto.peticion.DTOGestionarMiembro original) { this.original = original; }
    public dto.peticion.DTOGestionarMiembro getOriginal() { return original; }

    public String getCanalId() { return original.getCanalId(); }
    public String getUsuarioId() { return original.getUsuarioId(); }
    public String getAccion() { return original.getAccion(); }
}
