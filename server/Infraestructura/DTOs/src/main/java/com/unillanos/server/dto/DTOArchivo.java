package com.unillanos.server.dto;

/**
 * DTO de respuesta con información de un archivo.
 */
public class DTOArchivo {
    
    private String id;                     // UUID del archivo
    private String nombreOriginal;         // Nombre original del archivo
    private String tipoMime;               // Tipo MIME
    private String tipoArchivo;            // "IMAGEN", "AUDIO", "DOCUMENTO"
    private long tamanoBytes;              // Tamaño en bytes
    private String hashSha256;             // Hash SHA-256 del archivo
    private boolean duplicado;             // Si es un archivo duplicado
    private String usuarioId;              // Usuario que subió el archivo
    private String fechaSubida;            // ISO-8601
    private String urlDescarga;            // URL para descargar el archivo

    public DTOArchivo() {
    }

    public DTOArchivo(String id, String nombreOriginal, String tipoMime, String tipoArchivo,
                      long tamanoBytes, String hashSha256, boolean duplicado, String usuarioId,
                      String fechaSubida, String urlDescarga) {
        this.id = id;
        this.nombreOriginal = nombreOriginal;
        this.tipoMime = tipoMime;
        this.tipoArchivo = tipoArchivo;
        this.tamanoBytes = tamanoBytes;
        this.hashSha256 = hashSha256;
        this.duplicado = duplicado;
        this.usuarioId = usuarioId;
        this.fechaSubida = fechaSubida;
        this.urlDescarga = urlDescarga;
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

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public void setTipoArchivo(String tipoArchivo) {
        this.tipoArchivo = tipoArchivo;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public String getHashSha256() {
        return hashSha256;
    }

    public void setHashSha256(String hashSha256) {
        this.hashSha256 = hashSha256;
    }

    public boolean isDuplicado() {
        return duplicado;
    }

    public void setDuplicado(boolean duplicado) {
        this.duplicado = duplicado;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(String fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public String getUrlDescarga() {
        return urlDescarga;
    }

    public void setUrlDescarga(String urlDescarga) {
        this.urlDescarga = urlDescarga;
    }

    @Override
    public String toString() {
        return "DTOArchivo{" +
                "id='" + id + '\'' +
                ", nombreOriginal='" + nombreOriginal + '\'' +
                ", tipoMime='" + tipoMime + '\'' +
                ", tipoArchivo='" + tipoArchivo + '\'' +
                ", tamanoBytes=" + tamanoBytes +
                ", hashSha256='" + hashSha256 + '\'' +
                ", duplicado=" + duplicado +
                ", usuarioId='" + usuarioId + '\'' +
                ", fechaSubida='" + fechaSubida + '\'' +
                ", urlDescarga='" + urlDescarga + '\'' +
                '}';
    }
}

