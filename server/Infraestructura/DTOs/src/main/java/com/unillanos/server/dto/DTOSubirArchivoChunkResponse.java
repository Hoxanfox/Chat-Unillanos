package com.unillanos.server.dto;

/**
 * DTO de respuesta al subir un chunk de archivo.
 * Contiene información sobre el progreso de la subida.
 */
public class DTOSubirArchivoChunkResponse {
    
    private int chunkNumber;
    private int totalChunks;
    private int chunksReceived;
    private boolean isComplete;

    // Constructor por defecto
    public DTOSubirArchivoChunkResponse() {}

    // Constructor con parámetros
    public DTOSubirArchivoChunkResponse(int chunkNumber, int totalChunks, int chunksReceived, boolean isComplete) {
        this.chunkNumber = chunkNumber;
        this.totalChunks = totalChunks;
        this.chunksReceived = chunksReceived;
        this.isComplete = isComplete;
    }

    // Getters y Setters
    public int getChunkNumber() {
        return chunkNumber;
    }

    public void setChunkNumber(int chunkNumber) {
        this.chunkNumber = chunkNumber;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public int getChunksReceived() {
        return chunksReceived;
    }

    public void setChunksReceived(int chunksReceived) {
        this.chunksReceived = chunksReceived;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    @Override
    public String toString() {
        return "DTOSubirArchivoChunkResponse{" +
                "chunkNumber=" + chunkNumber +
                ", totalChunks=" + totalChunks +
                ", chunksReceived=" + chunksReceived +
                ", isComplete=" + isComplete +
                '}';
    }
}
