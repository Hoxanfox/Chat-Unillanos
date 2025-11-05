package dominio;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de Dominio: Mensaje Recibido de Canal
 * Representa un mensaje recibido por un usuario desde un canal.
 */
public class MensajeRecibidoCanal {
    private UUID idMensaje;
    private byte[] contenido;
    private LocalDateTime fechaEnvio;
    private String tipo;
    private UUID idDestinatario;
    private UUID idRemitenteCanal;

    public MensajeRecibidoCanal() {
    }

    public MensajeRecibidoCanal(UUID idMensaje, byte[] contenido, LocalDateTime fechaEnvio, 
                                String tipo, UUID idDestinatario, UUID idRemitenteCanal) {
        this.idMensaje = idMensaje;
        this.contenido = contenido;
        this.fechaEnvio = fechaEnvio;
        this.tipo = tipo;
        this.idDestinatario = idDestinatario;
        this.idRemitenteCanal = idRemitenteCanal;
    }

    // Getters y Setters
    public UUID getIdMensaje() {
        return idMensaje;
    }

    public void setIdMensaje(UUID idMensaje) {
        this.idMensaje = idMensaje;
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

    public UUID getIdDestinatario() {
        return idDestinatario;
    }

    public void setIdDestinatario(UUID idDestinatario) {
        this.idDestinatario = idDestinatario;
    }

    public UUID getIdRemitenteCanal() {
        return idRemitenteCanal;
    }

    public void setIdRemitenteCanal(UUID idRemitenteCanal) {
        this.idRemitenteCanal = idRemitenteCanal;
    }

    @Override
    public String toString() {
        return "MensajeRecibidoCanal{" +
                "idMensaje=" + idMensaje +
                ", tipo='" + tipo + '\'' +
                ", fechaEnvio=" + fechaEnvio +
                ", idDestinatario=" + idDestinatario +
                ", idRemitenteCanal=" + idRemitenteCanal +
                '}';
    }
}

