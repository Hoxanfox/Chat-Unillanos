package com.arquitectura.utils.file;

import java.io.IOException;

public interface IFileStorageService {
    String readFileAsBase64(String filePath) throws Exception;
    String storeFile(byte[] fileData, String newFileName, String subDirectory) throws IOException;
}

