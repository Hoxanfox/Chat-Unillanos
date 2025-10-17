package com.unillanos.server.gestor.archivos;

import com.unillanos.server.entity.ArchivoEntity;
import com.unillanos.server.entity.TipoArchivo;
import com.unillanos.server.logs.LoggingService;
import com.unillanos.server.repository.interfaces.IArchivoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestor principal de archivos multimedia del servidor.
 * Maneja la subida y descarga de archivos por chunks, almacenamiento físico
 * y persistencia en base de datos.
 * 
 * @author Chat-Unillanos Team
 * @version 1.0.0
 */
@Component
public class GestorArchivos {
    
    private static final Logger logger = LoggerFactory.getLogger(GestorArchivos.class);
    private static final int CHUNK_SIZE = 256; // 1.5 MB
    
    private final IArchivoRepository archivoRepository;
    private final LoggingService loggingService;
    
    @Value("${server.uploads.basePath:uploads}")
    private String uploadsBasePath;
    
    // Sesiones activas de subida y descarga
    private final Map<String, UploadSession> uploadSessions = new ConcurrentHashMap<>();
    private final Map<String, DownloadSession> downloadSessions = new ConcurrentHashMap<>();
    
    // Scheduler para limpiar sesiones expiradas
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
        Thread.ofVirtual().factory()
    );
    
    public GestorArchivos(IArchivoRepository archivoRepository, LoggingService loggingService) {
        this.archivoRepository = archivoRepository;
        this.loggingService = loggingService;
        
        // Limpiar sesiones expiradas cada 10 minutos
        scheduler.scheduleAtFixedRate(this::limpiarSesionesExpiradas, 10, 10, TimeUnit.MINUTES);
        
        logger.info("GestorArchivos inicializado. CHUNK_SIZE: {} bytes", CHUNK_SIZE);
    }
    
    /**
     * Inicia una nueva sesión de subida de archivo
     */
    public String iniciarSubida(String usuarioId, String nombreArchivo, String tipoMime, int totalChunks) {
        String uploadId = UUID.randomUUID().toString();
        
        UploadSession session = new UploadSession(uploadId, usuarioId, nombreArchivo, tipoMime, totalChunks);
        uploadSessions.put(uploadId, session);
        
        logger.info("Sesión de subida iniciada: {} - Usuario: {}, Archivo: {}, Chunks: {}", 
                   uploadId, usuarioId, nombreArchivo, totalChunks);
        
        loggingService.logFileEvent("upload_started", usuarioId, uploadId, nombreArchivo);
        
        return uploadId;
    }
    
    /**
     * Recibe un chunk de archivo durante la subida
     */
    public void recibirChunk(String uploadId, int numeroChunk, byte[] chunkData) {
        UploadSession session = uploadSessions.get(uploadId);
        
        if (session == null) {
            throw new IllegalArgumentException("Sesión de subida no encontrada: " + uploadId);
        }
        
        session.agregarChunk(numeroChunk, chunkData);
        
        logger.debug("Chunk recibido: {} - Chunk {}/{} ({} bytes) - Progreso: {}%", 
                    uploadId, numeroChunk, session.getTotalChunks(), 
                    chunkData.length, session.calcularProgreso());
    }
    
    /**
     * Finaliza la subida de un archivo, ensambla los chunks, calcula hash y almacena
     */
    public ArchivoEntity finalizarSubida(String uploadId, String hashEsperado) throws IOException {
        UploadSession session = uploadSessions.get(uploadId);
        
        if (session == null) {
            throw new IllegalArgumentException("Sesión de subida no encontrada: " + uploadId);
        }
        
        if (!session.estaCompleta()) {
            throw new IllegalStateException("Subida incompleta. Recibidos: " + 
                                          session.getChunksRecibidos() + "/" + session.getTotalChunks());
        }
        
        try {
            // Ensamblar el archivo completo
            byte[] archivoCompleto = session.ensamblarArchivo();
            
            // Calcular hash SHA-256
            String hashCalculado = calcularHashSHA256(archivoCompleto);
            
            // Verificar hash si se proporcionó
            if (hashEsperado != null && !hashCalculado.equals(hashEsperado)) {
                throw new IOException("Hash no coincide. Esperado: " + hashEsperado + 
                                     ", Calculado: " + hashCalculado);
            }
            
            logger.info("Archivo ensamblado correctamente: {} - Tamaño: {} bytes, Hash: {}", 
                       uploadId, archivoCompleto.length, hashCalculado);
            
            // Verificar si ya existe un archivo con el mismo hash (deduplicación)
            Optional<ArchivoEntity> archivoExistente = archivoRepository.findByHash(hashCalculado);
            if (archivoExistente.isPresent()) {
                logger.info("Archivo duplicado detectado. Reutilizando archivo existente: {}", 
                           archivoExistente.get().getId());
                
                // Registrar log de deduplicación
                loggingService.logFileEvent("upload_deduplicated", session.getUsuarioId(), 
                                          archivoExistente.get().getId(), session.getNombreArchivo());
                
                // Limpiar sesión
                uploadSessions.remove(uploadId);
                
                return archivoExistente.get();
            }
            
            // Determinar tipo de archivo
            TipoArchivo tipoArchivo = determinarTipoArchivo(session.getTipoMime());
            
            // Generar nombre único para almacenamiento
            String nombreAlmacenado = UUID.randomUUID().toString() + 
                                     obtenerExtension(session.getNombreArchivo());
            
            // Determinar ruta de almacenamiento según tipo
            String rutaRelativa = obtenerRutaSegunTipo(tipoArchivo, nombreAlmacenado);
            Path rutaCompleta = Paths.get(uploadsBasePath, rutaRelativa);
            
            // Crear directorios si no existen
            Files.createDirectories(rutaCompleta.getParent());
            
            // Guardar archivo físicamente
            Files.write(rutaCompleta, archivoCompleto);
            
            logger.info("Archivo guardado físicamente: {}", rutaCompleta);
            
            // Detectar tipo MIME si es null
            String tipoMime = session.getTipoMime();
            if (tipoMime == null || tipoMime.trim().isEmpty()) {
                tipoMime = detectarTipoMime(session.getNombreArchivo());
                logger.warn("Tipo MIME no disponible en sesión, detectado: {}", tipoMime);
            }

            // Validar que el usuario_id no sea nulo (permitir usuario temporal para registro)
            String usuarioId = session.getUsuarioId();
            if (usuarioId == null || usuarioId.trim().isEmpty()) {
                logger.error("Error: usuario_id es nulo o vacío en la sesión: {}", uploadId);
                throw new IllegalStateException("El usuario_id no puede ser nulo o vacío");
            }

            // Si es un usuario temporal de registro (REGISTRATION_*), guardar como NULL en BD
            boolean esRegistro = usuarioId.startsWith("REGISTRATION_");
            String usuarioIdParaGuardar = esRegistro ? null : usuarioId;

            logger.info("Intentando guardar archivo en BD - Usuario ID: {}, Archivo: {}, Hash: {}, Es Registro: {}",
                       usuarioId, session.getNombreArchivo(), hashCalculado, esRegistro);

            // Crear entidad y guardar en BD
            ArchivoEntity archivo = new ArchivoEntity();
            archivo.setId(UUID.randomUUID().toString());
            archivo.setNombreOriginal(session.getNombreArchivo());
            archivo.setNombreAlmacenado(nombreAlmacenado);
            archivo.setTipoMime(tipoMime);
            archivo.setTipoArchivo(tipoArchivo);
            archivo.setHashSha256(hashCalculado);
            archivo.setTamanoBytes(archivoCompleto.length);
            archivo.setRutaAlmacenamiento(rutaRelativa);
            archivo.setUsuarioId(usuarioIdParaGuardar);  // NULL si es registro
            archivo.setFechaSubida(LocalDateTime.now());
            
            ArchivoEntity archivoGuardado = archivoRepository.save(archivo);
            
            logger.info("Archivo guardado en BD: {} - ID: {}", session.getNombreArchivo(), archivoGuardado.getId());
            
            loggingService.logFileEvent("upload_completed", session.getUsuarioId(), 
                                       archivoGuardado.getId(), session.getNombreArchivo());
            
            // Limpiar sesión
            uploadSessions.remove(uploadId);
            
            return archivoGuardado;
            
        } catch (Exception e) {
            logger.error("Error al finalizar subida: {}", uploadId, e);
            loggingService.logError("Error al finalizar subida de archivo", e, "GestorArchivos.finalizarSubida");
            uploadSessions.remove(uploadId);
            throw e;
        }
    }
    
    /**
     * Inicia una nueva sesión de descarga de archivo
     */
    public DownloadSession iniciarDescarga(String usuarioId, String archivoId) throws IOException {
        // Buscar archivo en BD
        Optional<ArchivoEntity> archivoOpt = archivoRepository.findById(archivoId);
        
        if (archivoOpt.isEmpty()) {
            throw new IllegalArgumentException("Archivo no encontrado: " + archivoId);
        }
        
        ArchivoEntity archivo = archivoOpt.get();
        
        // Leer archivo físico
        Path rutaCompleta = Paths.get(uploadsBasePath, archivo.getRutaAlmacenamiento());
        
        if (!Files.exists(rutaCompleta)) {
            throw new IOException("Archivo físico no encontrado: " + rutaCompleta);
        }
        
        byte[] contenidoArchivo = Files.readAllBytes(rutaCompleta);
        
        // Calcular total de chunks
        int totalChunks = (int) Math.ceil((double) contenidoArchivo.length / CHUNK_SIZE);
        
        // Crear sesión de descarga
        String downloadId = UUID.randomUUID().toString();
        DownloadSession session = new DownloadSession(
            downloadId, usuarioId, archivoId,
            archivo.getNombreOriginal(), archivo.getTipoMime(),
            archivo.getTamanoBytes(), totalChunks, contenidoArchivo
        );
        
        downloadSessions.put(downloadId, session);
        
        logger.info("Sesión de descarga iniciada: {} - Usuario: {}, Archivo: {}, Chunks: {}", 
                   downloadId, usuarioId, archivo.getNombreOriginal(), totalChunks);
        
        loggingService.logFileEvent("download_started", usuarioId, archivoId, archivo.getNombreOriginal());
        
        return session;
    }
    
    /**
     * Obtiene un chunk específico durante la descarga
     */
    public byte[] obtenerChunk(String downloadId, int numeroChunk) {
        DownloadSession session = downloadSessions.get(downloadId);
        
        if (session == null) {
            throw new IllegalArgumentException("Sesión de descarga no encontrada: " + downloadId);
        }
        
        byte[] chunk = session.obtenerChunk(numeroChunk, CHUNK_SIZE);
        
        logger.debug("Chunk enviado: {} - Chunk {}/{} ({} bytes)", 
                    downloadId, numeroChunk, session.getTotalChunks(), chunk.length);
        
        // Si es el último chunk, registrar finalización y limpiar sesión
        if (session.estaCompleta()) {
            logger.info("Descarga completada: {} - {}", downloadId, session.getNombreArchivo());
            loggingService.logFileEvent("download_completed", session.getUsuarioId(), 
                                       session.getArchivoId(), session.getNombreArchivo());
            downloadSessions.remove(downloadId);
        }
        
        return chunk;
    }
    
    /**
     * Obtiene una sesión de descarga activa
     */
    public DownloadSession obtenerSesionDescarga(String downloadId) {
        return downloadSessions.get(downloadId);
    }
    
    /**
     * Determina el tipo de archivo según el MIME type
     */
    private TipoArchivo determinarTipoArchivo(String tipoMime) {
        if (tipoMime == null) {
            return TipoArchivo.DOCUMENT;
        }
        
        String mime = tipoMime.toLowerCase();
        
        if (mime.startsWith("image/")) {
            return TipoArchivo.IMAGE;
        } else if (mime.startsWith("audio/")) {
            return TipoArchivo.AUDIO;
        } else {
            return TipoArchivo.DOCUMENT;
        }
    }
    
    /**
     * Obtiene la ruta de almacenamiento según el tipo de archivo
     */
    private String obtenerRutaSegunTipo(TipoArchivo tipo, String nombreArchivo) {
        return switch (tipo) {
            case IMAGE -> "imagenes/" + nombreArchivo;
            case AUDIO -> "audios/" + nombreArchivo;
            case DOCUMENT -> "documentos/" + nombreArchivo;
        };
    }
    
    /**
     * Extrae la extensión del nombre de archivo
     */
    private String obtenerExtension(String nombreArchivo) {
        int lastDot = nombreArchivo.lastIndexOf('.');
        if (lastDot > 0 && lastDot < nombreArchivo.length() - 1) {
            return nombreArchivo.substring(lastDot);
        }
        return "";
    }
    
    /**
     * Calcula el hash SHA-256 de un array de bytes
     */
    private String calcularHashSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular hash SHA-256", e);
        }
    }
    
    /**
     * Limpia las sesiones expiradas (sin actividad por más de 30 minutos)
     */
    private void limpiarSesionesExpiradas() {
        try {
            int uploadExpiradas = 0;
            int downloadExpiradas = 0;
            
            // Limpiar sesiones de subida expiradas
            for (Map.Entry<String, UploadSession> entry : uploadSessions.entrySet()) {
                if (entry.getValue().haExpirado()) {
                    uploadSessions.remove(entry.getKey());
                    uploadExpiradas++;
                    logger.info("Sesión de subida expirada eliminada: {}", entry.getKey());
                }
            }
            
            // Limpiar sesiones de descarga expiradas
            for (Map.Entry<String, DownloadSession> entry : downloadSessions.entrySet()) {
                if (entry.getValue().haExpirado()) {
                    downloadSessions.remove(entry.getKey());
                    downloadExpiradas++;
                    logger.info("Sesión de descarga expirada eliminada: {}", entry.getKey());
                }
            }
            
            if (uploadExpiradas > 0 || downloadExpiradas > 0) {
                logger.info("Limpieza de sesiones completada. Uploads: {}, Downloads: {}", 
                           uploadExpiradas, downloadExpiradas);
            }
            
        } catch (Exception e) {
            logger.error("Error durante limpieza de sesiones: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene estadísticas de las sesiones activas
     */
    public Map<String, Object> obtenerEstadisticas() {
        return Map.of(
            "uploadSessionsActivas", uploadSessions.size(),
            "downloadSessionsActivas", downloadSessions.size(),
            "chunkSize", CHUNK_SIZE
        );
    }
    
    /**
     * Cierra el gestor y libera recursos
     */
    public void shutdown() {
        scheduler.shutdown();
        uploadSessions.clear();
        downloadSessions.clear();
        logger.info("GestorArchivos cerrado correctamente");
    }

    /**
     * Detecta el tipo MIME basado en la extensión del archivo
     */
    private String detectarTipoMime(String nombreArchivo) {
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
