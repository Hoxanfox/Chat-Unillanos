package dto.canales;

import java.time.LocalDateTime;

/**
 * DTO para transferir información de mensajes de canal entre capas.
 * Usado tanto para mensajes enviados como recibidos.
 */
public class DTOMensajeCanal {
    private String mensajeId;
    private String canalId;
    private String remitenteId;
    private String nombreRemitente;
    private String tipo; // "texto", "audio", "imagen", "archivo"
    private String contenido; // Para texto
    private String fileId; // Para archivos multimedia
    private LocalDateTime fechaEnvio;
    private boolean esPropio; // true si el mensaje fue enviado por el usuario actual

    public DTOMensajeCanal() {
    }

    public DTOMensajeCanal(String mensajeId, String canalId, String remitenteId, String nombreRemitente,
                           String tipo, String contenido, String fileId, LocalDateTime fechaEnvio, boolean esPropio) {
        this.mensajeId = mensajeId;
        this.canalId = canalId;
        this.remitenteId = remitenteId;
        this.nombreRemitente = nombreRemitente;
        this.tipo = tipo;
        this.contenido = contenido;
        this.fileId = fileId;
        this.fechaEnvio = fechaEnvio;
        this.esPropio = esPropio;
    }

    // Factory methods
    public static DTOMensajeCanal deTexto(String mensajeId, String canalId, String remitenteId,
                                          String nombreRemitente, String contenido, LocalDateTime fechaEnvio, boolean esPropio) {
        return new DTOMensajeCanal(mensajeId, canalId, remitenteId, nombreRemitente, "texto", contenido, null, fechaEnvio, esPropio);
    }

    public static DTOMensajeCanal deAudio(String mensajeId, String canalId, String remitenteId,
                                          String nombreRemitente, String fileId, LocalDateTime fechaEnvio, boolean esPropio) {
        return new DTOMensajeCanal(mensajeId, canalId, remitenteId, nombreRemitente, "audio", null, fileId, fechaEnvio, esPropio);
    }

    // Getters y Setters
    public String getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(String mensajeId) {
        this.mensajeId = mensajeId;
    }

    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    public String getRemitenteId() {
        return remitenteId;
    }

    public void setRemitenteId(String remitenteId) {
        this.remitenteId = remitenteId;
    }

    public String getNombreRemitente() {
        return nombreRemitente;
    }

    public void setNombreRemitente(String nombreRemitente) {
        this.nombreRemitente = nombreRemitente;
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

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public boolean isEsPropio() {
        return esPropio;
    }

    public void setEsPropio(boolean esPropio) {
        this.esPropio = esPropio;
    }

    @Override
    public String toString() {
        return "DTOMensajeCanal{" +
                "mensajeId='" + mensajeId + '\'' +
                ", canalId='" + canalId + '\'' +
                ", remitenteId='" + remitenteId + '\'' +
                ", nombreRemitente='" + nombreRemitente + '\'' +
                ", tipo='" + tipo + '\'' +
                ", fechaEnvio=" + fechaEnvio +
                ", esPropio=" + esPropio +
                '}';
    }
}

