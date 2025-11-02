package dto.comunicacion.peticion.mensaje;

/**
 * DTO para el payload de la petición 'enviarMensajeDirecto'.
 * Alineado con la nueva API del servidor que incluye peers WebRTC.
 */
public final class DTOEnviarMensaje {
    private final String peerRemitenteId;  // ← NUEVO: UUID del peer remitente
    private final String peerDestinoId;    // ← NUEVO: UUID del peer destino
    private final String remitenteId;      // Quién envía el mensaje
    private final String destinatarioId;   // A quién se le envía
    private final String tipo;             // "TEXTO", "AUDIO", "IMAGEN", "ARCHIVO", "VIDEO"
    private final String contenido;        // Para mensajes de texto
    private final String fileId;           // Para mensajes de audio, imagen, etc.
    private final String fileName;         // Nombre del archivo (opcional)

    // Constructor privado para forzar el uso de los métodos estáticos de fábrica.
    private DTOEnviarMensaje(String peerRemitenteId, String peerDestinoId,
                            String remitenteId, String destinatarioId, String tipo,
                            String contenido, String fileId, String fileName) {
        this.peerRemitenteId = peerRemitenteId;
        this.peerDestinoId = peerDestinoId;
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.tipo = tipo;
        this.contenido = contenido;
        this.fileId = fileId;
        this.fileName = fileName;
    }

    /**
     * Crea un DTO para un mensaje de texto.
     */
    public static DTOEnviarMensaje deTexto(String peerRemitenteId, String peerDestinoId,
                                          String remitenteId, String destinatarioId, String contenido) {
        return new DTOEnviarMensaje(peerRemitenteId, peerDestinoId, remitenteId, destinatarioId, "TEXTO", contenido, null, null);
    }

    /**
     * Crea un DTO para un mensaje de audio.
     */
    public static DTOEnviarMensaje deAudio(String peerRemitenteId, String peerDestinoId,
                                          String remitenteId, String destinatarioId,
                                          String audioFileId, String fileName) {
        return new DTOEnviarMensaje(peerRemitenteId, peerDestinoId, remitenteId, destinatarioId, "AUDIO", null, audioFileId, fileName);
    }

    /**
     * Crea un DTO para un mensaje de imagen.
     */
    public static DTOEnviarMensaje deImagen(String peerRemitenteId, String peerDestinoId,
                                           String remitenteId, String destinatarioId,
                                           String contenido, String imageFileId, String fileName) {
        return new DTOEnviarMensaje(peerRemitenteId, peerDestinoId, remitenteId, destinatarioId, "IMAGEN", contenido, imageFileId, fileName);
    }

    /**
     * Crea un DTO para un mensaje con archivo.
     */
    public static DTOEnviarMensaje deArchivo(String peerRemitenteId, String peerDestinoId,
                                            String remitenteId, String destinatarioId,
                                            String contenido, String fileId, String fileName) {
        return new DTOEnviarMensaje(peerRemitenteId, peerDestinoId, remitenteId, destinatarioId, "ARCHIVO", contenido, fileId, fileName);
    }

    // Getters
    public String getPeerRemitenteId() { return peerRemitenteId; }
    public String getPeerDestinoId() { return peerDestinoId; }
    public String getRemitenteId() { return remitenteId; }
    public String getDestinatarioId() { return destinatarioId; }
    public String getTipo() { return tipo; }
    public String getContenido() { return contenido; }
    public String getFileId() { return fileId; }
    public String getFileName() { return fileName; }
}
