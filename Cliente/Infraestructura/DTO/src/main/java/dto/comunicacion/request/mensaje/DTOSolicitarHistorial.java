package dto.comunicacion.request.mensaje;

/**
 * Wrapper reorganizado: Request para solicitar historial de mensajes.
 * Usa composición para mantener compatibilidad con la clase original.
 */
public class DTOSolicitarHistorial {
    private final dto.comunicacion.peticion.mensaje.DTOSolicitarHistorial original;

    public DTOSolicitarHistorial(dto.comunicacion.peticion.mensaje.DTOSolicitarHistorial original) {
        this.original = original;
    }

    public dto.comunicacion.peticion.mensaje.DTOSolicitarHistorial getOriginal() {
        return original;
    }

    // Delegar getters
    public String getRemitenteId() { return original.getRemitenteId(); }
    public String getPeerRemitenteId() { return original.getPeerRemitenteId(); }
    public String getDestinatarioId() { return original.getDestinatarioId(); }
    public String getPeerDestinatarioId() { return original.getPeerDestinatarioId(); }

}
