package com.unillanos.server.dto;

/**
 * DTO de respuesta al descargar un chunk de archivo.
 * Contiene el chunk específico y metadatos para el cliente.
 */
public class DTODescargarArchivoChunkResponse {
    
    private int numeroChunk;
    private int totalChunks;
    private String base64ChunkData;
    private String hashChunk;
    private boolean esUltimoChunk;

    // Constructor por defecto
    public DTODescargarArchivoChunkResponse() {}

    // Constructor con parámetros
    public DTODescargarArchivoChunkResponse(int numeroChunk, int totalChunks, 
                                          String base64ChunkData, String hashChunk, 
                                          boolean esUltimoChunk) {
        this.numeroChunk = numeroChunk;
        this.totalChunks = totalChunks;
        this.base64ChunkData = base64ChunkData;
        this.hashChunk = hashChunk;
        this.esUltimoChunk = esUltimoChunk;
    }

    // Getters y Setters
    public int getNumeroChunk() {
        return numeroChunk;
    }

    public void setNumeroChunk(int numeroChunk) {
        this.numeroChunk = numeroChunk;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public String getBase64ChunkData() {
        return base64ChunkData;
    }

    public void setBase64ChunkData(String base64ChunkData) {
        this.base64ChunkData = base64ChunkData;
    }

    public String getHashChunk() {
        return hashChunk;
    }

    public void setHashChunk(String hashChunk) {
        this.hashChunk = hashChunk;
    }

    public boolean isEsUltimoChunk() {
        return esUltimoChunk;
    }

    public void setEsUltimoChunk(boolean esUltimoChunk) {
        this.esUltimoChunk = esUltimoChunk;
    }

    @Override
    public String toString() {
        return "DTODescargarArchivoChunkResponse{" +
                "numeroChunk=" + numeroChunk +
                ", totalChunks=" + totalChunks +
                ", hashChunk='" + hashChunk + '\'' +
                ", esUltimoChunk=" + esUltimoChunk +
                ", base64ChunkDataLength=" + (base64ChunkData != null ? base64ChunkData.length() : 0) +
                '}';
    }
}
