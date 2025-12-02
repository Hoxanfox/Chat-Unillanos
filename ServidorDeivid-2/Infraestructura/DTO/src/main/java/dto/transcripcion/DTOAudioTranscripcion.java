package dto.transcripcion;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO para representar un audio con su transcripción
 */
public class DTOAudioTranscripcion implements Serializable {

    private static final long serialVersionUID = 1L;

    private String audioId;
    private String mensajeId;
    private String remitenteId;
    private String nombreRemitente;
    private String canalId;
    private String nombreCanal;
    private String contactoId;
    private String nombreContacto;
    private String rutaArchivo;
    private String transcripcion;
    private LocalDateTime fechaEnvio;
    private long duracionSegundos;
    private boolean esCanal; // true si es de canal, false si es de contacto
    private boolean transcrito;

    public DTOAudioTranscripcion() {
    }

    // Getters y Setters
    public String getAudioId() {
        return audioId;
    }

    public void setAudioId(String audioId) {
        this.audioId = audioId;
    }

    public String getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(String mensajeId) {
        this.mensajeId = mensajeId;
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

    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    public String getNombreCanal() {
        return nombreCanal;
    }

    public void setNombreCanal(String nombreCanal) {
        this.nombreCanal = nombreCanal;
    }

    public String getContactoId() {
        return contactoId;
    }

    public void setContactoId(String contactoId) {
        this.contactoId = contactoId;
    }

    public String getNombreContacto() {
        return nombreContacto;
    }

    public void setNombreContacto(String nombreContacto) {
        this.nombreContacto = nombreContacto;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public String getTranscripcion() {
        return transcripcion;
    }

    public void setTranscripcion(String transcripcion) {
        this.transcripcion = transcripcion;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public long getDuracionSegundos() {
        return duracionSegundos;
    }

    public void setDuracionSegundos(long duracionSegundos) {
        this.duracionSegundos = duracionSegundos;
    }

    public boolean isEsCanal() {
        return esCanal;
    }

    public void setEsCanal(boolean esCanal) {
        this.esCanal = esCanal;
    }

    public boolean isTranscrito() {
        return transcrito;
    }

    public void setTranscrito(boolean transcrito) {
        this.transcrito = transcrito;
    }

    @Override
    public String toString() {
        return "DTOAudioTranscripcion{" +
                "audioId='" + audioId + '\'' +
                ", nombreRemitente='" + nombreRemitente + '\'' +
                ", origen='" + (esCanal ? nombreCanal : nombreContacto) + '\'' +
                ", fechaEnvio=" + fechaEnvio +
                ", transcrito=" + transcrito +
                '}';
    }
}

