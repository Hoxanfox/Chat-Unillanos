package dto.comunicacion.request.canal;

/**
 * Wrapper reorganizado: Request para solicitar historial de un canal.
 * Usa composición para delegar al DTO original.
 */
public class DTOSolicitarHistorialCanal {
    private final dto.comunicacion.peticion.canal.DTOSolicitarHistorialCanal original;

    public DTOSolicitarHistorialCanal(dto.comunicacion.peticion.canal.DTOSolicitarHistorialCanal original) {
        this.original = original;
    }

    public dto.comunicacion.peticion.canal.DTOSolicitarHistorialCanal getOriginal() { return original; }

    // Delegados comunes
    public String getCanalId() { return original.getCanalId(); }
    public String getUsuarioId() { return original.getUsuarioId(); }
    public int getLimite() { return original.getLimite(); }
    public int getOffset() { return original.getOffset(); }
}
