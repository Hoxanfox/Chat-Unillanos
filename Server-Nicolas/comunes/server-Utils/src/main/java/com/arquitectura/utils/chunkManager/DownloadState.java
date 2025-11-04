package com.arquitectura.utils.chunkManager;

class DownloadState {
    String filePath;
    long fileSize;
    int totalChunks;

    DownloadState(String p, long s, int c) {
        this.filePath=p;
        this.fileSize=s;
        this.totalChunks=c;
    }
    }