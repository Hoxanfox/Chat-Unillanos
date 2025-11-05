package com.unillanos.server.dto;

/**
 * DTO de respuesta para descarga de archivo con contenido en Base64.
 */
public class DTOArchivoData {
    
    private String id;
    private String nombreOriginal;
    private String tipoMime;
    private long tamanoBytes;
    private String base64Data;             // Contenido del archivo en Base64

    public DTOArchivoData() {
    }

    public DTOArchivoData(String id, String nombreOriginal, String tipoMime, 
                          long tamanoBytes, String base64Data) {
        this.id = id;
        this.nombreOriginal = nombreOriginal;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
        this.base64Data = base64Data;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
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
        return "DTOArchivoData{" +
                "id='" + id + '\'' +
                ", nombreOriginal='" + nombreOriginal + '\'' +
                ", tipoMime='" + tipoMime + '\'' +
                ", tamanoBytes=" + tamanoBytes +
                ", base64Data='" + (base64Data != null && base64Data.length() > 50 ? 
                    base64Data.substring(0, 50) + "..." : base64Data) + '\'' +
                '}';
    }
}

