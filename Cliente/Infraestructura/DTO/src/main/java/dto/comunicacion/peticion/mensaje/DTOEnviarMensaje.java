package dto.comunicacion.peticion.mensaje;

/**
 * DTO para el payload de la petición 'enviarMensajePrivado'.
 * Alineado con la API del servidor.
 */
public final class DTOEnviarMensaje {
    private final String remitenteId;    // Quién envía el mensaje
    private final String destinatarioId; // A quién se le envía
    private final String tipo;           // "TEXTO", "AUDIO", "IMAGEN", "ARCHIVO", "VIDEO"
    private final String contenido;      // Para mensajes de texto
    private final String fileId;         // Para mensajes de audio, imagen, etc.
    private final String fileName;       // Nombre del archivo (opcional)

    // Constructor privado para forzar el uso de los métodos estáticos de fábrica.
    private DTOEnviarMensaje(String remitenteId, String destinatarioId, String tipo,
                            String contenido, String fileId, String fileName) {
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.tipo = tipo;
        this.contenido = contenido;
        this.fileId = fileId;
        this.fileName = fileName;
    }

    /**
     * Crea un DTO para un mensaje de texto.
     * @param remitenteId El ID del usuario que envía el mensaje.
     * @param destinatarioId El ID del contacto que recibirá el mensaje.
     * @param contenido El texto del mensaje.
     */
    public static DTOEnviarMensaje deTexto(String remitenteId, String destinatarioId, String contenido) {
        return new DTOEnviarMensaje(remitenteId, destinatarioId, "TEXTO", contenido, null, null);
    }

    /**
     * Crea un DTO para un mensaje de audio.
     * @param remitenteId El ID del usuario que envía el mensaje.
     * @param destinatarioId El ID del contacto que recibirá el mensaje.
     * @param audioFileId El ID del archivo de audio ya subido al servidor.
     * @param fileName El nombre del archivo de audio.
     */
    public static DTOEnviarMensaje deAudio(String remitenteId, String destinatarioId,
                                           String audioFileId, String fileName) {
        return new DTOEnviarMensaje(remitenteId, destinatarioId, "AUDIO", null, audioFileId, fileName);
    }

    /**
     * Crea un DTO para un mensaje de imagen.
     * @param remitenteId El ID del usuario que envía el mensaje.
     * @param destinatarioId El ID del contacto que recibirá el mensaje.
     * @param contenido Descripción o texto que acompaña la imagen.
     * @param imageFileId El ID de la imagen ya subida al servidor.
     * @param fileName El nombre del archivo de imagen.
     */
    public static DTOEnviarMensaje deImagen(String remitenteId, String destinatarioId,
                                            String contenido, String imageFileId, String fileName) {
        return new DTOEnviarMensaje(remitenteId, destinatarioId, "IMAGEN", contenido, imageFileId, fileName);
    }

    /**
     * Crea un DTO para un mensaje con archivo.
     * @param remitenteId El ID del usuario que envía el mensaje.
     * @param destinatarioId El ID del contacto que recibirá el mensaje.
     * @param contenido Descripción o texto que acompaña el archivo.
     * @param fileId El ID del archivo ya subido al servidor.
     * @param fileName El nombre del archivo.
     */
    public static DTOEnviarMensaje deArchivo(String remitenteId, String destinatarioId,
                                             String contenido, String fileId, String fileName) {
        return new DTOEnviarMensaje(remitenteId, destinatarioId, "ARCHIVO", contenido, fileId, fileName);
    }

    // Getters
    public String getRemitenteId() { return remitenteId; }
    public String getDestinatarioId() { return destinatarioId; }
    public String getTipo() { return tipo; }
    public String getContenido() { return contenido; }
    public String getFileId() { return fileId; }
    public String getFileName() { return fileName; }
}
