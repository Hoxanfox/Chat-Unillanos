package com.unillanos.server.dto;

/**
 * DTO para marcar un mensaje como leído.
 * Se envía cuando el destinatario confirma que ha leído el mensaje.
 */
public class DTOMarcarMensajeLeido {
    
    private String mensajeId;
    private String usuarioId; // Quien marca como leído

    // Constructor por defecto
    public DTOMarcarMensajeLeido() {}

    // Constructor con parámetros
    public DTOMarcarMensajeLeido(String mensajeId, String usuarioId) {
        this.mensajeId = mensajeId;
        this.usuarioId = usuarioId;
    }

    // Getters y Setters
    public String getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(String mensajeId) {
        this.mensajeId = mensajeId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    @Override
    public String toString() {
        return "DTOMarcarMensajeLeido{" +
                "mensajeId='" + mensajeId + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                '}';
    }
}
