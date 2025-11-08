package dto.comunicacion.peticion.mensaje;

/**
 * DTO específico para el payload de mensajes de audio.
 * Alineado con la API del servidor que espera el campo 'contenido'.
 */
public class DTOEnviarMensajeAudioPayload {
    private final String peerRemitenteId;
    private final String peerDestinoId;
    private final String remitenteId;
    private final String destinatarioId;
    private final String tipo;
    private final String contenido;  // ✅ CORREGIDO: El servidor espera 'contenido', no 'audioId'

    public DTOEnviarMensajeAudioPayload(String peerRemitenteId, String peerDestinoId,
                                        String remitenteId, String destinatarioId,
                                        String audioFilePath) {
        this.peerRemitenteId = peerRemitenteId;
        this.peerDestinoId = peerDestinoId;
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.tipo = "audio";
        this.contenido = audioFilePath;  // ✅ CORREGIDO: Usar 'contenido'
    }

    // Getters
    public String getPeerRemitenteId() { return peerRemitenteId; }
    public String getPeerDestinoId() { return peerDestinoId; }
    public String getRemitenteId() { return remitenteId; }
    public String getDestinatarioId() { return destinatarioId; }
    public String getTipo() { return tipo; }
    public String getContenido() { return contenido; }  // ✅ CORREGIDO: Getter para 'contenido'
}
