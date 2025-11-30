package dto.mensajeria;

/**
 * DTO para solicitar historial de mensajes entre dos usuarios.
 * Usado en la ruta: solicitarhistorialprivado
 */
public class DTOSolicitarHistorial {
    private String remitenteId;
    private String destinatarioId;
    private String peerRemitenteId;
    private String peerDestinatarioId;

    public DTOSolicitarHistorial() {}

    public DTOSolicitarHistorial(String remitenteId, String peerRemitenteId, String destinatarioId, String peerDestinatarioId) {
        this.remitenteId = remitenteId;
        this.peerRemitenteId = peerRemitenteId;
        this.destinatarioId = destinatarioId;
        this.peerDestinatarioId = peerDestinatarioId;
    }

    public String getRemitenteId() {
        return remitenteId;
    }

    public void setRemitenteId(String remitenteId) {
        this.remitenteId = remitenteId;
    }

    public String getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(String destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public String getPeerRemitenteId() {
        return peerRemitenteId;
    }

    public void setPeerRemitenteId(String peerRemitenteId) {
        this.peerRemitenteId = peerRemitenteId;
    }

    public String getPeerDestinatarioId() {
        return peerDestinatarioId;
    }

    public void setPeerDestinatarioId(String peerDestinatarioId) {
        this.peerDestinatarioId = peerDestinatarioId;
    }
}