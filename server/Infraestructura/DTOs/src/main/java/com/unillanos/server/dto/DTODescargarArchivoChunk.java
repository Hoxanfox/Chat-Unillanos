package com.unillanos.server.dto;

/**
 * DTO para solicitar la descarga de un chunk específico de un archivo.
 * Se usa para descargar archivos grandes por partes.
 */
public class DTODescargarArchivoChunk {
    
    private String usuarioId;
    private String archivoId;
    private int numeroChunk;

    // Constructor por defecto
    public DTODescargarArchivoChunk() {}

    // Constructor con parámetros
    public DTODescargarArchivoChunk(String usuarioId, String archivoId, int numeroChunk) {
        this.usuarioId = usuarioId;
        this.archivoId = archivoId;
        this.numeroChunk = numeroChunk;
    }

    // Getters y Setters
    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getArchivoId() {
        return archivoId;
    }

    public void setArchivoId(String archivoId) {
        this.archivoId = archivoId;
    }

    public int getNumeroChunk() {
        return numeroChunk;
    }

    public void setNumeroChunk(int numeroChunk) {
        this.numeroChunk = numeroChunk;
    }

    @Override
    public String toString() {
        return "DTODescargarArchivoChunk{" +
                "usuarioId='" + usuarioId + '\'' +
                ", archivoId='" + archivoId + '\'' +
                ", numeroChunk=" + numeroChunk +
                '}';
    }
}
