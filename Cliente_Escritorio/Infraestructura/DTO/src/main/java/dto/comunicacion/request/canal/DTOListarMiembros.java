package dto.comunicacion.request.canal;

/** Wrapper reorganizado: Request listar miembros. Usa composición. */
public class DTOListarMiembros {
    private final dto.peticion.DTOListarMiembros original;

    public DTOListarMiembros(dto.peticion.DTOListarMiembros original) { this.original = original; }
    public dto.peticion.DTOListarMiembros getOriginal() { return original; }

    // Delegados correctos según DTO original
    public String getCanalId() { return original.getCanalId(); }
    public String getSolicitanteId() { return original.getSolicitanteId(); }
}
