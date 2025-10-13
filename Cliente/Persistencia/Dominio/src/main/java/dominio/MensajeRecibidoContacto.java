package dominio;

import java.util.Date;
import java.util.UUID;

/**
 * Clase de Dominio que representa la entidad 'MensajeRecibidoContacto'.
 * Corresponde directamente a la tabla de la base de datos para los mensajes
 * que el usuario local ha recibido.
 */
public class MensajeRecibidoContacto {

    private final UUID idMensaje;
    private final byte[] contenido;
    private final Date fechaEnvio;
    private final String tipo;
    private final UUID idRemitente;
    private final UUID idDestinatario;

    public MensajeRecibidoContacto(UUID idMensaje, byte[] contenido, Date fechaEnvio, String tipo, UUID idRemitente, UUID idDestinatario) {
        this.idMensaje = idMensaje;
        this.contenido = contenido;
        this.fechaEnvio = fechaEnvio;
        this.tipo = tipo;
        this.idRemitente = idRemitente;
        this.idDestinatario = idDestinatario;
    }

    // Getters para todos los campos

    public UUID getIdMensaje() {
        return idMensaje;
    }

    public byte[] getContenido() {
        return contenido;
    }

    public Date getFechaEnvio() {
        return fechaEnvio;
    }

    public String getTipo() {
        return tipo;
    }

    public UUID getIdRemitente() {
        return idRemitente;
    }

    public UUID getIdDestinatario() {
        return idDestinatario;
    }
}
