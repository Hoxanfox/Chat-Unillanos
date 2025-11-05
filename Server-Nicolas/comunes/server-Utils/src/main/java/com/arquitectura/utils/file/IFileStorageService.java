package com.arquitectura.utils.file;

import java.io.File;
import java.io.IOException;

public interface IFileStorageService {
    String readFileAsBase64(String filePath) throws Exception;
    String storeFile(byte[] fileData, String newFileName, String subDirectory) throws IOException;
    byte[] readChunk(String relativePath, long offset, int length) throws IOException;
    public String storeFile(File sourceFile, String newFileName, String subDirectory) throws IOException;
}

