package com.arquitectura.logicaPeers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.domain.Peer;
import com.arquitectura.persistence.repository.PeerRepository;
import com.arquitectura.utils.file.IFileStorageService;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Servicio para transferir archivos de audio entre servidores P2P usando chunks.
 */
@Service
public class AudioFileP2PService {

    private static final Logger log = LoggerFactory.getLogger(AudioFileP2PService.class);
    private static final int CHUNK_SIZE = 512 * 1024; // 512KB por chunk
    private static final int CONNECTION_TIMEOUT_MS = 10000;

    private final PeerRepository peerRepository;
    private final IFileStorageService fileStorageService;
    private final Gson gson;

    // Almacenamiento temporal de chunks recibidos
    private final Map<String, Map<Integer, byte[]>> chunksEnTransito = new HashMap<>();
    private final Map<String, AudioFileMetadata> metadataEnTransito = new HashMap<>();

    @Autowired
    public AudioFileP2PService(PeerRepository peerRepository,
                               IFileStorageService fileStorageService,
                               Gson gson) {
        this.peerRepository = peerRepository;
        this.fileStorageService = fileStorageService;
        this.gson = gson;
    }

    /**
     * Transfiere un archivo de audio a otro servidor peer usando chunks.
     *
     * @param peerDestinoId ID del servidor destino
     * @param rutaArchivoLocal Ruta local del archivo (ej: "audio_files/uuid_timestamp.wav")
     * @return Ruta donde el servidor destino guardó el archivo, o null si falla
     */
    public String transferirArchivoAudio(UUID peerDestinoId, String rutaArchivoLocal) {
        log.info("→ [AudioFileP2PService] Iniciando transferencia de audio a peer {}: {}",
                 peerDestinoId, rutaArchivoLocal);

        try {
            // 1. Obtener información del peer destino
            Optional<Peer> peerOpt = peerRepository.findById(peerDestinoId);
            if (peerOpt.isEmpty()) {
                log.error("✗ Peer destino no encontrado: {}", peerDestinoId);
                return null;
            }
            Peer peer = peerOpt.get();

            // 2. Leer el archivo local
            Path archivoPath = Paths.get("storage", rutaArchivoLocal);
            if (!Files.exists(archivoPath)) {
                log.error("✗ Archivo no encontrado: {}", archivoPath);
                return null;
            }

            byte[] archivoCompleto = Files.readAllBytes(archivoPath);
            String nombreArchivo = archivoPath.getFileName().toString();

            // 3. Iniciar la transferencia
            String transferId = UUID.randomUUID().toString();
            int totalChunks = (int) Math.ceil((double) archivoCompleto.length / CHUNK_SIZE);

            Map<String, Object> metadataPayload = new HashMap<>();
            metadataPayload.put("transferId", transferId);
            metadataPayload.put("fileName", nombreArchivo);
            metadataPayload.put("fileSize", archivoCompleto.length);
            metadataPayload.put("totalChunks", totalChunks);
            metadataPayload.put("originalPath", rutaArchivoLocal);

            DTORequest inicioRequest = new DTORequest("iniciarTransferenciaAudio", metadataPayload);
            DTOResponse inicioResponse = enviarPeticionAPeer(peer, inicioRequest);

            if (inicioResponse == null || !"success".equals(inicioResponse.getStatus())) {
                log.error("✗ Error al iniciar transferencia en peer destino");
                return null;
            }

            log.info("→ Transferencia iniciada, enviando {} chunks...", totalChunks);

            // 4. Enviar chunks
            for (int i = 0; i < totalChunks; i++) {
                int offset = i * CHUNK_SIZE;
                int length = Math.min(CHUNK_SIZE, archivoCompleto.length - offset);
                byte[] chunk = Arrays.copyOfRange(archivoCompleto, offset, offset + length);
                String chunkBase64 = Base64.getEncoder().encodeToString(chunk);

                Map<String, Object> chunkPayload = new HashMap<>();
                chunkPayload.put("transferId", transferId);
                chunkPayload.put("chunkNumber", i);
                chunkPayload.put("chunkData", chunkBase64);

                DTORequest chunkRequest = new DTORequest("recibirChunkAudio", chunkPayload);
                DTOResponse chunkResponse = enviarPeticionAPeer(peer, chunkRequest);

                if (chunkResponse == null || !"success".equals(chunkResponse.getStatus())) {
                    log.error("✗ Error al enviar chunk {}/{}", i + 1, totalChunks);
                    return null;
                }

                if ((i + 1) % 10 == 0 || i == totalChunks - 1) {
                    log.info("  → Progreso: {}/{} chunks enviados", i + 1, totalChunks);
                }
            }

            // 5. Finalizar transferencia
            Map<String, Object> finPayload = new HashMap<>();
            finPayload.put("transferId", transferId);

            DTORequest finRequest = new DTORequest("finalizarTransferenciaAudio", finPayload);
            DTOResponse finResponse = enviarPeticionAPeer(peer, finRequest);

            if (finResponse == null || !"success".equals(finResponse.getStatus())) {
                log.error("✗ Error al finalizar transferencia");
                return null;
            }

            // Extraer la ruta donde se guardó en el servidor destino
            @SuppressWarnings("unchecked")
            Map<String, Object> responseData = (Map<String, Object>) finResponse.getData();
            String rutaRemota = (String) responseData.get("filePath");
            log.info("✓ Transferencia completada exitosamente. Ruta remota: {}", rutaRemota);

            return rutaRemota;

        } catch (Exception e) {
            log.error("✗ Error en transferencia de audio: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Inicia la recepción de un archivo de audio desde otro peer.
     */
    public boolean iniciarRecepcionAudio(String transferId, String fileName,
                                         long fileSize, int totalChunks, String originalPath) {
        log.info("→ [AudioFileP2PService] Iniciando recepción de audio: {} ({} bytes, {} chunks)",
                 fileName, fileSize, totalChunks);

        AudioFileMetadata metadata = new AudioFileMetadata();
        metadata.transferId = transferId;
        metadata.fileName = fileName;
        metadata.fileSize = fileSize;
        metadata.totalChunks = totalChunks;
        metadata.originalPath = originalPath;

        metadataEnTransito.put(transferId, metadata);
        chunksEnTransito.put(transferId, new HashMap<>());

        return true;
    }

    /**
     * Recibe y almacena un chunk de audio.
     */
    public boolean recibirChunkAudio(String transferId, int chunkNumber, String chunkDataBase64) {
        try {
            if (!chunksEnTransito.containsKey(transferId)) {
                log.error("✗ TransferID no encontrado: {}", transferId);
                return false;
            }

            byte[] chunkData = Base64.getDecoder().decode(chunkDataBase64);
            chunksEnTransito.get(transferId).put(chunkNumber, chunkData);

            AudioFileMetadata metadata = metadataEnTransito.get(transferId);
            int recibidos = chunksEnTransito.get(transferId).size();

            if (recibidos % 10 == 0 || recibidos == metadata.totalChunks) {
                log.info("  → Recibidos {}/{} chunks", recibidos, metadata.totalChunks);
            }

            return true;

        } catch (Exception e) {
            log.error("✗ Error al recibir chunk: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Finaliza la recepción, ensambla el archivo y lo guarda.
     *
     * @return Ruta donde se guardó el archivo en este servidor
     */
    public String finalizarRecepcionAudio(String transferId) {
        try {
            AudioFileMetadata metadata = metadataEnTransito.get(transferId);
            Map<Integer, byte[]> chunks = chunksEnTransito.get(transferId);

            if (metadata == null || chunks == null) {
                log.error("✗ TransferID no encontrado: {}", transferId);
                return null;
            }

            if (chunks.size() != metadata.totalChunks) {
                log.error("✗ Chunks incompletos: {}/{}", chunks.size(), metadata.totalChunks);
                return null;
            }

            log.info("→ Ensamblando archivo de {} chunks...", metadata.totalChunks);

            // Ensamblar el archivo
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (int i = 0; i < metadata.totalChunks; i++) {
                byte[] chunk = chunks.get(i);
                if (chunk == null) {
                    log.error("✗ Chunk {} faltante", i);
                    return null;
                }
                baos.write(chunk);
            }

            byte[] archivoCompleto = baos.toByteArray();

            // Guardar el archivo usando el mismo nombre
            String rutaGuardada = fileStorageService.storeFile(
                archivoCompleto,
                metadata.fileName,
                "audio_files"
            );

            log.info("✓ Archivo ensamblado y guardado en: {}", rutaGuardada);

            // Limpiar memoria
            metadataEnTransito.remove(transferId);
            chunksEnTransito.remove(transferId);

            return rutaGuardada;

        } catch (Exception e) {
            log.error("✗ Error al finalizar recepción: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Envía una petición a un peer y espera respuesta.
     */
    private DTOResponse enviarPeticionAPeer(Peer peer, DTORequest request) throws Exception {
        try (Socket socket = new Socket(peer.getIp(), peer.getPuerto())) {
            socket.setSoTimeout(CONNECTION_TIMEOUT_MS);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String requestJson = gson.toJson(request);
            out.println(requestJson);

            String responseJson = in.readLine();
            if (responseJson == null) {
                throw new Exception("Sin respuesta del peer");
            }

            return gson.fromJson(responseJson, DTOResponse.class);

        } catch (Exception e) {
            log.error("✗ Error al comunicar con peer {}: {}", peer.getPeerId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Clase interna para almacenar metadata de transferencias.
     */
    private static class AudioFileMetadata {
        String transferId;
        String fileName;
        long fileSize;
        int totalChunks;
        String originalPath;
    }
}
