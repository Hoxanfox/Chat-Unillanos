package com.arquitectura.utils.chunkManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class UploadState {
        String fileName;
        String tempDir;
        int totalChunks;
        Set<Integer> chunksReceived = ConcurrentHashMap.newKeySet();
        UploadState(String f, String t, int c) {
            this.fileName=f;
            this.tempDir=t;
            this.totalChunks=c;
        }
    }