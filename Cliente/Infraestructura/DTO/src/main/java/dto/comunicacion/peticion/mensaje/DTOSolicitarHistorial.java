package dto.comunicacion.peticion.mensaje;

/**
 * DTO para el payload de la petición 'solicitarHistorialPrivado'.
 * Incluye información del remitente y destinatario con sus respectivos peers.
 */
public class DTOSolicitarHistorial {
    private final String remitenteId;          // UUID del usuario que solicita
    private final String peerRemitenteId;      // UUID del peer del solicitante
    private final String destinatarioId;       // UUID del contacto del chat
    private final String peerDestinatarioId;   // UUID del peer del destinatario

    public DTOSolicitarHistorial(String remitenteId, String peerRemitenteId,
                                  String destinatarioId, String peerDestinatarioId) {
        this.remitenteId = remitenteId;
        this.peerRemitenteId = peerRemitenteId;
        this.destinatarioId = destinatarioId;
        this.peerDestinatarioId = peerDestinatarioId;
    }

    public String getRemitenteId() {
        return remitenteId;
    }

    public String getPeerRemitenteId() {
        return peerRemitenteId;
    }

    public String getDestinatarioId() {
        return destinatarioId;
    }

    public String getPeerDestinatarioId() {
        return peerDestinatarioId;
    }

    @Override
    public String toString() {
        return "DTOSolicitarHistorial{" +
                "remitenteId='" + remitenteId + '\'' +
                ", peerRemitenteId='" + peerRemitenteId + '\'' +
                ", destinatarioId='" + destinatarioId + '\'' +
                ", peerDestinatarioId='" + peerDestinatarioId + '\'' +
                '}';
    }
}
