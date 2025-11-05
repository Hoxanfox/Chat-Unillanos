package com.unillanos.server.dto;

/**
 * DTO para que un usuario se una a un canal.
 */
public class DTOUnirseCanal {
    
    private String usuarioId;       // Requerido
    private String canalId;         // Requerido

    public DTOUnirseCanal() {
    }

    public DTOUnirseCanal(String usuarioId, String canalId) {
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
        return "DTOUnirseCanal{" +
                "usuarioId='" + usuarioId + '\'' +
                ", canalId='" + canalId + '\'' +
                '}';
    }
}

