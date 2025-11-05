package com.arquitectura.utils.chunkManager;

import com.arquitectura.DTO.archivos.DTODownloadInfo;
import com.arquitectura.DTO.archivos.DTOEndUpload;
import com.arquitectura.DTO.archivos.DTOStartUpload;
import com.arquitectura.DTO.archivos.DTOUploadChunk;
import com.arquitectura.utils.file.IFileStorageService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FileChunkManager {
    private final Map<String, UploadState> activeUploads = new ConcurrentHashMap<>();
    private final Map<String, DownloadState> activeDownloads = new ConcurrentHashMap<>();
    private final IFileStorageService fileStorage;
    private final Path tempUploadDir = Paths.get("storage", "temp_uploads");
    private final int CHUNK_SIZE_BYTES = 512 * 1024;

    public FileChunkManager(IFileStorageService fileStorage) throws IOException {
        this.fileStorage = fileStorage;
        Files.createDirectories(tempUploadDir);
    }

    public String startUpload(DTOStartUpload dto) throws Exception {
        String uploadId = "upload-" + UUID.randomUUID().toString();
        Path uploadPath = tempUploadDir.resolve(uploadId);
        Files.createDirectories(uploadPath);

        UploadState state = new UploadState(dto.getFileName(), uploadPath.toString(), dto.getTotalChunks());
        activeUploads.put(uploadId, state);

        System.out.println("[Server] Iniciando subida: " + uploadId + " para " + dto.getFileName());
        return uploadId;
    }

    public void processChunk(DTOUploadChunk dto) throws Exception {
        UploadState state = activeUploads.get(dto.getUploadId());
        if (state == null) {
            throw new Exception("Upload ID inválido: " + dto.getUploadId());
        }
        byte[] data = Base64.getDecoder().decode(dto.getChunkData_base64());
        Path chunkFile = Paths.get(state.tempDir, dto.getChunkNumber() + ".chunk");
        Files.write(chunkFile, data);

        state.chunksReceived.add(dto.getChunkNumber());
        System.out.println("[Server] Recibido chunk " + state.chunksReceived.size() + "/" + state.totalChunks + " para " + dto.getUploadId());
    }

    public FileUploadResponse endUpload(DTOEndUpload dto, UUID autorId, String subDirectory) throws Exception {
        UploadState state = activeUploads.get(dto.getUploadId());
        if (state == null) {
            throw new Exception("Upload ID inválido: " + dto.getUploadId());
        }
        if (state.chunksReceived.size() != state.totalChunks) {
            throw new Exception("Chunks incompletos: " + state.chunksReceived.size() + "/" + state.totalChunks);
        }

        // 1. Ensamblar el archivo final en un archivo temporal
        Path tempFile = Files.createTempFile(tempUploadDir, "final-", state.fileName);
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            for (int i = 1; i <= state.totalChunks; i++) {
                byte[] chunkData = Files.readAllBytes(Paths.get(state.tempDir, i + ".chunk"));
                fos.write(chunkData);
            }
        }
        String extension = "";
        int i = state.fileName.lastIndexOf('.');
        if (i > 0) extension = state.fileName.substring(i);
        String newFileName = autorId.toString() + "_" + System.currentTimeMillis() + extension;

        // Corregido: Leer el archivo temporal como bytes
        byte[] fileBytes = Files.readAllBytes(tempFile);
        String relativePath = fileStorage.storeFile(fileBytes, newFileName, subDirectory);

        long fileSize = Files.size(tempFile);
        String mimeType = Files.probeContentType(tempFile);

        // 3. Limpiar archivos temporales
        cleanUpDirectory(Paths.get(state.tempDir)); // Borra el directorio de chunks
        activeUploads.remove(dto.getUploadId());   // Limpia el mapa
        Files.delete(tempFile);                     // Borra el archivo temporal ensamblado

        System.out.println("[Server] Subida finalizada: " + relativePath);

        return new FileUploadResponse(
                relativePath, // ¡Este es el 'fileId' que el cliente usará!
                state.fileName,
                fileSize,
                mimeType,
                dto.getFileHash_sha256() // Corrección: usa el DTO del cliente
        );
    }

    public DTODownloadInfo startDownload(String fileId) throws Exception {
        // fileId es la ruta relativa, ej: "user_photos/juan.jpg"
        Path filePath = Paths.get("storage").resolve(fileId).normalize();
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new Exception("Archivo no encontrado o no legible: " + fileId);
        }

        long fileSize = Files.size(filePath);
        int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE_BYTES);

        String downloadId = "download-" + UUID.randomUUID().toString();
        DownloadState state = new DownloadState(fileId, fileSize, totalChunks);
        activeDownloads.put(downloadId, state);

        return new DTODownloadInfo(
                downloadId,
                filePath.getFileName().toString(),
                fileSize,
                totalChunks,
                Files.probeContentType(filePath)
        );
    }

    public byte[] getChunk(String downloadId, int chunkNumber) throws Exception {
        DownloadState state = activeDownloads.get(downloadId);
        if (state == null) throw new Exception("Download ID inválido");

        long offset = (long) (chunkNumber - 1) * CHUNK_SIZE_BYTES;

        return fileStorage.readChunk(state.filePath, offset, CHUNK_SIZE_BYTES);
    }

    private void cleanUpDirectory(Path directory) throws IOException {
        Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
