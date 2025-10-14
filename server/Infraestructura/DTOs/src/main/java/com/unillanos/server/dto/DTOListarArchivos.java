package com.unillanos.server.dto;

/**
 * DTO para listar archivos de un usuario.
 */
public class DTOListarArchivos {
    
    private String usuarioId;              // Requerido - Usuario que solicita
    private String tipoArchivo;            // Opcional - Filtrar por tipo (IMAGEN, AUDIO, DOCUMENTO)
    private int limit;                     // Opcional - Por defecto 50
    private int offset;                    // Opcional - Por defecto 0

    public DTOListarArchivos() {
        this.limit = 50;  // Valor por defecto
        this.offset = 0;  // Valor por defecto
    }

    public DTOListarArchivos(String usuarioId, String tipoArchivo, int limit, int offset) {
        this.usuarioId = usuarioId;
        this.tipoArchivo = tipoArchivo;
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

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public void setTipoArchivo(String tipoArchivo) {
        this.tipoArchivo = tipoArchivo;
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
        return "DTOListarArchivos{" +
                "usuarioId='" + usuarioId + '\'' +
                ", tipoArchivo='" + tipoArchivo + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                '}';
    }
}

