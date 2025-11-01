package dto.comunicacion.peticion.mensaje;

/**
 * DTO específico para enviar mensajes de audio con contenido Base64.
 */
public class DTOEnviarMensajeAudio {
    private final String peerDestinoId;
    private final String peerRemitenteId;
    private final String remitenteId;
    private final String destinatarioId;
    private final String tipo;
    private final String contenido;  // Audio en Base64

    public DTOEnviarMensajeAudio(String peerDestinoId, String peerRemitenteId,
                                 String remitenteId, String destinatarioId,
                                 String audioBase64) {
        this.peerDestinoId = peerDestinoId;
        this.peerRemitenteId = peerRemitenteId;
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.tipo = "audio";
        this.contenido = audioBase64;
    }

    // Getters
    public String getPeerDestinoId() { return peerDestinoId; }
    public String getPeerRemitenteId() { return peerRemitenteId; }
    public String getRemitenteId() { return remitenteId; }
    public String getDestinatarioId() { return destinatarioId; }
    public String getTipo() { return tipo; }
    public String getContenido() { return contenido; }
}
