package com.unillanos.server.dto;

/**
 * DTO para descargar un archivo.
 */
public class DTODescargarArchivo {
    
    private String archivoId;              // Requerido - ID del archivo
    private String usuarioId;              // Requerido - Usuario que descarga

    public DTODescargarArchivo() {
    }

    public DTODescargarArchivo(String archivoId, String usuarioId) {
        this.archivoId = archivoId;
        this.usuarioId = usuarioId;
    }

    // Getters y Setters
    public String getArchivoId() {
        return archivoId;
    }

    public void setArchivoId(String archivoId) {
        this.archivoId = archivoId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    @Override
    public String toString() {
        return "DTODescargarArchivo{" +
                "archivoId='" + archivoId + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                '}';
    }
}

