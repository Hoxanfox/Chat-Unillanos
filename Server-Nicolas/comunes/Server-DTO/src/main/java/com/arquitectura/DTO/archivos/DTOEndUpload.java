package com.arquitectura.DTO.archivos;

// Cliente envía esto para finalizar la subida
public class DTOEndUpload {
    private String uploadId;
    private String fileHash_sha256; // Corrección: El DTO del cliente lo llama fileHash_sha256
    
    // Getters
    public String getUploadId() { return uploadId; }
    public String getFileHash_sha256() { return fileHash_sha256; }
}