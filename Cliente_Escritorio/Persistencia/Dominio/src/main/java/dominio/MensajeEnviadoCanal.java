package dominio;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de Dominio: Mensaje Enviado a Canal
 * Representa un mensaje enviado por un usuario a un canal.
 */
public class MensajeEnviadoCanal {
    private UUID idMensajeEnviadoCanal;
    private byte[] contenido;
    private LocalDateTime fechaEnvio;
    private String tipo;
    private UUID idRemitente;
    private UUID idDestinatarioCanal;

    public MensajeEnviadoCanal() {
    }

    public MensajeEnviadoCanal(UUID idMensajeEnviadoCanal, byte[] contenido, LocalDateTime fechaEnvio, 
                               String tipo, UUID idRemitente, UUID idDestinatarioCanal) {
        this.idMensajeEnviadoCanal = idMensajeEnviadoCanal;
        this.contenido = contenido;
        this.fechaEnvio = fechaEnvio;
        this.tipo = tipo;
        this.idRemitente = idRemitente;
        this.idDestinatarioCanal = idDestinatarioCanal;
    }

    // Getters y Setters
    public UUID getIdMensajeEnviadoCanal() {
        return idMensajeEnviadoCanal;
    }

    public void setIdMensajeEnviadoCanal(UUID idMensajeEnviadoCanal) {
        this.idMensajeEnviadoCanal = idMensajeEnviadoCanal;
    }

    public byte[] getContenido() {
        return contenido;
    }

    public void setContenido(byte[] contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public UUID getIdRemitente() {
        return idRemitente;
    }

    public void setIdRemitente(UUID idRemitente) {
        this.idRemitente = idRemitente;
    }

    public UUID getIdDestinatarioCanal() {
        return idDestinatarioCanal;
    }

    public void setIdDestinatarioCanal(UUID idDestinatarioCanal) {
        this.idDestinatarioCanal = idDestinatarioCanal;
    }

    @Override
    public String toString() {
        return "MensajeEnviadoCanal{" +
                "idMensajeEnviadoCanal=" + idMensajeEnviadoCanal +
                ", tipo='" + tipo + '\'' +
                ", fechaEnvio=" + fechaEnvio +
                ", idRemitente=" + idRemitente +
                ", idDestinatarioCanal=" + idDestinatarioCanal +
                '}';
    }
}

