package com.arquitectura.fachada.archivo;

import com.arquitectura.DTO.archivos.DTODownloadInfo;
import com.arquitectura.DTO.archivos.DTOEndUpload;
import com.arquitectura.DTO.archivos.DTOStartUpload;
import com.arquitectura.DTO.archivos.DTOUploadChunk;
import com.arquitectura.utils.chunkManager.FileChunkManager;
import com.arquitectura.utils.chunkManager.FileUploadResponse;
import com.arquitectura.utils.file.IFileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Implementación de la fachada de archivos.
 * Coordina las operaciones de transferencia de archivos del sistema.
 */
@Component
public class ArchivoFachadaImpl implements IArchivoFachada {

    private final FileChunkManager fileChunkManager;
    private final IFileStorageService fileStorageService;

    @Autowired
    public ArchivoFachadaImpl(FileChunkManager fileChunkManager, IFileStorageService fileStorageService) {
        this.fileChunkManager = fileChunkManager;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public String startUpload(DTOStartUpload dto) throws Exception {
        return fileChunkManager.startUpload(dto);
    }

    @Override
    public void processChunk(DTOUploadChunk dto) throws Exception {
        fileChunkManager.processChunk(dto);
    }

    @Override
    public FileUploadResponse endUpload(DTOEndUpload dto, UUID autorId, String subDirectory) throws Exception {
        return fileChunkManager.endUpload(dto, autorId, subDirectory);
    }

    @Override
    public DTODownloadInfo startDownload(String fileId) throws Exception {
        return fileChunkManager.startDownload(fileId);
    }

    @Override
    public byte[] getChunk(String downloadId, int chunkNumber) throws Exception {
        return fileChunkManager.getChunk(downloadId, chunkNumber);
    }

    @Override
    public String getFileAsBase64(String relativePath) throws Exception {
        return fileStorageService.readFileAsBase64(relativePath);
    }
}

