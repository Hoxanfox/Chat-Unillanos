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
        String uploadId = "upload-" + UUID.randomUUID();
        Path uploadPath = tempUploadDir.resolve(uploadId);
        Files.createDirectories(uploadPath);

        UploadState state = new UploadState(dto.getFileName(), uploadPath.toString(), dto.getTotalChunks());
        activeUploads.put(uploadId, state);

        System.out.println("\n[CHUNK MANAGER] ========== INICIO UPLOAD ==========");
        System.out.println("Upload ID: " + uploadId);
        System.out.println("Archivo: " + dto.getFileName());
        System.out.println("Total chunks: " + dto.getTotalChunks());
        System.out.println("MIME Type: " + dto.getFileMimeType());
        System.out.println("==================================================\n");

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

        System.out.println("[CHUNK MANAGER] Chunk recibido: " + dto.getChunkNumber() + "/" + state.totalChunks +
                          " | Upload ID: " + dto.getUploadId() + " | Tamaño: " + data.length + " bytes");
    }

    public FileUploadResponse endUpload(DTOEndUpload dto, UUID autorId, String subDirectory) throws Exception {
        UploadState state = activeUploads.get(dto.getUploadId());
        if (state == null) {
            throw new Exception("Upload ID inválido: " + dto.getUploadId());
        }
        if (state.chunksReceived.size() != state.totalChunks) {
            throw new Exception("Chunks incompletos: " + state.chunksReceived.size() + "/" + state.totalChunks);
        }

        System.out.println("\n[CHUNK MANAGER] ========== FINALIZANDO UPLOAD ==========");
        System.out.println("Upload ID: " + dto.getUploadId());
        System.out.println("Ensamblando " + state.totalChunks + " chunks...");

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

        System.out.println("Archivo guardado: " + relativePath);
        System.out.println("Tamaño final: " + fileSize + " bytes");
        System.out.println("MIME Type: " + mimeType);
        System.out.println("======================================================\n");

        // 3. Limpiar archivos temporales
        cleanUpDirectory(Paths.get(state.tempDir)); // Borra el directorio de chunks
        activeUploads.remove(dto.getUploadId());   // Limpia el mapa
        Files.delete(tempFile);                     // Borra el archivo temporal ensamblado

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

        String downloadId = "download-" + UUID.randomUUID();
        DownloadState state = new DownloadState(fileId, fileSize, totalChunks);
        activeDownloads.put(downloadId, state);

        System.out.println("\n[CHUNK MANAGER] ========== INICIO DOWNLOAD ==========");
        System.out.println("Download ID: " + downloadId);
        System.out.println("File ID: " + fileId);
        System.out.println("Archivo: " + filePath.getFileName());
        System.out.println("Tamaño: " + fileSize + " bytes");
        System.out.println("Total chunks: " + totalChunks);
        System.out.println("=====================================================\n");

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

        byte[] chunkData = fileStorage.readChunk(state.filePath, offset, CHUNK_SIZE_BYTES);

        System.out.println("[CHUNK MANAGER] Enviando chunk: " + chunkNumber + "/" + state.totalChunks +
                          " | Download ID: " + downloadId + " | Tamaño: " + chunkData.length + " bytes");

        return chunkData;
    }

    private void cleanUpDirectory(Path directory) throws IOException {
        Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
