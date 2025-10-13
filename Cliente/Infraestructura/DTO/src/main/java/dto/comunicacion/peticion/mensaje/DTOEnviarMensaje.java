package dto.comunicacion.peticion.mensaje;

/**
 * DTO para el payload de la petición 'enviarMensajePrivado'.
 * AHORA incluye tanto el remitente como el destinatario.
 */
public final class DTOEnviarMensaje {
    private final String remitenteId;    // Quién envía el mensaje
    private final String destinatarioId; // A quién se le envía
    private final String tipo;           // "texto", "audio", etc.
    private final String contenido;      // Para mensajes de texto
    private final String fileId;         // Para mensajes de audio, imagen, etc.

    // Constructor privado para forzar el uso de los métodos estáticos de fábrica.
    private DTOEnviarMensaje(String remitenteId, String destinatarioId, String tipo, String contenido, String fileId) {
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.tipo = tipo;
        this.contenido = contenido;
        this.fileId = fileId;
    }

    /**
     * Crea un DTO para un mensaje de texto.
     * @param remitenteId El ID del usuario que envía el mensaje.
     * @param destinatarioId El ID del contacto que recibirá el mensaje.
     * @param contenido El texto del mensaje.
     */
    public static DTOEnviarMensaje deTexto(String remitenteId, String destinatarioId, String contenido) {
        return new DTOEnviarMensaje(remitenteId, destinatarioId, "texto", contenido, null);
    }

    /**
     * Crea un DTO para un mensaje de audio.
     * @param remitenteId El ID del usuario que envía el mensaje.
     * @param destinatarioId El ID del contacto que recibirá el mensaje.
     * @param audioFileId El ID del archivo de audio ya subido al servidor.
     */
    public static DTOEnviarMensaje deAudio(String remitenteId, String destinatarioId, String audioFileId) {
        return new DTOEnviarMensaje(remitenteId, destinatarioId, "audio", null, audioFileId);
    }

    // Getters
    public String getRemitenteId() { return remitenteId; }
    public String getDestinatarioId() { return destinatarioId; }
    public String getTipo() { return tipo; }
    public String getContenido() { return contenido; }
    public String getFileId() { return fileId; }
}

