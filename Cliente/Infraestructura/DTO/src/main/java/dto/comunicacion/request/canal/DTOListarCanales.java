package dto.comunicacion.request.canal;

/** Wrapper reorganizado: Request listar canales. Usa composición. */
public class DTOListarCanales {
    private final dto.peticion.DTOListarCanales original;

    public DTOListarCanales(dto.peticion.DTOListarCanales original) { this.original = original; }
    public dto.peticion.DTOListarCanales getOriginal() { return original; }

    public String getUsuarioId() { return original.getUsuarioId(); }

    // Delegar a los getters reales del DTO original (ingles: limit/offset)
    public int getLimit() { return original.getLimit(); }
    public int getOffset() { return original.getOffset(); }

    // Métodos de compatibilidad en español para código existente que use getLimite()
    public int getLimite() { return getLimit(); }
}
