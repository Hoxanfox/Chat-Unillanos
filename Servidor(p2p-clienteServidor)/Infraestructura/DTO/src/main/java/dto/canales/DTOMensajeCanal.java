package dto.canales;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO para representar un mensaje de canal.
 * Usado para transferir datos de mensajes entre servidor y cliente.
 */
public class DTOMensajeCanal implements Serializable {

    private static final long serialVersionUID = 1L;

    private String mensajeId;
    private String canalId;
    private String remitenteId;
    private String nombreRemitente;
    private String fotoRemitente;
    private String tipo;              // "TEXTO", "AUDIO", "ARCHIVO", "IMAGEN"
    private String contenido;         // Contenido de texto o ruta local del archivo
    private String fileId;            // ID del archivo en el servidor
    private LocalDateTime fechaEnvio;
    private boolean esPropio;         // true si el mensaje fue enviado por el usuario actual

    public DTOMensajeCanal() {
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

    public String getFotoRemitente() {
        return fotoRemitente;
    }

    public void setFotoRemitente(String fotoRemitente) {
        this.fotoRemitente = fotoRemitente;
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

