package com.unillanos.server.dto;

/**
 * DTO para que un usuario salga de un canal.
 */
public class DTOSalirCanal {
    
    private String usuarioId;       // Requerido
    private String canalId;         // Requerido

    public DTOSalirCanal() {
    }

    public DTOSalirCanal(String usuarioId, String canalId) {
        this.usuarioId = usuarioId;
        this.canalId = canalId;
    }

    // Getters y Setters
    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    @Override
    public String toString() {
        return "DTOSalirCanal{" +
                "usuarioId='" + usuarioId + '\'' +
                ", canalId='" + canalId + '\'' +
                '}';
    }
}

