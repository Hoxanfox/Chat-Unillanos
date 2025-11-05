package com.unillanos.server.dto;

/**
 * DTO para subir un chunk individual de un archivo.
 */
public class DTOSubirArchivoChunk {

    private String sessionId;
    private String usuarioId;
    private String nombreArchivo;
    private String tipoMime;
    private long tamanoTotal;
    private int numeroChunk;
    private int totalChunks;
    private String base64ChunkData;
    private String hashChunk;

    // Constructor por defecto
    public DTOSubirArchivoChunk() {}

    // Constructor con parámetros
    public DTOSubirArchivoChunk(String sessionId, String usuarioId, String nombreArchivo,
                                String tipoMime, long tamanoTotal, int numeroChunk,
                                int totalChunks, String base64ChunkData, String hashChunk) {
        this.sessionId = sessionId;
        this.usuarioId = usuarioId;
        this.nombreArchivo = nombreArchivo;
        this.tipoMime = tipoMime;
        this.tamanoTotal = tamanoTotal;
        this.numeroChunk = numeroChunk;
        this.totalChunks = totalChunks;
        this.base64ChunkData = base64ChunkData;
        this.hashChunk = hashChunk;
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

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(String tipoMime) {
        this.tipoMime = tipoMime;
    }

    public long getTamanoTotal() {
        return tamanoTotal;
    }

    public void setTamanoTotal(long tamanoTotal) {
        this.tamanoTotal = tamanoTotal;
    }

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

    @Override
    public String toString() {
        return "DTOSubirArchivoChunk{" +
                "sessionId='" + sessionId + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                ", nombreArchivo='" + nombreArchivo + '\'' +
                ", numeroChunk=" + numeroChunk +
                ", totalChunks=" + totalChunks +
                '}';
    }
}

