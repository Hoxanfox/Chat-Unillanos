package dominio;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clase de Dominio que representa la entidad 'MensajeRecibidoContacto'.
 * Corresponde directamente a la tabla de la base de datos para los mensajes
 * que el usuario local ha recibido.
 */
public class MensajeRecibidoContacto {

    private UUID idMensaje;
    private byte[] contenido;
    private LocalDateTime fechaEnvio;
    private String tipo;
    private UUID idDestinatario;
    private UUID idRemitenteUsuario;

    public MensajeRecibidoContacto() {
    }

    public MensajeRecibidoContacto(UUID idMensaje, byte[] contenido, LocalDateTime fechaEnvio,
                                   String tipo, UUID idDestinatario, UUID idRemitenteUsuario) {
        this.idMensaje = idMensaje;
        this.contenido = contenido;
        this.fechaEnvio = fechaEnvio;
        this.tipo = tipo;
        this.idDestinatario = idDestinatario;
        this.idRemitenteUsuario = idRemitenteUsuario;
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

    public UUID getIdRemitenteUsuario() {
        return idRemitenteUsuario;
    }

    public void setIdRemitenteUsuario(UUID idRemitenteUsuario) {
        this.idRemitenteUsuario = idRemitenteUsuario;
    }

    @Override
    public String toString() {
        return "MensajeRecibidoContacto{" +
                "idMensaje=" + idMensaje +
                ", tipo='" + tipo + '\'' +
                ", fechaEnvio=" + fechaEnvio +
                ", idDestinatario=" + idDestinatario +
                ", idRemitenteUsuario=" + idRemitenteUsuario +
                '}';
    }
}
