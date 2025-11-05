package com.unillanos.server.dto;

/**
 * DTO de respuesta con información de un miembro de un canal.
 */
public class DTOMiembroCanal {
    
    private String usuarioId;
    private String nombreUsuario;
    private String rol;             // "ADMIN" o "MEMBER"
    private String fechaUnion;      // ISO-8601

    public DTOMiembroCanal() {
    }

    public DTOMiembroCanal(String usuarioId, String nombreUsuario, String rol, String fechaUnion) {
        this.usuarioId = usuarioId;
        this.nombreUsuario = nombreUsuario;
        this.rol = rol;
        this.fechaUnion = fechaUnion;
    }

    // Getters y Setters
    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getFechaUnion() {
        return fechaUnion;
    }

    public void setFechaUnion(String fechaUnion) {
        this.fechaUnion = fechaUnion;
    }

    @Override
    public String toString() {
        return "DTOMiembroCanal{" +
                "usuarioId='" + usuarioId + '\'' +
                ", nombreUsuario='" + nombreUsuario + '\'' +
                ", rol='" + rol + '\'' +
                ", fechaUnion='" + fechaUnion + '\'' +
                '}';
    }
}

