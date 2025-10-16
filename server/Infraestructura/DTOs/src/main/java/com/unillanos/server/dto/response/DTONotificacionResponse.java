package com.unillanos.server.dto.response;

import java.time.LocalDateTime;

/**
 * DTO para notificaciones en respuestas.
 * Estructura específica que espera el cliente.
 */
public class DTONotificacionResponse {
    private String id;
    private String usuarioId;
    private String tipo; // "SOLICITUD_AMISTAD", "INVITACION_CANAL", "MENSAJE_DIRECTO", etc.
    private String titulo;
    private String mensaje;
    private String remitenteId;
    private String nombreRemitente;
    private String canalId;
    private String nombreCanal;
    private boolean leida;
    private LocalDateTime timestamp;
    private String accion; // "aceptar", "rechazar", "ver", etc.

    public DTONotificacionResponse() {}

    public DTONotificacionResponse(String id, String usuarioId, String tipo, String titulo, String mensaje,
                                 String remitenteId, String nombreRemitente, String canalId, String nombreCanal,
                                 boolean leida, LocalDateTime timestamp, String accion) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.remitenteId = remitenteId;
        this.nombreRemitente = nombreRemitente;
        this.canalId = canalId;
        this.nombreCanal = nombreCanal;
        this.leida = leida;
        this.timestamp = timestamp;
        this.accion = accion;
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
}
