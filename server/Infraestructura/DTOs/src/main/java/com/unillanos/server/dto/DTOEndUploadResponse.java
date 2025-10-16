package com.unillanos.server.dto;

/**
 * DTO de respuesta al finalizar la subida de un archivo.
 * Contiene información sobre el archivo subido exitosamente.
 */
public class DTOEndUploadResponse {
    
    private String fileId;
    private String fileName;
    private long fileSize;
    private String fileHash;
    private String fileMimeType;

    // Constructor por defecto
    public DTOEndUploadResponse() {}

    // Constructor con parámetros
    public DTOEndUploadResponse(String fileId, String fileName, long fileSize, String fileHash, String fileMimeType) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileHash = fileHash;
        this.fileMimeType = fileMimeType;
    }

    // Getters y Setters
    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getFileMimeType() {
        return fileMimeType;
    }

    public void setFileMimeType(String fileMimeType) {
        this.fileMimeType = fileMimeType;
    }

    @Override
    public String toString() {
        return "DTOEndUploadResponse{" +
                "fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", fileHash='" + fileHash + '\'' +
                ", fileMimeType='" + fileMimeType + '\'' +
                '}';
    }
}
