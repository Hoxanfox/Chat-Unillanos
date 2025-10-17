package com.unillanos.server.entity;

import com.unillanos.server.dto.DTOArchivo;

import java.time.LocalDateTime;

/**
 * Entidad que representa un archivo multimedia en la base de datos.
 */
public class ArchivoEntity {
    
    private String id;                      // UUID
    private String nombreOriginal;          // Nombre original del archivo
    private String nombreAlmacenado;        // Nombre UUID en disco
    private String tipoMime;                // Tipo MIME
    private TipoArchivo tipoArchivo;        // IMAGE, AUDIO, DOCUMENT
    private String hashSha256;              // Hash SHA-256 para deduplicación
    private long tamanoBytes;               // Tamaño en bytes
    private String rutaAlmacenamiento;      // Ruta relativa en el sistema de archivos
    private String usuarioId;               // Usuario que subió el archivo
    private LocalDateTime fechaSubida;

    // Constructores
    public ArchivoEntity() {
    }

    public ArchivoEntity(String id, String nombreOriginal, String nombreAlmacenado,
                         String tipoMime, TipoArchivo tipoArchivo, String hashSha256,
                         long tamanoBytes, String rutaAlmacenamiento, String usuarioId,
                         LocalDateTime fechaSubida) {
        this.id = id;
        this.nombreOriginal = nombreOriginal;
        this.nombreAlmacenado = nombreAlmacenado;
        this.tipoMime = tipoMime;
        this.tipoArchivo = tipoArchivo;
        this.hashSha256 = hashSha256;
        this.tamanoBytes = tamanoBytes;
        this.rutaAlmacenamiento = rutaAlmacenamiento;
        this.usuarioId = usuarioId;
        this.fechaSubida = fechaSubida;
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

    public String getNombreAlmacenado() {
        return nombreAlmacenado;
    }

    public void setNombreAlmacenado(String nombreAlmacenado) {
        this.nombreAlmacenado = nombreAlmacenado;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(String tipoMime) {
        this.tipoMime = tipoMime;
    }

    public TipoArchivo getTipoArchivo() {
        return tipoArchivo;
    }

    public void setTipoArchivo(TipoArchivo tipoArchivo) {
        this.tipoArchivo = tipoArchivo;
    }

    public String getHashSha256() {
        return hashSha256;
    }

    public void setHashSha256(String hashSha256) {
        this.hashSha256 = hashSha256;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public String getRutaAlmacenamiento() {
        return rutaAlmacenamiento;
    }

    public void setRutaAlmacenamiento(String rutaAlmacenamiento) {
        this.rutaAlmacenamiento = rutaAlmacenamiento;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(LocalDateTime fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    /**
     * Convierte la entidad a DTO.
     *
     * @param esDuplicado Indica si el archivo es duplicado
     * @return DTOArchivo con la información del archivo
     */
    public DTOArchivo toDTO(boolean esDuplicado) {
        DTOArchivo dto = new DTOArchivo();
        dto.setId(this.id);
        dto.setNombreOriginal(this.nombreOriginal);
        dto.setTipoMime(this.tipoMime);
        dto.setTipoArchivo(this.tipoArchivo != null ? this.tipoArchivo.name() : null);
        dto.setTamanoBytes(this.tamanoBytes);
        dto.setHashSha256(this.hashSha256);
        dto.setDuplicado(esDuplicado);
        dto.setUsuarioId(this.usuarioId);
        dto.setFechaSubida(this.fechaSubida != null ? this.fechaSubida.toString() : null);
        dto.setUrlDescarga("/api/archivos/descargar/" + this.id);
        return dto;
    }

    @Override
    public String toString() {
        return "ArchivoEntity{" +
                "id='" + id + '\'' +
                ", nombreOriginal='" + nombreOriginal + '\'' +
                ", nombreAlmacenado='" + nombreAlmacenado + '\'' +
                ", tipoMime='" + tipoMime + '\'' +
                ", tipoArchivo=" + tipoArchivo +
                ", hashSha256='" + hashSha256 + '\'' +
                ", tamanoBytes=" + tamanoBytes +
                ", rutaAlmacenamiento='" + rutaAlmacenamiento + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                ", fechaSubida=" + fechaSubida +
                '}';
    }
}
