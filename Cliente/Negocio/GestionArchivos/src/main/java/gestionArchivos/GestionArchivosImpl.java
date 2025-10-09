package gestionArchivos;

import com.google.gson.Gson;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.gestionArchivos.DTOEndUpload;
import dto.gestionArchivos.DTOStartUpload;
import dto.gestionArchivos.DTOUploadChunk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del componente de negocio que gestiona la subida de archivos por chunks.
 */
public class GestionArchivosImpl implements IGestionArchivos {

    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final Gson gson;
    private static final int CHUNK_SIZE = 1024 * 512; // 512 KB por chunk

    public GestionArchivosImpl() {
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = new GestorRespuesta();
        this.gson = new Gson();
    }

    @Override
    public CompletableFuture<String> subirArchivo(File archivo) {
        CompletableFuture<String> futuroSubida = new CompletableFuture<>();

        try {
            // FASE 0: PREPARACIÓN
            byte[] fileBytes = Files.readAllBytes(archivo.toPath());
            String fileHash = calcularHashSHA256(fileBytes);
            int totalChunks = (int) Math.ceil((double) fileBytes.length / CHUNK_SIZE);

            // FASE 1: INICIO DE LA SUBIDA
            iniciarSubida(archivo, totalChunks)
                    .thenCompose(uploadId -> {
                        // FASE 2: TRANSFERENCIA DE CHUNKS
                        return transferirChunks(uploadId, fileBytes, totalChunks);
                    })
                    .thenCompose(uploadId -> {
                        // FASE 3: FINALIZACIÓN DE LA SUBIDA
                        return finalizarSubida(uploadId, fileHash);
                    })
                    .thenAccept(fileName -> futuroSubida.complete(fileName)) // Éxito
                    .exceptionally(ex -> {
                        futuroSubida.completeExceptionally(ex); // Falla
                        return null;
                    });

        } catch (Exception e) {
            futuroSubida.completeExceptionally(e);
        }

        return futuroSubida;
    }

    private CompletableFuture<String> iniciarSubida(File archivo, int totalChunks) {
        CompletableFuture<String> futuroUploadId = new CompletableFuture<>();
        final String ACCION = "startFileUpload";

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse res) -> {
            if (res.fueExitoso()) {
                // El campo 'data' debería contener un objeto con el 'uploadId'
                String uploadId = gson.fromJson(gson.toJson(res.getData()), UploadIdResponse.class).uploadId;
                futuroUploadId.complete(uploadId);
            } else {
                futuroUploadId.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTOStartUpload payload = new DTOStartUpload(archivo.getName(), getMimeType(archivo), totalChunks);
        enviadorPeticiones.enviar(new DTORequest(ACCION, payload));
        return futuroUploadId;
    }

    private CompletableFuture<String> transferirChunks(String uploadId, byte[] fileBytes, int totalChunks) {
        CompletableFuture<String> futuroTransferencia = new CompletableFuture<>();
        CompletableFuture<Void> futuroChunkActual = CompletableFuture.completedFuture(null);

        for (int i = 0; i < totalChunks; i++) {
            final int chunkNumber = i + 1;
            int offset = i * CHUNK_SIZE;
            int length = Math.min(CHUNK_SIZE, fileBytes.length - offset);
            byte[] chunkData = new byte[length];
            System.arraycopy(fileBytes, offset, chunkData, 0, length);
            String chunkBase64 = Base64.getEncoder().encodeToString(chunkData);

            futuroChunkActual = futuroChunkActual.thenCompose(v -> enviarChunk(uploadId, chunkNumber, chunkBase64));
        }

        futuroChunkActual.thenRun(() -> futuroTransferencia.complete(uploadId))
                .exceptionally(ex -> {
                    futuroTransferencia.completeExceptionally(ex);
                    return null;
                });

        return futuroTransferencia;
    }

    private CompletableFuture<Void> enviarChunk(String uploadId, int chunkNumber, String chunkBase64) {
        CompletableFuture<Void> futuroChunk = new CompletableFuture<>();
        final String ACCION = "uploadFileChunk" + "_" + chunkNumber; // Acción única para cada chunk

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse res) -> {
            if (res.fueExitoso()) {
                futuroChunk.complete(null);
            } else {
                futuroChunk.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTOUploadChunk payload = new DTOUploadChunk(uploadId, chunkNumber, chunkBase64);
        // Usamos una acción diferente en la petición para evitar colisiones
        enviadorPeticiones.enviar(new DTORequest("uploadFileChunk", payload));
        return futuroChunk;
    }

    private CompletableFuture<String> finalizarSubida(String uploadId, String fileHash) {
        CompletableFuture<String> futuroFinal = new CompletableFuture<>();
        final String ACCION = "endFileUpload";

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse res) -> {
            if (res.fueExitoso()) {
                // Suponemos que la respuesta devuelve el nombre final del archivo
                String fileName = gson.fromJson(gson.toJson(res.getData()), FileNameResponse.class).fileName;
                futuroFinal.complete(fileName);
            } else {
                futuroFinal.completeExceptionally(new RuntimeException(res.getMessage()));
            }
        });

        DTOEndUpload payload = new DTOEndUpload(uploadId, fileHash);
        enviadorPeticiones.enviar(new DTORequest(ACCION, payload));
        return futuroFinal;
    }

    // --- Métodos de utilidad ---

    private String getMimeType(File file) {
        try {
            return Files.probeContentType(file.toPath());
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private String calcularHashSHA256(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(fileBytes);
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo calcular el hash del archivo", e);
        }
    }

    // Clases auxiliares para parsear respuestas JSON simples
    private static class UploadIdResponse { String uploadId; }
    private static class FileNameResponse { String fileName; }
}
