package com.unillanos.server.entity;

import java.time.LocalDateTime;

/**
 * Entidad que representa un registro de log en la base de datos.
 * Tabla: logs_sistema
 */
public class LogEntity {
    
    private Long id;
    private LocalDateTime timestamp;
    private String tipo; // LOGIN, LOGOUT, ERROR, INFO, SYSTEM
    private String usuarioId;
    private String ipAddress;
    private String accion;
    private String detalles;

    public LogEntity() {
    }

    public LogEntity(LocalDateTime timestamp, String tipo, String usuarioId, 
                     String ipAddress, String accion, String detalles) {
        this.timestamp = timestamp;
        this.tipo = tipo;
        this.usuarioId = usuarioId;
        this.ipAddress = ipAddress;
        this.accion = accion;
        this.detalles = detalles;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getDetalles() {
        return detalles;
    }

    public void setDetalles(String detalles) {
        this.detalles = detalles;
    }

    @Override
    public String toString() {
        return "LogEntity{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", tipo='" + tipo + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", accion='" + accion + '\'' +
                ", detalles='" + detalles + '\'' +
                '}';
    }
}
