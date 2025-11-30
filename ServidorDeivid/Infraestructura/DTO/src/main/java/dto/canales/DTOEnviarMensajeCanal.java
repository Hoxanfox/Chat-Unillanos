package dto.canales;

import java.io.Serializable;

/**
 * DTO para enviar un mensaje a un canal.
 * Soporta mensajes de texto, audio y archivos.
 */
public class DTOEnviarMensajeCanal implements Serializable {

    private static final long serialVersionUID = 1L;

    private String remitenteId;
    private String canalId;
    private String contenido;    // Para mensajes de texto
    private String fileId;       // Para archivos/audio
    private String tipoMensaje;  // "TEXTO", "AUDIO", "ARCHIVO"

    public DTOEnviarMensajeCanal() {
    }

    public DTOEnviarMensajeCanal(String remitenteId, String canalId, String contenido, String fileId, String tipoMensaje) {
        this.remitenteId = remitenteId;
        this.canalId = canalId;
        this.contenido = contenido;
        this.fileId = fileId;
        this.tipoMensaje = tipoMensaje;
    }

    // Factory methods para facilitar la creación
    public static DTOEnviarMensajeCanal deTexto(String remitenteId, String canalId, String contenido) {
        return new DTOEnviarMensajeCanal(remitenteId, canalId, contenido, null, "TEXTO");
    }

    public static DTOEnviarMensajeCanal deAudio(String remitenteId, String canalId, String audioFileId) {
        return new DTOEnviarMensajeCanal(remitenteId, canalId, null, audioFileId, "AUDIO");
    }

    public static DTOEnviarMensajeCanal deArchivo(String remitenteId, String canalId, String fileId) {
        return new DTOEnviarMensajeCanal(remitenteId, canalId, null, fileId, "ARCHIVO");
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

    public String getTipoMensaje() {
        return tipoMensaje;
    }

    public void setTipoMensaje(String tipoMensaje) {
        this.tipoMensaje = tipoMensaje;
    }

    @Override
    public String toString() {
        return "DTOEnviarMensajeCanal{" +
                "remitenteId='" + remitenteId + '\'' +
                ", canalId='" + canalId + '\'' +
                ", tipoMensaje='" + tipoMensaje + '\'' +
                ", hasContenido=" + (contenido != null) +
                ", hasFileId=" + (fileId != null) +
                '}';
    }
}

