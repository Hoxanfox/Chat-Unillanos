package com.arquitectura.DTO.archivos;

// Cliente envía esto para cada chunk
public class DTOUploadChunk {
    private String uploadId;
    private int chunkNumber;
    private String chunkData_base64; // El "trozo" de archivo
    
    // Getters
    public String getUploadId() { return uploadId; }
    public int getChunkNumber() { return chunkNumber; }
    public String getChunkData_base64() { return chunkData_base64; }
}