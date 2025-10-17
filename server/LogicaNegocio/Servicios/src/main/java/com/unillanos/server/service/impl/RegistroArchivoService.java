package com.unillanos.server.service.impl;

import com.unillanos.server.config.ServerConfigProperties;
import com.unillanos.server.dto.*;
import com.unillanos.server.exception.NotFoundException;
import com.unillanos.server.exception.ValidationException;
import com.unillanos.server.repository.interfaces.IArchivoRepository;
import com.unillanos.server.entity.ArchivoEntity;

import com.unillanos.server.validation.TipoArchivoValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio especializado para subida de archivos durante el REGISTRO.
 * NO requiere autenticación - los archivos se guardan con usuario_id = NULL.
 */
@Service
public class RegistroArchivoService {

    private static final Logger logger = LoggerFactory.getLogger(RegistroArchivoService.class);

    private final IArchivoRepository archivoRepository;
    private final LoggerService loggerService;
    private final ServerConfigProperties config;
    
    // Sesiones de registro en memoria
    private final Map<String, SessionRegistroInfo> sessionesRegistro = new ConcurrentHashMap<>();

    public RegistroArchivoService(IArchivoRepository archivoRepository,
                                  LoggerService loggerService,
                                  ServerConfigProperties config) {
        this.archivoRepository = archivoRepository;
        this.loggerService = loggerService;
        this.config = config;
    }

    /**
     * Clase interna para sesiones de registro
     */
    private static class SessionRegistroInfo {
        String sessionId;
        String nombreArchivo;
        String tipoMime;
        long tamanoTotal;  // Cambiar de int a long
        int totalChunks;
        Set<Integer> chunksRecibidos = new HashSet<>();
        LocalDateTime fechaInicio;
        
        SessionRegistroInfo(String sessionId, String nombreArchivo, 
                           String tipoMime, long tamanoTotal, int totalChunks) {  // Cambiar de int a long
            this.sessionId = sessionId;
            this.nombreArchivo = nombreArchivo;
            this.tipoMime = tipoMime;
            this.tamanoTotal = tamanoTotal;
            this.totalChunks = totalChunks;
            this.fechaInicio = LocalDateTime.now();
        }
    }

    /**
     * Inicia sesión de subida para REGISTRO (sin autenticación).
     */
    public DTOResponse iniciarSubidaParaRegistro(DTOIniciarSubida dto) {
        logger.info("Iniciando subida para REGISTRO - Archivo: {}", dto.getNombreArchivo());
        try {
            // 1. Validar campos básicos
            if (dto.getNombreArchivo() == null || dto.getNombreArchivo().trim().isEmpty()) {
                throw new ValidationException("El nombre del archivo es requerido", "nombreArchivo");
            }
            if (dto.getTipoMime() == null || dto.getTipoMime().trim().isEmpty()) {
                throw new ValidationException("El tipo MIME es requerido", "tipoMime");
            }
            if (dto.getTamanoTotal() <= 0) {
                throw new ValidationException("El tamaño del archivo debe ser mayor a 0", "tamanoTotal");
            }
            if (dto.getTotalChunks() <= 0) {
                throw new ValidationException("El total de chunks debe ser mayor a 0", "totalChunks");
            }

            // 2. Crear sesión EN MEMORIA
            String sessionId = UUID.randomUUID().toString();
            SessionRegistroInfo session = new SessionRegistroInfo(
                sessionId,
                dto.getNombreArchivo(),
                dto.getTipoMime(),
                dto.getTamanoTotal(),
                dto.getTotalChunks()
            );
            
            sessionesRegistro.put(sessionId, session);
            logger.info("Sesión de registro creada: {}", sessionId);

            // 3. Crear directorio temporal
            crearDirectorioSesion(sessionId);

            // 4. Responder
            DTOIniciarSubidaResponse response = new DTOIniciarSubidaResponse(
                sessionId,
                config.getArchivos() != null ? config.getArchivos().getChunkSize() : 512 * 1024,
                new ArrayList<>()
            );

            loggerService.logInfo("iniciarSubidaParaRegistro",
                String.format("Sesión de registro iniciada: %s", sessionId));

            return DTOResponse.success("uploadFileForRegistration", "Sesión iniciada", response);

        } catch (ValidationException e) {
            logger.warn("Error al iniciar subida para registro: {}", e.getMessage());
            return DTOResponse.error("uploadFileForRegistration", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al iniciar subida para registro", e);
            return DTOResponse.error("uploadFileForRegistration", "Error interno");
        }
    }

    /**
     * Sube un chunk para REGISTRO (sin autenticación).
     */
    public DTOResponse subirChunkParaRegistro(DTOSubirArchivoChunk dto) {
        logger.debug("Recibiendo chunk de registro {}/{} para sesión {}", 
            dto.getNumeroChunk(), dto.getTotalChunks(), dto.getSessionId());
        try {
            // 1. Validar campos básicos
            if (dto.getSessionId() == null || dto.getSessionId().trim().isEmpty()) {
                throw new ValidationException("El sessionId es requerido", "sessionId");
            }
            if (dto.getNumeroChunk() <= 0) {
                throw new ValidationException("El número de chunk debe ser mayor a 0", "numeroChunk");
            }
            if (dto.getTotalChunks() <= 0) {
                throw new ValidationException("El total de chunks debe ser mayor a 0", "totalChunks");
            }
            if (dto.getBase64ChunkData() == null || dto.getBase64ChunkData().trim().isEmpty()) {
                throw new ValidationException("Los datos del chunk son requeridos", "base64ChunkData");
            }

            // 2. Verificar sesión
            SessionRegistroInfo session = sessionesRegistro.get(dto.getSessionId());
            if (session == null) {
                logger.warn("Sesión de registro no encontrada: {}", dto.getSessionId());
                throw new NotFoundException("Sesión no encontrada", "SESSION_NOT_FOUND");
            }

            // 3. Verificar si ya recibido
            if (session.chunksRecibidos.contains(dto.getNumeroChunk())) {
                logger.info("Chunk {} ya recibido", dto.getNumeroChunk());
                return DTOResponse.success("uploadFileChunkForRegistration", "Chunk ya recibido", null);
            }

            // 4. Decodificar y guardar (SIN verificar hash para simplificar)
            byte[] chunkData = Base64.getDecoder().decode(dto.getBase64ChunkData());

            // 5. Guardar chunk en disco
            int chunkIndex = dto.getNumeroChunk() - 1;
            Path chunkPath = Paths.get(config.getArchivos().getDirectorioTemp(), 
                dto.getSessionId(), "chunk_" + chunkIndex);
            Files.write(chunkPath, chunkData);

            // 6. Registrar en sesión
            session.chunksRecibidos.add(dto.getNumeroChunk());

            logger.debug("Chunk de registro {} guardado", dto.getNumeroChunk());
            return DTOResponse.success("uploadFileChunkForRegistration", "Chunk recibido", null);

        } catch (ValidationException | NotFoundException e) {
            logger.warn("Error al subir chunk de registro: {}", e.getMessage());
            return DTOResponse.error("uploadFileChunkForRegistration", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al subir chunk de registro", e);
            return DTOResponse.error("uploadFileChunkForRegistration", "Error al procesar chunk");
        }
    }

    /**
     * Finaliza subida para REGISTRO - guarda archivo con usuario_id = NULL.
     */
    public DTOResponse finalizarSubidaParaRegistro(String sessionId) {
        logger.info("Finalizando subida de registro para sesión: {}", sessionId);
        try {
            // 1. Validar sessionId
            if (sessionId == null || sessionId.trim().isEmpty()) {
                throw new ValidationException("El sessionId es requerido", "sessionId");
            }

            // 2. Obtener sesión
            SessionRegistroInfo session = sessionesRegistro.get(sessionId);
            if (session == null) {
                logger.warn("Sesión de registro no encontrada: {}", sessionId);
                throw new NotFoundException("Sesión no encontrada", "SESSION_NOT_FOUND");
            }

            // 3. Verificar chunks
            if (session.chunksRecibidos.size() != session.totalChunks) {
                logger.warn("Faltan chunks de registro. Recibidos: {}, esperados: {}", 
                    session.chunksRecibidos.size(), session.totalChunks);
                throw new ValidationException(
                    String.format("Faltan chunks. Recibidos: %d, esperados: %d",
                        session.chunksRecibidos.size(), session.totalChunks),
                    "sessionId"
                );
            }

            // 4. Ensamblar archivo
            Path archivoCompleto = ensamblarArchivo(session);

            // 5. Calcular hash
            String hashArchivo = calculateSha256(Files.readAllBytes(archivoCompleto));
            logger.debug("Hash de archivo de registro: {}", hashArchivo);

            // 6. Verificar duplicados
            Optional<ArchivoEntity> archivoExistente = archivoRepository.findByHash(hashArchivo);
            if (archivoExistente.isPresent()) {
                Files.deleteIfExists(archivoCompleto);
                eliminarDirectorioSesion(sessionId);
                sessionesRegistro.remove(sessionId);

                logger.info("Archivo de registro duplicado detectado: {}", archivoExistente.get().getId());
                return DTOResponse.success("endFileUploadForRegistration", "Archivo subido (duplicado)",
                    archivoExistente.get().toDTO(true));
            }

            // 7. Mover a ubicación final
            String tipoLogico = TipoArchivoValidator.detectarTipo(session.tipoMime);
            String archivoId = UUID.randomUUID().toString();
            String extension = obtenerExtension(session.nombreArchivo);
            String nombreAlmacenado = archivoId + (extension != null ? ("." + extension) : "");
            
            Path directorioDestino = Paths.get(config.getArchivos().getDirectorioBase(), 
                tipoLogico.toLowerCase());
            Path rutaFinal = directorioDestino.resolve(nombreAlmacenado);

            Files.createDirectories(directorioDestino);
            Files.move(archivoCompleto, rutaFinal);

            // 8. Guardar en BD con usuario_id = NULL
            ArchivoEntity archivo = new ArchivoEntity();
            archivo.setId(archivoId);
            archivo.setNombreOriginal(session.nombreArchivo);
            archivo.setNombreAlmacenado(nombreAlmacenado);
            archivo.setTipoMime(session.tipoMime);
            archivo.setTamanoBytes(session.tamanoTotal);
            archivo.setHashSha256(hashArchivo);
            archivo.setRutaAlmacenamiento(Paths.get(tipoLogico.toLowerCase(), nombreAlmacenado).toString());
            archivo.setFechaSubida(LocalDateTime.now());
            archivo.setUsuarioId(null); // *** PUNTO CLAVE: NULL para registro ***

            ArchivoEntity archivoGuardado = archivoRepository.save(archivo);

            // 9. Limpiar
            eliminarDirectorioSesion(sessionId);
            sessionesRegistro.remove(sessionId);

            loggerService.logInfo("finalizarSubidaParaRegistro",
                String.format("Archivo de registro ensamblado: %s (ID: %s)", 
                    session.nombreArchivo, archivoId));

            return DTOResponse.success("endFileUploadForRegistration", 
                "Archivo de registro subido exitosamente",
                archivoGuardado.toDTO(false));

        } catch (ValidationException | NotFoundException e) {
            logger.warn("Error al finalizar subida de registro: {}", e.getMessage());
            return DTOResponse.error("endFileUploadForRegistration", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al finalizar subida de registro", e);
            return DTOResponse.error("endFileUploadForRegistration", "Error al procesar archivo");
        }
    }

    /**
     * Actualiza el usuario_id de un archivo después del registro exitoso.
     */
    public void vincularArchivoConUsuario(String archivoId, String usuarioId) {
        logger.info("Vinculando archivo {} con usuario {}", archivoId, usuarioId);
        try {
            Optional<ArchivoEntity> archivoOpt = archivoRepository.findById(archivoId);
            if (archivoOpt.isPresent()) {
                ArchivoEntity archivo = archivoOpt.get();
                archivo.setUsuarioId(usuarioId);
                archivoRepository.save(archivo);
                logger.info("Archivo {} vinculado exitosamente", archivoId);
            } else {
                logger.warn("Archivo no encontrado para vincular: {}", archivoId);
            }
        } catch (Exception e) {
            logger.error("Error al vincular archivo con usuario", e);
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private Path ensamblarArchivo(SessionRegistroInfo session) throws IOException {
        Path sessionDir = Paths.get(config.getArchivos().getDirectorioTemp(), session.sessionId);
        Path archivoCompleto = sessionDir.resolve("archivo_completo");

        if (!Files.exists(sessionDir)) {
            throw new IOException("Directorio de sesión no encontrado: " + sessionDir);
        }

        List<Path> chunkPaths = new ArrayList<>();
        for (int i = 0; i < session.totalChunks; i++) {
            Path chunkPath = sessionDir.resolve("chunk_" + i);
            if (!Files.exists(chunkPath)) {
                throw new IOException("Chunk faltante: " + chunkPath.getFileName());
            }
            chunkPaths.add(chunkPath);
        }

        logger.info("Ensamblando {} chunks de registro", session.totalChunks);

        try (var outputStream = Files.newOutputStream(archivoCompleto)) {
            for (Path chunkPath : chunkPaths) {
                Files.copy(chunkPath, outputStream);
            }
        }

        logger.info("Archivo de registro ensamblado: {} bytes", Files.size(archivoCompleto));
        return archivoCompleto;
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        int lastDot = nombreArchivo.lastIndexOf('.');
        if (lastDot > 0 && lastDot < nombreArchivo.length() - 1) {
            return nombreArchivo.substring(lastDot + 1).toLowerCase();
        }
        return null;
    }

    private void crearDirectorioSesion(String sessionId) throws IOException {
        Path sessionDir = Paths.get(config.getArchivos().getDirectorioTemp(), sessionId);
        Files.createDirectories(sessionDir);
    }

    private void eliminarDirectorioSesion(String sessionId) {
        try {
            Path sessionDir = Paths.get(config.getArchivos().getDirectorioTemp(), sessionId);
            if (Files.exists(sessionDir)) {
                Files.walk(sessionDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.warn("No se pudo eliminar: {}", path);
                        }
                    });
            }
        } catch (IOException e) {
            logger.warn("Error al limpiar directorio de registro: {}", e.getMessage());
        }
    }
}
