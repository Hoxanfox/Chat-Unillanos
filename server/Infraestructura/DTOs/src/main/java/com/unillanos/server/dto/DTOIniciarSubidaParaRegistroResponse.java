package com.unillanos.server.dto;

import java.util.List;

/**
 * DTO de respuesta al iniciar una subida de archivo por chunks durante el registro.
 * Incluye tanto sessionId como uploadId para compatibilidad con el cliente.
 */
public class DTOIniciarSubidaParaRegistroResponse {
    
    private String sessionId;
    private String uploadId; // Alias para sessionId para compatibilidad con el cliente
    private int chunkSize;
    private List<Integer> chunksRecibidos;

    // Constructor por defecto
    public DTOIniciarSubidaParaRegistroResponse() {}

    // Constructor con parámetros
    public DTOIniciarSubidaParaRegistroResponse(String sessionId, int chunkSize, List<Integer> chunksRecibidos) {
        this.sessionId = sessionId;
        this.uploadId = sessionId; // El uploadId del cliente es el sessionId del servidor
        this.chunkSize = chunkSize;
        this.chunksRecibidos = chunksRecibidos;
    }

    // Getters y Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
        this.uploadId = sessionId; // Mantener sincronizados
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
        this.sessionId = uploadId; // Mantener sincronizados
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public List<Integer> getChunksRecibidos() {
        return chunksRecibidos;
    }

    public void setChunksRecibidos(List<Integer> chunksRecibidos) {
        this.chunksRecibidos = chunksRecibidos;
    }

    @Override
    public String toString() {
        return "DTOIniciarSubidaParaRegistroResponse{" +
                "sessionId='" + sessionId + '\'' +
                ", uploadId='" + uploadId + '\'' +
                ", chunkSize=" + chunkSize +
                ", chunksRecibidos=" + chunksRecibidos +
                '}';
    }
}
