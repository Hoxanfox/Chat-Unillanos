package com.unillanos.server.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para notificaciones del sistema.
 */
public class NotificacionEntity {
    private String id;
    private String usuarioId;
    private String tipo; // "SOLICITUD_AMISTAD", "INVITACION_CANAL", "MENSAJE_DIRECTO", etc.
    private String titulo;
    private String mensaje;
    private String remitenteId;
    private String canalId;
    private boolean leida;
    private LocalDateTime timestamp;
    private String accion; // "aceptar", "rechazar", "ver", etc.
    private String metadata; // JSON adicional si es necesario

    public NotificacionEntity() {}

    public NotificacionEntity(String id, String usuarioId, String tipo, String titulo, String mensaje,
                            String remitenteId, String canalId, boolean leida, LocalDateTime timestamp,
                            String accion, String metadata) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.remitenteId = remitenteId;
        this.canalId = canalId;
        this.leida = leida;
        this.timestamp = timestamp;
        this.accion = accion;
        this.metadata = metadata;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

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

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    /**
     * Convierte la entidad a DTO para respuestas.
     */
    public com.unillanos.server.dto.response.DTONotificacionResponse toDTO(String nombreRemitente, String nombreCanal) {
        return new com.unillanos.server.dto.response.DTONotificacionResponse(
            this.id,
            this.usuarioId,
            this.tipo,
            this.titulo,
            this.mensaje,
            this.remitenteId,
            nombreRemitente,
            this.canalId,
            nombreCanal,
            this.leida,
            this.timestamp,
            this.accion
        );
    }
}
