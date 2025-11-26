package dto.mensajeria;

/**
 * DTO para enviar mensajes de audio entre contactos.
 * Usado en la ruta: enviarmensajedirectoaudio
 */
public class DTOEnviarMensajeAudio {
    private String remitenteId;
    private String destinatarioId;
    private String peerRemitenteId;
    private String peerDestinoId;
    private String audioId;

    public DTOEnviarMensajeAudio() {}

    public DTOEnviarMensajeAudio(String remitenteId, String destinatarioId, String peerRemitenteId, String peerDestinoId, String audioId) {
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.peerRemitenteId = peerRemitenteId;
        this.peerDestinoId = peerDestinoId;
        this.audioId = audioId;
    }

    // Getters y Setters
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

    public String getAudioId() {
        return audioId;
    }

    public void setAudioId(String audioId) {
        this.audioId = audioId;
    }
}
