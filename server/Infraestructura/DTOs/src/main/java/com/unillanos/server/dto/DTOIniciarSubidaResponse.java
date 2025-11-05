package com.unillanos.server.dto;

import java.util.List;

/**
 * DTO de respuesta al iniciar una subida de archivo por chunks.
 * Contiene la información necesaria para continuar con la subida.
 */
public class DTOIniciarSubidaResponse {
    
    private String sessionId;
    private int chunkSize;
    private List<Integer> chunksRecibidos;

    // Constructor por defecto
    public DTOIniciarSubidaResponse() {}

    // Constructor con parámetros
    public DTOIniciarSubidaResponse(String sessionId, int chunkSize, List<Integer> chunksRecibidos) {
        this.sessionId = sessionId;
        this.chunkSize = chunkSize;
        this.chunksRecibidos = chunksRecibidos;
    }

    // Getters y Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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
        return "DTOIniciarSubidaResponse{" +
                "sessionId='" + sessionId + '\'' +
                ", chunkSize=" + chunkSize +
                ", chunksRecibidos=" + chunksRecibidos +
                '}';
    }
}
