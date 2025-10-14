package com.unillanos.server.dto;

/**
 * DTO para obtener el historial de mensajes (directos o de canal).
 */
public class DTOHistorial {
    
    private String usuarioId;           // Requerido - Usuario que solicita
    private String destinatarioId;      // Opcional - Para historial de mensajes directos
    private String canalId;             // Opcional - Para historial de canal
    private int limit;                  // Opcional - Por defecto 50
    private int offset;                 // Opcional - Por defecto 0

    public DTOHistorial() {
        this.limit = 50;  // Valor por defecto
        this.offset = 0;  // Valor por defecto
    }

    public DTOHistorial(String usuarioId, String destinatarioId, String canalId, int limit, int offset) {
        this.usuarioId = usuarioId;
        this.destinatarioId = destinatarioId;
        this.canalId = canalId;
        this.limit = limit > 0 ? limit : 50;
        this.offset = offset >= 0 ? offset : 0;
    }

    // Getters y Setters
    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(String destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit > 0 ? limit : 50;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset >= 0 ? offset : 0;
    }

    @Override
    public String toString() {
        return "DTOHistorial{" +
                "usuarioId='" + usuarioId + '\'' +
                ", destinatarioId='" + destinatarioId + '\'' +
                ", canalId='" + canalId + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                '}';
    }
}

