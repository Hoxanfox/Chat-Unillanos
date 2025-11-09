package com.arquitectura.controlador.controllers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.archivos.DTODownloadInfo;
import com.arquitectura.DTO.archivos.DTOEndUpload;
import com.arquitectura.DTO.archivos.DTOStartUpload;
import com.arquitectura.DTO.archivos.DTOUploadChunk;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.fachada.IChatFachada;
import com.arquitectura.utils.chunkManager.FileUploadResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Controlador para operaciones relacionadas con archivos:
 * - Subida de archivos por chunks
 * - Descarga de archivos por chunks
 */
@Component
public class FileController extends BaseController {
    
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
        "startfileupload",
        "uploadfileforregistration",
        "uploadfilechunk",
        "endfileupload",
        "startfiledownload",
        "requestfilechunk",
        "descargararchivo"
    );
    
    @Autowired
    public FileController(IChatFachada chatFachada, Gson gson) {
        super(chatFachada, gson);
    }
    
    @Override
    public boolean handleAction(String action, DTORequest request, IClientHandler handler) {
        if (!SUPPORTED_ACTIONS.contains(action.toLowerCase())) {
            return false;
        }
        
        switch (action.toLowerCase()) {
            case "startfileupload":
            case "uploadfileforregistration":
                handleStartUpload(request, handler, action);
                break;
            case "uploadfilechunk":
                handleUploadChunk(request, handler);
                break;
            case "endfileupload":
                handleEndUpload(request, handler, action);
                break;
            case "startfiledownload":
                handleStartDownload(request, handler, action);
                break;
            case "requestfilechunk":
                handleRequestChunk(request, handler);
                break;
            case "descargararchivo":
                handleDownloadFile(request, handler);
                break;
            default:
                return false;
        }
        
        return true;
    }
    
    @Override
    public Set<String> getSupportedActions() {
        return SUPPORTED_ACTIONS;
    }
    
    private void handleStartUpload(DTORequest request, IClientHandler handler, String action) {
        try {
            DTOStartUpload payload = gson.fromJson(gson.toJsonTree(request.getPayload()), DTOStartUpload.class);
            String uploadId = chatFachada.archivos().startUpload(payload);
            sendJsonResponse(handler, action, true, "Upload iniciado", Map.of("uploadId", uploadId));
        } catch (Exception e) {
            System.err.println("Error al iniciar upload: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, action, false, "Error interno del servidor al iniciar upload", null);
        }
    }
    
    private void handleUploadChunk(DTORequest request, IClientHandler handler) {
        try {
            DTOUploadChunk payload = gson.fromJson(gson.toJsonTree(request.getPayload()), DTOUploadChunk.class);
            chatFachada.archivos().processChunk(payload);

            // Respuesta PUSH (ack dinámico)
            String ackAction = "uploadFileChunk_" + payload.getUploadId() + "_" + payload.getChunkNumber();
            sendJsonResponse(handler, ackAction, true, "Chunk " + payload.getChunkNumber() + " recibido", null);
        } catch (Exception e) {
            System.err.println("Error al procesar chunk: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "uploadFileChunk", false, "Error interno del servidor al procesar chunk", null);
        }
    }
    
    private void handleEndUpload(DTORequest request, IClientHandler handler, String action) {
        try {
            DTOEndUpload payload = gson.fromJson(gson.toJsonTree(request.getPayload()), DTOEndUpload.class);

            String subDirectory;
            UUID autorId;

            if (handler.isAuthenticated()) {
                autorId = handler.getAuthenticatedUser().getUserId();
                subDirectory = "audio_files";
            } else {
                autorId = UUID.fromString("00000000-0000-0000-0000-000000000000");
                subDirectory = "user_photos";
            }

            FileUploadResponse responseDataLocal = chatFachada.archivos().endUpload(payload, autorId, subDirectory);
            sendJsonResponse(handler, action, true, "Archivo subido", responseDataLocal);
        } catch (Exception e) {
            System.err.println("Error al finalizar upload: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, action, false, "Error interno del servidor al finalizar upload", null);
        }
    }
    
    private void handleStartDownload(DTORequest request, IClientHandler handler, String action) {
        try {
            JsonObject payloadJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String fileId = payloadJson.get("fileId").getAsString();

            DTODownloadInfo info = chatFachada.archivos().startDownload(fileId);
            sendJsonResponse(handler, action, true, "Descarga iniciada", info);
        } catch (Exception e) {
            System.err.println("Error al iniciar descarga: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, action, false, "Error interno del servidor al iniciar descarga", null);
        }
    }
    
    private void handleRequestChunk(DTORequest request, IClientHandler handler) {
        try {
            JsonObject payloadJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String downloadId = payloadJson.get("downloadId").getAsString();
            int chunkNumber = payloadJson.get("chunkNumber").getAsInt();

            byte[] chunkBytes = chatFachada.archivos().getChunk(downloadId, chunkNumber);
            String chunkBase64 = Base64.getEncoder().encodeToString(chunkBytes);

            Map<String, Object> chunkData = new HashMap<>();
            chunkData.put("chunkNumber", chunkNumber);
            chunkData.put("chunkDataBase64", chunkBase64);

            // Respuesta PUSH (chunk dinámico)
            String pushAction = "downloadFileChunk_" + downloadId + "_" + chunkNumber;
            sendJsonResponse(handler, pushAction, true, "Enviando chunk", chunkData);
        } catch (Exception e) {
            System.err.println("Error al obtener chunk: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "requestFileChunk", false, "Error interno del servidor al obtener chunk", null);
        }
    }
    
    private void handleDownloadFile(DTORequest request, IClientHandler handler) {
        try {
            JsonObject payloadJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            UUID peerDestinoId = UUID.fromString(payloadJson.get("peerDestinoId").getAsString());
            String fileId = payloadJson.get("fileId").getAsString();

            System.out.println("→ [FileController] Iniciando descarga de archivo desde peer: " + peerDestinoId);

            // Descargar el archivo completo desde el peer usando la fachada
            byte[] archivoCompleto = chatFachada.p2p().descargarArchivoDesdePeer(peerDestinoId, fileId);

            // Convertir a Base64 para enviar al cliente
            String archivoBase64 = Base64.getEncoder().encodeToString(archivoCompleto);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("fileId", fileId);
            responseData.put("fileDataBase64", archivoBase64);
            responseData.put("size", archivoCompleto.length);

            sendJsonResponse(handler, "descargararchivo", true, "Archivo descargado exitosamente desde peer", responseData);

        } catch (Exception e) {
            System.err.println("Error al descargar archivo desde peer: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "descargararchivo", false, "Error al descargar archivo: " + e.getMessage(), null);
        }
    }
}
