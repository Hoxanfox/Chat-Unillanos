package dominio;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clase de Dominio que representa la entidad 'MensajeEnviadoContacto'.
 * Corresponde directamente a la tabla de la base de datos para los mensajes
 * que el usuario local ha enviado.
 */
public class MensajeEnviadoContacto {

    private UUID idMensajeEnviadoContacto;
    private byte[] contenido;
    private LocalDateTime fechaEnvio;
    private String tipo;
    private UUID idRemitente;
    private UUID idDestinatarioUsuario;

    public MensajeEnviadoContacto() {
    }

    public MensajeEnviadoContacto(UUID idMensajeEnviadoContacto, byte[] contenido, LocalDateTime fechaEnvio,
                                  String tipo, UUID idRemitente, UUID idDestinatarioUsuario) {
        this.idMensajeEnviadoContacto = idMensajeEnviadoContacto;
        this.contenido = contenido;
        this.fechaEnvio = fechaEnvio;
        this.tipo = tipo;
        this.idRemitente = idRemitente;
        this.idDestinatarioUsuario = idDestinatarioUsuario;
    }

    // Getters y Setters
    public UUID getIdMensajeEnviadoContacto() {
        return idMensajeEnviadoContacto;
    }

    public void setIdMensajeEnviadoContacto(UUID idMensajeEnviadoContacto) {
        this.idMensajeEnviadoContacto = idMensajeEnviadoContacto;
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

    public UUID getIdDestinatarioUsuario() {
        return idDestinatarioUsuario;
    }

    public void setIdDestinatarioUsuario(UUID idDestinatarioUsuario) {
        this.idDestinatarioUsuario = idDestinatarioUsuario;
    }

    @Override
    public String toString() {
        return "MensajeEnviadoContacto{" +
                "idMensajeEnviadoContacto=" + idMensajeEnviadoContacto +
                ", tipo='" + tipo + '\'' +
                ", fechaEnvio=" + fechaEnvio +
                ", idRemitente=" + idRemitente +
                ", idDestinatarioUsuario=" + idDestinatarioUsuario +
                '}';
    }
}
