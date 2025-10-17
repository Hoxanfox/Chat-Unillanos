package com.unillanos.server.service.impl;

import com.google.gson.Gson;
import com.unillanos.server.dto.*;
import com.unillanos.server.entity.ArchivoEntity;
import com.unillanos.server.gestor.archivos.DownloadSession;
import com.unillanos.server.gestor.archivos.GestorArchivos;
import com.unillanos.server.logs.LoggingService;
import com.unillanos.server.service.interfaces.IActionDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para transferencia de archivos por chunks.
 * Maneja las peticiones del cliente para subida y descarga de archivos grandes.
 * 
 * Acciones soportadas:
 * - startFileUpload: Inicia una sesión de subida
 * - uploadFileChunk: Recibe un chunk de archivo
 * - endFileUpload: Finaliza la subida y guarda el archivo
 * - startFileDownload: Inicia una sesión de descarga
 * - requestFileChunk: Solicita un chunk específico para descarga
 * - uploadFileForRegistration: Subida sin autenticación (para registro)
 * 
 * @author Chat-Unillanos Team
 * @version 1.0.0
 */
@Service
public class FileTransferService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileTransferService.class);
    
    private final GestorArchivos gestorArchivos;
    private final LoggingService loggingService;
    private final ConnectionManager connectionManager;
    private final Gson gson;
    
    // Almacena uploadId temporal para sesiones de registro (sin usuario autenticado)
    private final Map<String, String> registrationUploads = new HashMap<>();
    
    public FileTransferService(GestorArchivos gestorArchivos, 
                              LoggingService loggingService,
                              ConnectionManager connectionManager) {
        this.gestorArchivos = gestorArchivos;
        this.loggingService = loggingService;
        this.connectionManager = connectionManager;
        this.gson = new Gson();
        
        logger.info("FileTransferService inicializado correctamente");
    }
    
    /**
     * Inicia una nueva sesión de subida de archivo (requiere autenticación)
     */
    public DTOResponse startFileUpload(DTORequest request, ChannelHandlerContext ctx) {
        try {
            // Obtener usuario autenticado
            String usuarioId = connectionManager.getUserIdByContext(ctx);
            if (usuarioId == null) {
                logger.warn("Intento de subida sin autenticación desde: {}", ctx.channel().remoteAddress());
                return DTOResponse.error("startFileUpload", "Usuario no autenticado");
            }
            
            // Parsear payload
            Map<String, Object> payload = (Map<String, Object>) request.getPayload();
            String nombreArchivo = (String) payload.get("fileName");

            // Intentar múltiples nombres para el tipo MIME
            String tipoMime = (String) payload.get("mimeType");
            if (tipoMime == null) {
                tipoMime = (String) payload.get("fileMimeType");
            }
            if (tipoMime == null) {
                tipoMime = (String) payload.get("tipo_mime");
            }
            // Si aún es null, usar un valor por defecto basado en la extensión del archivo
            if (tipoMime == null || tipoMime.trim().isEmpty()) {
                tipoMime = detectarMimeType(nombreArchivo);
                logger.warn("Tipo MIME no proporcionado, detectado: {}", tipoMime);
            }

            int totalChunks = ((Number) payload.get("totalChunks")).intValue();
            
            // Validar datos
            if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
                return DTOResponse.error("startFileUpload", "Nombre de archivo requerido");
            }
            
            if (totalChunks <= 0) {
                return DTOResponse.error("startFileUpload", "Total de chunks inválido");
            }
            
            // Iniciar sesión de subida
            String uploadId = gestorArchivos.iniciarSubida(usuarioId, nombreArchivo, tipoMime, totalChunks);
            
            logger.info("Sesión de subida iniciada: {} - Usuario: {}, Archivo: {}, MIME: {}",
                       uploadId, usuarioId, nombreArchivo, tipoMime);

            // Respuesta
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("uploadId", uploadId);
            responseData.put("message", "Sesión de subida iniciada");
            
            return DTOResponse.success("startFileUpload", "Sesión iniciada exitosamente", responseData);
            
        } catch (Exception e) {
            logger.error("Error al iniciar subida de archivo", e);
            loggingService.logError("Error al iniciar subida", e, "FileTransferService.startFileUpload");
            return DTOResponse.error("startFileUpload", "Error al iniciar subida: " + e.getMessage());
        }
    }
    
    /**
     * Recibe un chunk de archivo durante la subida
     */
    public DTOResponse uploadFileChunk(DTORequest request, ChannelHandlerContext ctx) {
        try {
            // Parsear payload
            Map<String, Object> payload = (Map<String, Object>) request.getPayload();
            String uploadId = (String) payload.get("uploadId");
            int numeroChunk = ((Number) payload.get("chunkNumber")).intValue();

            // El cliente envía "chunkData_base64", intentar ambas versiones
            String chunkBase64 = (String) payload.get("chunkData_base64");
            if (chunkBase64 == null) {
                chunkBase64 = (String) payload.get("chunkData");
            }

            // Validar datos
            if (uploadId == null || uploadId.trim().isEmpty()) {
                return DTOResponse.error("uploadFileChunk", "Upload ID requerido");
            }
            
            if (chunkBase64 == null || chunkBase64.trim().isEmpty()) {
                return DTOResponse.error("uploadFileChunk", "Datos del chunk requeridos");
            }
            
            // Decodificar chunk
            byte[] chunkData = Base64.getDecoder().decode(chunkBase64);
            
            // Recibir chunk
            gestorArchivos.recibirChunk(uploadId, numeroChunk, chunkData);
            
            logger.debug("Chunk recibido: {} - Chunk #{} ({} bytes)", 
                        uploadId, numeroChunk, chunkData.length);
            
            // Respuesta
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("uploadId", uploadId);
            responseData.put("chunkNumber", numeroChunk);
            responseData.put("received", true);
            
            return DTOResponse.success("uploadFileChunk_" + uploadId + "_" + numeroChunk, 
                                      "Chunk recibido", responseData);
            
        } catch (IllegalArgumentException e) {
            logger.error("Error de validación al recibir chunk: {}", e.getMessage());
            return DTOResponse.error("uploadFileChunk", e.getMessage());
        } catch (Exception e) {
            logger.error("Error al recibir chunk de archivo", e);
            loggingService.logError("Error al recibir chunk", e, "FileTransferService.uploadFileChunk");
            return DTOResponse.error("uploadFileChunk", "Error al recibir chunk: " + e.getMessage());
        }
    }
    
    /**
     * Finaliza la subida de un archivo y lo guarda en el sistema
     */
    public DTOResponse endFileUpload(DTORequest request, ChannelHandlerContext ctx) {
        try {
            // Parsear payload
            Map<String, Object> payload = (Map<String, Object>) request.getPayload();
            String uploadId = (String) payload.get("uploadId");
            String hashEsperado = (String) payload.get("fileHash_sha256");

            // Si no viene como fileHash_sha256, intentar con fileHash
            if (hashEsperado == null) {
                hashEsperado = (String) payload.get("fileHash");
            }

            // Validar datos
            if (uploadId == null || uploadId.trim().isEmpty()) {
                return DTOResponse.error("endFileUpload", "Upload ID requerido");
            }
            
            // Finalizar subida y guardar archivo
            ArchivoEntity archivo = gestorArchivos.finalizarSubida(uploadId, hashEsperado);
            
            logger.info("Subida finalizada exitosamente: {} - Archivo ID: {}", 
                       uploadId, archivo.getId());
            
            // Respuesta con información del archivo
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("fileId", archivo.getId());
            responseData.put("fileName", archivo.getNombreOriginal());
            responseData.put("size", archivo.getTamanoBytes());
            responseData.put("mimeType", archivo.getTipoMime());
            responseData.put("hash", archivo.getHashSha256());
            
            return DTOResponse.success("endFileUpload", "Archivo subido exitosamente", responseData);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Error de validación al finalizar subida: {}", e.getMessage());
            return DTOResponse.error("endFileUpload", e.getMessage());
        } catch (Exception e) {
            logger.error("Error al finalizar subida de archivo", e);
            loggingService.logError("Error al finalizar subida", e, "FileTransferService.endFileUpload");
            return DTOResponse.error("endFileUpload", "Error al finalizar subida: " + e.getMessage());
        }
    }
    
    /**
     * Inicia una sesión de descarga de archivo
     */
    public DTOResponse startFileDownload(DTORequest request, ChannelHandlerContext ctx) {
        try {
            // Obtener usuario autenticado
            String usuarioId = connectionManager.getUserIdByContext(ctx);
            if (usuarioId == null) {
                return DTOResponse.error("startFileDownload", "Usuario no autenticado");
            }
            
            // Parsear payload
            Map<String, Object> payload = (Map<String, Object>) request.getPayload();
            String archivoId = (String) payload.get("fileId");
            
            // Validar datos
            if (archivoId == null || archivoId.trim().isEmpty()) {
                return DTOResponse.error("startFileDownload", "File ID requerido");
            }
            
            // Iniciar sesión de descarga
            DownloadSession session = gestorArchivos.iniciarDescarga(usuarioId, archivoId);
            
            logger.info("Sesión de descarga iniciada: {} - Usuario: {}, Archivo: {}", 
                       session.getDownloadId(), usuarioId, session.getNombreArchivo());
            
            // Respuesta con información de la descarga
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("downloadId", session.getDownloadId());
            responseData.put("fileName", session.getNombreArchivo());
            responseData.put("mimeType", session.getTipoMime());
            responseData.put("fileSize", session.getTamanoBytes());
            responseData.put("totalChunks", session.getTotalChunks());
            
            return DTOResponse.success("startFileDownload", "Sesión de descarga iniciada", responseData);
            
        } catch (IllegalArgumentException e) {
            logger.error("Archivo no encontrado: {}", e.getMessage());
            return DTOResponse.error("startFileDownload", e.getMessage());
        } catch (Exception e) {
            logger.error("Error al iniciar descarga de archivo", e);
            loggingService.logError("Error al iniciar descarga", e, "FileTransferService.startFileDownload");
            return DTOResponse.error("startFileDownload", "Error al iniciar descarga: " + e.getMessage());
        }
    }
    
    /**
     * Envía un chunk específico durante la descarga
     */
    public DTOResponse requestFileChunk(DTORequest request, ChannelHandlerContext ctx) {
        try {
            // Obtener usuario autenticado
            String usuarioId = connectionManager.getUserIdByContext(ctx);
            if (usuarioId == null) {
                return DTOResponse.error("requestFileChunk", "Usuario no autenticado");
            }
            
            // Parsear payload
            Map<String, Object> payload = (Map<String, Object>) request.getPayload();
            String downloadId = (String) payload.get("downloadId");
            int numeroChunk = ((Number) payload.get("chunkNumber")).intValue();
            
            // Validar datos
            if (downloadId == null || downloadId.trim().isEmpty()) {
                return DTOResponse.error("requestFileChunk", "Download ID requerido");
            }
            
            // Obtener chunk
            byte[] chunkData = gestorArchivos.obtenerChunk(downloadId, numeroChunk);
            String chunkBase64 = Base64.getEncoder().encodeToString(chunkData);
            
            logger.debug("Chunk enviado: {} - Chunk #{} ({} bytes)", 
                        downloadId, numeroChunk, chunkData.length);
            
            // Respuesta con el chunk
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("downloadId", downloadId);
            responseData.put("chunkNumber", numeroChunk);
            responseData.put("chunkData", chunkBase64);
            
            return DTOResponse.success("downloadFileChunk_" + downloadId + "_" + numeroChunk, 
                                      "Chunk enviado", responseData);
            
        } catch (IllegalArgumentException e) {
            logger.error("Error de validación al enviar chunk: {}", e.getMessage());
            return DTOResponse.error("requestFileChunk", e.getMessage());
        } catch (Exception e) {
            logger.error("Error al enviar chunk de archivo", e);
            loggingService.logError("Error al enviar chunk", e, "FileTransferService.requestFileChunk");
            return DTOResponse.error("requestFileChunk", "Error al enviar chunk: " + e.getMessage());
        }
    }
    
    /**
     * Inicia subida de archivo para REGISTRO (sin autenticación)
     * El uploadId se almacena temporalmente para asociarlo al usuario después del registro
     */
    public DTOResponse uploadFileForRegistration(DTORequest request, ChannelHandlerContext ctx) {
        try {
            // NO requiere autenticación - es para el proceso de registro
            
            // Parsear payload
            Map<String, Object> payload = (Map<String, Object>) request.getPayload();
            String nombreArchivo = (String) payload.get("fileName");
            String tipoMime = (String) payload.get("mimeType");
            int totalChunks = ((Number) payload.get("totalChunks")).intValue();
            
            // Validar datos
            if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
                return DTOResponse.error("uploadFileForRegistration", "Nombre de archivo requerido");
            }
            
            if (totalChunks <= 0) {
                return DTOResponse.error("uploadFileForRegistration", "Total de chunks inválido");
            }
            
            // Iniciar sesión de subida con usuario temporal
            String tempUserId = "REGISTRATION_" + ctx.channel().id().asShortText();
            String uploadId = gestorArchivos.iniciarSubida(tempUserId, nombreArchivo, tipoMime, totalChunks);
            
            // Almacenar para asociar después
            registrationUploads.put(uploadId, tempUserId);
            
            logger.info("Sesión de subida para registro iniciada: {} - Archivo: {}", 
                       uploadId, nombreArchivo);
            
            // Respuesta
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("uploadId", uploadId);
            responseData.put("message", "Sesión de subida para registro iniciada");
            
            return DTOResponse.success("uploadFileForRegistration", "Sesión iniciada exitosamente", responseData);
            
        } catch (Exception e) {
            logger.error("Error al iniciar subida para registro", e);
            loggingService.logError("Error al iniciar subida para registro", e, 
                                   "FileTransferService.uploadFileForRegistration");
            return DTOResponse.error("uploadFileForRegistration", "Error al iniciar subida: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene estadísticas del gestor de archivos
     */
    public DTOResponse getFileTransferStats(DTORequest request, ChannelHandlerContext ctx) {
        try {
            // Obtener usuario autenticado
            String usuarioId = connectionManager.getUserIdByContext(ctx);
            if (usuarioId == null) {
                return DTOResponse.error("getFileTransferStats", "Usuario no autenticado");
            }
            
            Map<String, Object> stats = gestorArchivos.obtenerEstadisticas();
            
            return DTOResponse.success("getFileTransferStats", "Estadísticas obtenidas", stats);
            
        } catch (Exception e) {
            logger.error("Error al obtener estadísticas", e);
            return DTOResponse.error("getFileTransferStats", "Error al obtener estadísticas");
        }
    }

    /**
     * Detecta el tipo MIME basado en la extensión del archivo
     */
    private String detectarMimeType(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
            return "application/octet-stream";
        }

        String extension = "";
        int lastDot = nombreArchivo.lastIndexOf('.');
        if (lastDot > 0 && lastDot < nombreArchivo.length() - 1) {
            extension = nombreArchivo.substring(lastDot + 1).toLowerCase();
        }

        // Mapeo de extensiones comunes a tipos MIME
        return switch (extension) {
            // Imágenes
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";

            // Documentos
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt" -> "text/plain";

            // Audio
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            case "m4a" -> "audio/mp4";

            // Video
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mkv" -> "video/x-matroska";
            case "webm" -> "video/webm";

            // Archivos comprimidos
            case "zip" -> "application/zip";
            case "rar" -> "application/x-rar-compressed";
            case "7z" -> "application/x-7z-compressed";

            // Por defecto
            default -> "application/octet-stream";
        };
    }
}
