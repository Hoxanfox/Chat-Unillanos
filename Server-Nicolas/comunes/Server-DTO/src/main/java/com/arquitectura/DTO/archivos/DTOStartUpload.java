package com.arquitectura.DTO.archivos;

// Cliente envía esto para iniciar una subida
public class DTOStartUpload {
    private String fileName;
    private String fileMimeType;
    private int totalChunks;
    
    // Getters
    public String getFileName() { return fileName; }
    public String getFileMimeType() { return fileMimeType; }
    public int getTotalChunks() { return totalChunks; }
}