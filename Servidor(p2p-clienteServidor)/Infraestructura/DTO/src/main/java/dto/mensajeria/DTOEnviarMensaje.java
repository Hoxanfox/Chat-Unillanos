
package dto.mensajeria;

/**
 * DTO para enviar mensajes de texto entre contactos.
 * Usado en la ruta: enviarmensajedirecto
 */
public class DTOEnviarMensaje {
    private String remitenteId;
    private String destinatarioId;
    private String peerRemitenteId;
    private String peerDestinoId;
    private String contenido;

    public DTOEnviarMensaje() {}

    public DTOEnviarMensaje(String remitenteId, String destinatarioId, String peerRemitenteId, String peerDestinoId, String contenido) {
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.peerRemitenteId = peerRemitenteId;
        this.peerDestinoId = peerDestinoId;
        this.contenido = contenido;
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

    public String getPeerDestinoId() {
        return peerDestinoId;
    }

    public void setPeerDestinoId(String peerDestinoId) {
        this.peerDestinoId = peerDestinoId;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
}

