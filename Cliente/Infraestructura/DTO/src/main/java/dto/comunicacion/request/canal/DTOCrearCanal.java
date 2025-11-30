package dto.comunicacion.request.canal;

/** Wrapper reorganizado: Request crear canal. Usa composición. */
public class DTOCrearCanal {
    private final dto.peticion.DTOCrearCanal original;

    public DTOCrearCanal(dto.peticion.DTOCrearCanal original) { this.original = original; }
    public dto.peticion.DTOCrearCanal getOriginal() { return original; }

    public String getNombre() { return original.getNombre(); }
    public String getTipo() { return original.getTipo(); }
}
