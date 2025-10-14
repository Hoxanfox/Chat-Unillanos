package com.unillanos.server.dto;

/**
 * DTO para listar canales con paginación.
 */
public class DTOListarCanales {
    
    private String usuarioId;       // Opcional - Si se provee, lista solo los canales del usuario
    private int limit;              // Opcional - Por defecto 50
    private int offset;             // Opcional - Por defecto 0

    public DTOListarCanales() {
        this.limit = 50;  // Valor por defecto
        this.offset = 0;  // Valor por defecto
    }

    public DTOListarCanales(String usuarioId, int limit, int offset) {
        this.usuarioId = usuarioId;
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
        return "DTOListarCanales{" +
                "usuarioId='" + usuarioId + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                '}';
    }
}

