package com.unillanos.server.dto;

/**
 * DTO para iniciar una sesión de subida de archivo por chunks.
 * Se envía al servidor antes de comenzar a subir los chunks individuales.
 */
public class DTOIniciarSubida {
    
    private String usuarioId;
    private String nombreArchivo;
    private String tipoMime;
    private long tamanoTotal;
    private int totalChunks;

    // Constructor por defecto
    public DTOIniciarSubida() {}

    // Constructor con parámetros
    public DTOIniciarSubida(String usuarioId, String nombreArchivo, String tipoMime, 
                           long tamanoTotal, int totalChunks) {
        this.usuarioId = usuarioId;
        this.nombreArchivo = nombreArchivo;
        this.tipoMime = tipoMime;
        this.tamanoTotal = tamanoTotal;
        this.totalChunks = totalChunks;
    }

    // Getters y Setters
    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(String tipoMime) {
        this.tipoMime = tipoMime;
    }

    public long getTamanoTotal() {
        return tamanoTotal;
    }

    public void setTamanoTotal(long tamanoTotal) {
        this.tamanoTotal = tamanoTotal;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    @Override
    public String toString() {
        return "DTOIniciarSubida{" +
                "usuarioId='" + usuarioId + '\'' +
                ", nombreArchivo='" + nombreArchivo + '\'' +
                ", tipoMime='" + tipoMime + '\'' +
                ", tamanoTotal=" + tamanoTotal +
                ", totalChunks=" + totalChunks +
                '}';
    }
}
