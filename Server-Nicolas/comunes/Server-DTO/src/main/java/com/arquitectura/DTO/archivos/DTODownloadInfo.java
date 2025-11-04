package com.arquitectura.DTO.archivos;

// Servidor envía esto como respuesta a "startFileDownload"
public class DTODownloadInfo {
    private String downloadId;
    private String fileName;
    private long fileSize;
    private int totalChunks;
    private String mimeType;

    public DTODownloadInfo(String id, String name, long size, int chunks, String mime) {
        this.downloadId = id;
        this.fileName = name;
        this.fileSize = size;
        this.totalChunks = chunks;
        this.mimeType = mime;
    }
    // (No necesitas setters, Gson los usa)
}