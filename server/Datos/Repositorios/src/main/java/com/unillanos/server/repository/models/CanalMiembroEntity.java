package com.unillanos.server.repository.models;

import java.time.LocalDateTime;

/**
 * Entidad que representa la relación N:M entre usuarios y canales.
 * Define la pertenencia de un usuario a un canal y su rol.
 */
public class CanalMiembroEntity {
    
    private String canalId;
    private String usuarioId;
    private LocalDateTime fechaUnion;
    private RolCanal rol;

    // Constructores
    public CanalMiembroEntity() {
    }

    public CanalMiembroEntity(String canalId, String usuarioId, LocalDateTime fechaUnion, RolCanal rol) {
        this.canalId = canalId;
        this.usuarioId = usuarioId;
        this.fechaUnion = fechaUnion;
        this.rol = rol;
    }

    // Getters y Setters
    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public LocalDateTime getFechaUnion() {
        return fechaUnion;
    }

    public void setFechaUnion(LocalDateTime fechaUnion) {
        this.fechaUnion = fechaUnion;
    }

    public RolCanal getRol() {
        return rol;
    }

    public void setRol(RolCanal rol) {
        this.rol = rol;
    }

    @Override
    public String toString() {
        return "CanalMiembroEntity{" +
                "canalId='" + canalId + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                ", fechaUnion=" + fechaUnion +
                ", rol=" + rol +
                '}';
    }
}

