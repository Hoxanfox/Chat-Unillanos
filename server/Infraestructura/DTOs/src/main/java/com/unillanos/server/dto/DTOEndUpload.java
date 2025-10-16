package com.unillanos.server.dto;

/**
 * DTO para finalizar la subida de un archivo por chunks.
 * Se envía cuando se han subido todos los chunks del archivo.
 */
public class DTOEndUpload {
    
    private String sessionId;
    private String usuarioId;

    // Constructor por defecto
    public DTOEndUpload() {}

    // Constructor con parámetros
    public DTOEndUpload(String sessionId, String usuarioId) {
        this.sessionId = sessionId;
        this.usuarioId = usuarioId;
    }

    // Getters y Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    @Override
    public String toString() {
        return "DTOEndUpload{" +
                "sessionId='" + sessionId + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                '}';
    }
}
