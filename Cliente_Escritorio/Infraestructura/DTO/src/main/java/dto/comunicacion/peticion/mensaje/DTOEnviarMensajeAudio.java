package dto.comunicacion.peticion.mensaje;

/**
 * DTO específico para enviar mensajes de audio con referencia a archivo (audioId).
 * Alineado con la API del servidor que espera el campo "audioId".
 */
public class DTOEnviarMensajeAudio {
    private final String peerDestinoId;
    private final String peerRemitenteId;
    private final String remitenteId;
    private final String destinatarioId;
    private final String tipo;
    private final String audioId;  // ID del archivo de audio en el servidor (campo esperado por el servidor)

    public DTOEnviarMensajeAudio(String peerDestinoId, String peerRemitenteId,
                                 String remitenteId, String destinatarioId,
                                 String audioFileId) {
        this.peerDestinoId = peerDestinoId;
        this.peerRemitenteId = peerRemitenteId;
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.tipo = "audio";
        this.audioId = audioFileId;
    }

    // Getters
    public String getPeerDestinoId() { return peerDestinoId; }
    public String getPeerRemitenteId() { return peerRemitenteId; }
    public String getRemitenteId() { return remitenteId; }
    public String getDestinatarioId() { return destinatarioId; }
    public String getTipo() { return tipo; }
    public String getAudioId() { return audioId; }
}
