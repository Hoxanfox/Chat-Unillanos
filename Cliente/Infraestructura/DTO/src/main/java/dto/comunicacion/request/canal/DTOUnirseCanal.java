package dto.comunicacion.request.canal;

/** Wrapper reorganizado: Request unirse a canal. Usa composición. */
public class DTOUnirseCanal {
    private final dto.peticion.DTOUnirseCanal original;

    public DTOUnirseCanal(dto.peticion.DTOUnirseCanal original) { this.original = original; }
    public dto.peticion.DTOUnirseCanal getOriginal() { return original; }

    public String getCanalId() { return original.getCanalId(); }
    public String getUsuarioId() { return original.getUsuarioId(); }
}
