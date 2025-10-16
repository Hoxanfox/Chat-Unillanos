package com.unillanos.server.entity;

import java.time.LocalDateTime;

/**
 * Entidad para contactos/amistades del sistema.
 */
public class ContactoEntity {
    private String id;
    private String usuarioId;
    private String contactoId;
    private String estado; // "PENDIENTE", "ACEPTADO", "RECHAZADO", "BLOQUEADO"
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaRespuesta;
    private String solicitadoPor; // "usuarioId" o "contactoId"

    public ContactoEntity() {}

    public ContactoEntity(String id, String usuarioId, String contactoId, String estado,
                         LocalDateTime fechaSolicitud, LocalDateTime fechaRespuesta, String solicitadoPor) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.contactoId = contactoId;
        this.estado = estado;
        this.fechaSolicitud = fechaSolicitud;
        this.fechaRespuesta = fechaRespuesta;
        this.solicitadoPor = solicitadoPor;
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

    public String getContactoId() {
        return contactoId;
    }

    public void setContactoId(String contactoId) {
        this.contactoId = contactoId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }

    public void setFechaRespuesta(LocalDateTime fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }

    public String getSolicitadoPor() {
        return solicitadoPor;
    }

    public void setSolicitadoPor(String solicitadoPor) {
        this.solicitadoPor = solicitadoPor;
    }

    /**
     * Verifica si la relación está activa (aceptada).
     */
    public boolean isActiva() {
        return "ACEPTADO".equals(this.estado);
    }

    /**
     * Verifica si la solicitud está pendiente.
     */
    public boolean isPendiente() {
        return "PENDIENTE".equals(this.estado);
    }
}
