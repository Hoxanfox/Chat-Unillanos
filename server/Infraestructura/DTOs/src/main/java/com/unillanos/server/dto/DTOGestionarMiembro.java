package com.unillanos.server.dto;

/**
 * DTO para gestionar miembros de un canal (agregar, remover, cambiar rol).
 * Solo puede ser ejecutado por un administrador del canal.
 */
public class DTOGestionarMiembro {
    
    private String adminId;         // Requerido - Usuario que realiza la acción (debe ser admin)
    private String canalId;         // Requerido
    private String usuarioId;       // Requerido - Usuario a agregar/remover/cambiar rol
    private String accion;          // "AGREGAR", "REMOVER", "CAMBIAR_ROL"
    private String nuevoRol;        // Opcional - "ADMIN" o "MEMBER" (solo para CAMBIAR_ROL)

    public DTOGestionarMiembro() {
    }

    public DTOGestionarMiembro(String adminId, String canalId, String usuarioId, 
                               String accion, String nuevoRol) {
        this.adminId = adminId;
        this.canalId = canalId;
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.nuevoRol = nuevoRol;
    }

    // Getters y Setters
    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

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

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getNuevoRol() {
        return nuevoRol;
    }

    public void setNuevoRol(String nuevoRol) {
        this.nuevoRol = nuevoRol;
    }

    @Override
    public String toString() {
        return "DTOGestionarMiembro{" +
                "adminId='" + adminId + '\'' +
                ", canalId='" + canalId + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                ", accion='" + accion + '\'' +
                ", nuevoRol='" + nuevoRol + '\'' +
                '}';
    }
}

