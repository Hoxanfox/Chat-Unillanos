package com.arquitectura.utils.chunkManager;

public class FileUploadResponse {
        public String fileId;
        public String fileName;
        public long size;
        public String mimeType;
        public String hash;
        public FileUploadResponse(String id, String n, long s, String m, String h) {
            fileId=id; fileName=n; size=s; mimeType=m; hash=h;
        }
    }