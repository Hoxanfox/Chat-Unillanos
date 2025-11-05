package com.unillanos.server.dto;

/**
 * DTO para subir un archivo al servidor.
 */
public class DTOSubirArchivo {
    
    private String usuarioId;              // Requerido - Usuario que sube el archivo
    private String nombreArchivo;          // Requerido - Nombre original del archivo
    private String tipoMime;               // Requerido - Tipo MIME del archivo
    private long tamanoBytes;              // Requerido - Tamaño en bytes
    private String base64Data;             // Requerido - Contenido del archivo en Base64

    public DTOSubirArchivo() {
    }

    public DTOSubirArchivo(String usuarioId, String nombreArchivo, String tipoMime, 
                           long tamanoBytes, String base64Data) {
        this.usuarioId = usuarioId;
        this.nombreArchivo = nombreArchivo;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
        this.base64Data = base64Data;
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

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public void setBase64Data(String base64Data) {
        this.base64Data = base64Data;
    }

    @Override
    public String toString() {
        return "DTOSubirArchivo{" +
                "usuarioId='" + usuarioId + '\'' +
                ", nombreArchivo='" + nombreArchivo + '\'' +
                ", tipoMime='" + tipoMime + '\'' +
                ", tamanoBytes=" + tamanoBytes +
                ", base64Data='" + (base64Data != null && base64Data.length() > 50 ? 
                    base64Data.substring(0, 50) + "..." : base64Data) + '\'' +
                '}';
    }
}

