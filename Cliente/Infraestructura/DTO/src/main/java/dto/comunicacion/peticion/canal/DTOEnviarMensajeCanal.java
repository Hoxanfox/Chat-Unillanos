package dto.comunicacion.peticion.canal;

/**
 * DTO para enviar mensajes a canales.
 */
public class DTOEnviarMensajeCanal {
    private String remitenteId;
    private String canalId;
    private String tipo; // "texto", "audio", "imagen", "archivo"
    private String contenido; // Para mensajes de texto
    private String fileId; // Para archivos multimedia (audio, imagen, etc.)

    public DTOEnviarMensajeCanal() {
    }

    public DTOEnviarMensajeCanal(String remitenteId, String canalId, String tipo, String contenido, String fileId) {
        this.remitenteId = remitenteId;
        this.canalId = canalId;
        this.tipo = tipo;
        this.contenido = contenido;
        this.fileId = fileId;
    }

    // Factory methods para crear instancias específicas
    public static DTOEnviarMensajeCanal deTexto(String remitenteId, String canalId, String contenido) {
        return new DTOEnviarMensajeCanal(remitenteId, canalId, "texto", contenido, null);
    }

    public static DTOEnviarMensajeCanal deAudio(String remitenteId, String canalId, String audioFileId) {
        return new DTOEnviarMensajeCanal(remitenteId, canalId, "audio", null, audioFileId);
    }

    public static DTOEnviarMensajeCanal deArchivo(String remitenteId, String canalId, String fileId) {
        return new DTOEnviarMensajeCanal(remitenteId, canalId, "archivo", null, fileId);
    }

    // Getters y Setters
    public String getRemitenteId() {
        return remitenteId;
    }

    public void setRemitenteId(String remitenteId) {
        this.remitenteId = remitenteId;
    }

    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    @Override
    public String toString() {
        return "DTOEnviarMensajeCanal{" +
                "remitenteId='" + remitenteId + '\'' +
                ", canalId='" + canalId + '\'' +
                ", tipo='" + tipo + '\'' +
                '}';
    }
}

