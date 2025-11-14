package com.arquitectura.logicaPeers.transfer;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.domain.Peer;
import com.arquitectura.persistence.repository.PeerRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class FileTransferServiceImpl implements FileTransferService {

    private final PeerRepository peerRepository;
    private final com.arquitectura.utils.p2p.PeerConnectionPool peerConnectionPool;

    @Autowired
    public FileTransferServiceImpl(PeerRepository peerRepository,
                                   com.arquitectura.utils.p2p.PeerConnectionPool peerConnectionPool) {
        this.peerRepository = peerRepository;
        this.peerConnectionPool = peerConnectionPool;
    }

    @Override
    public byte[] descargarArchivoDesdePeer(UUID peerDestinoId, String fileId) throws Exception {
        System.out.println("→ [FileTransferService] Descargando archivo " + fileId + " desde peer: " + peerDestinoId);

        // Obtener información del peer destino
        Peer peerDestino = peerRepository.findById(peerDestinoId)
                .orElseThrow(() -> new Exception("Peer destino no encontrado: " + peerDestinoId));

        if (!peerDestino.estaActivo()) {
            throw new Exception("El peer destino no está activo: " + peerDestinoId);
        }

        // Paso 1: Iniciar la descarga para obtener información del archivo
        System.out.println("→ [FileTransferService] Iniciando descarga con startFileDownload");
        DTORequest startDownloadRequest = new DTORequest(
            "startFileDownload",
            java.util.Map.of("fileId", fileId)
        );

        DTOResponse startResponse = peerConnectionPool.enviarPeticion(
            peerDestino.getIp(),
            peerDestino.getPuerto(),
            startDownloadRequest
        );

        if (startResponse == null || !"success".equals(startResponse.getStatus())) {
            throw new Exception("Error al iniciar descarga: " + (startResponse != null ? startResponse.getMessage() : "null"));
        }

        // Parsear la información de descarga
        com.google.gson.Gson gson = new com.google.gson.Gson();
        com.google.gson.JsonObject payload = gson.toJsonTree(startResponse.getData()).getAsJsonObject();
        String downloadId = payload.get("downloadId").getAsString();
        int totalChunks = payload.get("totalChunks").getAsInt();
        long fileSize = payload.get("fileSize").getAsLong();

        System.out.println("→ [FileTransferService] Archivo info: downloadId=" + downloadId + ", totalChunks=" + totalChunks + ", size=" + fileSize);

        // Paso 2: Descargar todos los chunks
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        for (int chunkNumber = 0; chunkNumber < totalChunks; chunkNumber++) {
            System.out.println("→ [FileTransferService] Solicitando chunk " + (chunkNumber + 1) + "/" + totalChunks);

            DTORequest chunkRequest = new DTORequest(
                "requestFileChunk",
                java.util.Map.of(
                    "downloadId", downloadId,
                    "chunkNumber", chunkNumber
                )
            );

            DTOResponse chunkResponse = peerConnectionPool.enviarPeticion(
                peerDestino.getIp(),
                peerDestino.getPuerto(),
                chunkRequest
            );

            if (chunkResponse == null || !"success".equals(chunkResponse.getStatus())) {
                throw new Exception("Error al descargar chunk " + chunkNumber + ": " + (chunkResponse != null ? chunkResponse.getMessage() : "null"));
            }

            // Extraer y decodificar el chunk
            com.google.gson.JsonObject chunkData = gson.toJsonTree(chunkResponse.getData()).getAsJsonObject();
            String chunkBase64 = chunkData.get("chunkDataBase64").getAsString();
            byte[] chunkBytes = java.util.Base64.getDecoder().decode(chunkBase64);

            // Escribir los bytes del chunk al stream
            baos.write(chunkBytes);
        }

        System.out.println("✓ [FileTransferService] Archivo descargado exitosamente (" + baos.size() + " bytes)");
        return baos.toByteArray();
    }
}

