package com.unillanos.server.service.impl;

import com.unillanos.server.config.ServerConfigProperties;
import com.unillanos.server.dto.*;
import com.unillanos.server.exception.NotFoundException;
import com.unillanos.server.exception.ValidationException;
import com.unillanos.server.repository.interfaces.IArchivoRepository;
import com.unillanos.server.repository.interfaces.IChunkSessionRepository;
import com.unillanos.server.repository.interfaces.IUsuarioRepository;
import com.unillanos.server.entity.ArchivoEntity;
import com.unillanos.server.entity.ChunkSessionEntity;
import com.unillanos.server.entity.EstadoSesion;
import com.unillanos.server.entity.TipoArchivo;
import com.unillanos.server.validation.ChunkValidator;
import com.unillanos.server.validation.TipoArchivoValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de subida y descarga de archivos por chunks.
 * Permite transferir archivos grandes dividiéndolos en trozos de 2 MB.
 */
@Service
public class ChunkingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChunkingService.class);
    
    private final IChunkSessionRepository chunkSessionRepository;
    private final IArchivoRepository archivoRepository;
    private final IUsuarioRepository usuarioRepository;
    private final LoggerService loggerService;
    private final ServerConfigProperties config;

    public ChunkingService(IChunkSessionRepository chunkSessionRepository,
                           IArchivoRepository archivoRepository,
                           IUsuarioRepository usuarioRepository,
                           LoggerService loggerService,
                           ServerConfigProperties config) {
        this.chunkSessionRepository = chunkSessionRepository;
        this.archivoRepository = archivoRepository;
        this.usuarioRepository = usuarioRepository;
        this.loggerService = loggerService;
        this.config = config;
        inicializarDirectorios();
    }

    /**
     * Inicia una nueva sesión de subida de archivo.
     *
     * @param dto Datos para iniciar la subida
     * @return Respuesta con sessionId y configuración
     */
    public DTOResponse iniciarSubida(com.unillanos.server.dto.DTOIniciarSubida dto) {
        try {
            // 1. Validar datos
            com.unillanos.server.validation.ChunkValidator.validateIniciarSubida(dto, config);
            
            // 2. Verificar usuario
            if (usuarioRepository.findById(dto.getUsuarioId()).isEmpty()) {
                throw new com.unillanos.server.exception.NotFoundException("Usuario no encontrado", "USER_NOT_FOUND");
            }
            
            // 3. Crear sesión
            String sessionId = java.util.UUID.randomUUID().toString();
            com.unillanos.server.entity.ChunkSessionEntity session = new com.unillanos.server.entity.ChunkSessionEntity(
                sessionId,
                dto.getUsuarioId(),
                dto.getNombreArchivo(),
                dto.getTipoMime(),
                dto.getTamanoTotal(),
                dto.getTotalChunks(),
                new java.util.HashSet<>(),
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now(),
                com.unillanos.server.entity.EstadoSesion.ACTIVA
            );
            
            com.unillanos.server.entity.ChunkSessionEntity sessionGuardada = chunkSessionRepository.iniciarSesion(session);
            
            // 4. Crear directorio temporal para la sesión
            crearDirectorioSesion(sessionId);
            
            // 5. Preparar respuesta
            com.unillanos.server.dto.DTOIniciarSubidaResponse response = new com.unillanos.server.dto.DTOIniciarSubidaResponse(
                sessionId,
                config.getArchivos().getChunkSize(),
                new java.util.ArrayList<>()
            );
            
            loggerService.logInfo("iniciarSubida", 
                String.format("Sesión de subida iniciada: %s para usuario %s", sessionId, dto.getUsuarioId()));
            
            return com.unillanos.server.dto.DTOResponse.success("iniciar_subida", "Sesión de subida iniciada", response);
            
        } catch (com.unillanos.server.exception.ValidationException | com.unillanos.server.exception.NotFoundException e) {
            logger.warn("Error al iniciar subida: {}", e.getMessage());
            return com.unillanos.server.dto.DTOResponse.error("iniciar_subida", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al iniciar subida: {}", e.getMessage(), e);
            return com.unillanos.server.dto.DTOResponse.error("iniciar_subida", "Error interno del servidor al iniciar subida.");
        }
    }



    /**
     * Sube un chunk individual de un archivo.
     *
     * @param dto Datos del chunk a subir
     * @return Respuesta de confirmación
     */
    public DTOResponse subirChunk(DTOSubirArchivoChunk dto) {
        try {
            // 1. Validar chunk
            ChunkValidator.validateSubirChunk(dto, config);
            
            // 2. Verificar sesión
            Optional<ChunkSessionEntity> sessionOpt = chunkSessionRepository.obtenerSesion(dto.getSessionId());
            if (sessionOpt.isEmpty()) {
                throw new NotFoundException("Sesión no encontrada", "SESSION_NOT_FOUND");
            }
            
            ChunkSessionEntity session = sessionOpt.get();
            
            // 3. Verificar que la sesión esté activa
            if (session.getEstadoSesion() != EstadoSesion.ACTIVA) {
                throw new ValidationException("La sesión no está activa", "sessionId");
            }
            
            // 4. Verificar que el chunk no haya sido recibido previamente
            if (session.getChunksRecibidos().contains(dto.getNumeroChunk())) {
                logger.info("Chunk {} ya recibido para sesión {}", dto.getNumeroChunk(), dto.getSessionId());
                return DTOResponse.success("subir_chunk", "Chunk ya recibido", null);
            }
            
            // 5. Verificar hash del chunk
            byte[] chunkData = Base64.getDecoder().decode(dto.getBase64ChunkData());
            String hashCalculado = calculateSha256(chunkData);
            if (!hashCalculado.equals(dto.getHashChunk())) {
                throw new ValidationException("Hash del chunk no coincide", "hashChunk");
            }
            
            // 6. Guardar chunk en disco
            // Convertir de 1-based (cliente) a 0-based (servidor interno)
            int chunkIndex = dto.getNumeroChunk() - 1;
            Path chunkPath = Paths.get(config.getArchivos().getDirectorioTemp(), dto.getSessionId(), 
                "chunk_" + chunkIndex);
            Files.write(chunkPath, chunkData);
            
            // 7. Registrar chunk en la sesión
            chunkSessionRepository.registrarChunk(dto.getSessionId(), dto.getNumeroChunk());
            
            logger.debug("Chunk {} guardado para sesión {}", dto.getNumeroChunk(), dto.getSessionId());
            
            return DTOResponse.success("subir_chunk", "Chunk recibido exitosamente", null);
            
        } catch (ValidationException | NotFoundException e) {
            logger.warn("Error al subir chunk: {}", e.getMessage());
            return DTOResponse.error("subir_chunk", e.getMessage());
        } catch (IOException e) {
            logger.error("Error de E/S al guardar chunk: {}", e.getMessage(), e);
            return DTOResponse.error("subir_chunk", "Error interno del servidor al guardar chunk.");
        } catch (Exception e) {
            logger.error("Error inesperado al subir chunk: {}", e.getMessage(), e);
            return DTOResponse.error("subir_chunk", "Error inesperado al procesar chunk.");
        }
    }


    /**
     * Obtiene información básica de una sesión de chunking.
     *
     * @param sessionId ID de la sesión
     * @return Respuesta con la información de la sesión
     */
    public DTOResponse obtenerInformacionSesion(String sessionId) {
        try {
            // Verificar sesión
            Optional<ChunkSessionEntity> sessionOpt = chunkSessionRepository.obtenerSesion(sessionId);
            if (sessionOpt.isEmpty()) {
                return DTOResponse.error("obtener_informacion_sesion", "Sesión no encontrada");
            }
            
            ChunkSessionEntity session = sessionOpt.get();
            
            // Preparar respuesta con información básica
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionId", session.getSessionId());
            sessionInfo.put("usuarioId", session.getUsuarioId());
            sessionInfo.put("nombreArchivo", session.getNombreArchivo());
            sessionInfo.put("tipoMime", session.getTipoMime());
            sessionInfo.put("tamanoTotal", session.getTamanoTotal());
            sessionInfo.put("totalChunks", session.getTotalChunks());
            sessionInfo.put("chunksRecibidos", session.getChunksRecibidos());
            sessionInfo.put("estadoSesion", session.getEstadoSesion().toString());
            sessionInfo.put("fechaCreacion", session.getFechaInicio().toString());
            sessionInfo.put("fechaUltimaActividad", session.getUltimaActividad().toString());
            
            return DTOResponse.success("obtener_informacion_sesion", "Información de sesión obtenida", sessionInfo);
            
        } catch (Exception e) {
            logger.error("Error al obtener información de sesión: {}", e.getMessage(), e);
            return DTOResponse.error("obtener_informacion_sesion", "Error interno del servidor");
        }
    }

    /**
     * Finaliza una subida y ensambla el archivo completo.
     *
     * @param sessionId ID de la sesión a finalizar
     * @return Respuesta con información del archivo creado
     */
    public DTOResponse finalizarSubida(String sessionId) {
        try {
            // 1. Validar sessionId
            ChunkValidator.validateSessionId(sessionId);
            
            // 2. Obtener sesión
            Optional<ChunkSessionEntity> sessionOpt = chunkSessionRepository.obtenerSesion(sessionId);
            if (sessionOpt.isEmpty()) {
                throw new NotFoundException("Sesión no encontrada", "SESSION_NOT_FOUND");
            }
            
            ChunkSessionEntity session = sessionOpt.get();
            
            // 3. Verificar que todos los chunks estén presentes
            Set<Integer> chunksRecibidos = session.getChunksRecibidos();
            if (chunksRecibidos.size() != session.getTotalChunks()) {
                throw new ValidationException(
                    String.format("Faltan chunks. Recibidos: %d, esperados: %d", 
                        chunksRecibidos.size(), session.getTotalChunks()), 
                    "sessionId"
                );
            }
            
            // 4. Ensamblar archivo
            Path archivoCompleto = ensamblarArchivo(session);
            
            // 5. Verificar hash del archivo completo
            String hashArchivo = calculateSha256(Files.readAllBytes(archivoCompleto));
            
            // 6. Verificar duplicados
            Optional<ArchivoEntity> archivoExistente = archivoRepository.findByHash(hashArchivo);
            if (archivoExistente.isPresent()) {
                // Eliminar archivo temporal
                Files.deleteIfExists(archivoCompleto);
                eliminarDirectorioSesion(sessionId);
                chunkSessionRepository.actualizarEstado(sessionId, EstadoSesion.COMPLETADA);
                
                logger.info("Archivo duplicado detectado (hash: {}), usando archivo existente", hashArchivo);
                return DTOResponse.success("finalizar_subida", "Archivo subido exitosamente (duplicado)", 
                    archivoExistente.get().toDTO(true));
            }
            
            // 7. Mover archivo a ubicación final
            String tipoLogico = TipoArchivoValidator.detectarTipo(session.getTipoMime());
            String archivoId = UUID.randomUUID().toString();
            String extension = obtenerExtension(session.getNombreArchivo());
            String nombreAlmacenado = archivoId + (extension != null ? ("." + extension) : "");
            Path directorioDestino = Paths.get(config.getArchivos().getDirectorioBase(), tipoLogico.toLowerCase());
            Path rutaFinal = directorioDestino.resolve(nombreAlmacenado);
            
            // Crear directorio de destino si no existe
            Files.createDirectories(directorioDestino);
            
            logger.info("Moviendo archivo de {} a {}", archivoCompleto, rutaFinal);
            Files.move(archivoCompleto, rutaFinal);
            
            // 8. Crear entrada en base de datos
            ArchivoEntity archivo = new ArchivoEntity();
            archivo.setId(archivoId);
            archivo.setNombreOriginal(session.getNombreArchivo());
            archivo.setNombreAlmacenado(nombreAlmacenado);
            archivo.setTipoMime(session.getTipoMime());
            archivo.setTamanoBytes(session.getTamanoTotal());
            archivo.setHashSha256(hashArchivo);
            archivo.setRutaAlmacenamiento(Paths.get(tipoLogico.toLowerCase(), nombreAlmacenado).toString());
            archivo.setFechaSubida(LocalDateTime.now());
            archivo.setUsuarioId(session.getUsuarioId());
            
            ArchivoEntity archivoGuardado = archivoRepository.save(archivo);
            
            // 9. Limpiar recursos temporales
            eliminarDirectorioSesion(sessionId);
            chunkSessionRepository.actualizarEstado(sessionId, EstadoSesion.COMPLETADA);
            
            loggerService.logInfo("finalizar_subida", 
                String.format("Archivo ensamblado exitosamente: %s (%d bytes)", 
                    session.getNombreArchivo(), session.getTamanoTotal()));
            
            return DTOResponse.success("finalizar_subida", "Archivo subido exitosamente", 
                archivoGuardado.toDTO(false));
            
        } catch (ValidationException | NotFoundException e) {
            logger.warn("Error al finalizar subida: {}", e.getMessage());
            return DTOResponse.error("finalizar_subida", e.getMessage());
        } catch (IOException e) {
            logger.error("Error de E/S al finalizar subida: {}", e.getMessage(), e);
            return DTOResponse.error("finalizar_subida", "Error interno del servidor al procesar archivo.");
        } catch (Exception e) {
            logger.error("Error inesperado al finalizar subida: {}", e.getMessage(), e);
            return DTOResponse.error("finalizar_subida", "Error inesperado al procesar archivo.");
        }
    }

    /**
     * Descarga un chunk específico de un archivo.
     *
     * @param dto Datos de la descarga
     * @return Respuesta con el chunk solicitado
     */
    public DTOResponse descargarChunk(DTODescargarArchivoChunk dto) {
        try {
            // 1. Validar datos
            ChunkValidator.validateDescargarChunk(dto);
            
            // 2. Verificar archivo
            Optional<ArchivoEntity> archivoOpt = archivoRepository.findById(dto.getArchivoId());
            if (archivoOpt.isEmpty()) {
                throw new NotFoundException("Archivo no encontrado", "FILE_NOT_FOUND");
            }
            
            ArchivoEntity archivo = archivoOpt.get();
            
            // 3. Calcular número total de chunks
            int chunkSize = config.getArchivos().getChunkSize();
            int totalChunks = (int) Math.ceil((double) archivo.getTamanoBytes() / chunkSize);
            
            // 4. Validar número de chunk
            ChunkValidator.validateChunkNumber(dto.getNumeroChunk(), totalChunks);
            
            // 5. Leer archivo y extraer chunk
            Path archivoPath = Paths.get(config.getArchivos().getDirectorioBase(), 
                archivo.getRutaAlmacenamiento());
            
            if (!Files.exists(archivoPath)) {
                throw new NotFoundException("Archivo físico no encontrado", "FILE_PHYSICAL_NOT_FOUND");
            }
            
            byte[] archivoCompleto = Files.readAllBytes(archivoPath);
            
            // 6. Extraer chunk específico
            int offset = dto.getNumeroChunk() * chunkSize;
            int chunkLength = Math.min(chunkSize, archivoCompleto.length - offset);
            
            byte[] chunkData = Arrays.copyOfRange(archivoCompleto, offset, offset + chunkLength);
            String base64ChunkData = Base64.getEncoder().encodeToString(chunkData);
            String hashChunk = calculateSha256(chunkData);
            
            // 7. Crear respuesta
            DTODescargarArchivoChunkResponse response = new DTODescargarArchivoChunkResponse(
                dto.getNumeroChunk(),
                totalChunks,
                base64ChunkData,
                hashChunk,
                dto.getNumeroChunk() == totalChunks - 1
            );
            
            return DTOResponse.success("descargar_chunk", "Chunk descargado exitosamente", response);
            
        } catch (ValidationException | NotFoundException e) {
            logger.warn("Error al descargar chunk: {}", e.getMessage());
            return DTOResponse.error("descargar_chunk", e.getMessage());
        } catch (IOException e) {
            logger.error("Error de E/S al descargar chunk: {}", e.getMessage(), e);
            return DTOResponse.error("descargar_chunk", "Error interno del servidor al leer archivo.");
        } catch (Exception e) {
            logger.error("Error inesperado al descargar chunk: {}", e.getMessage(), e);
            return DTOResponse.error("descargar_chunk", "Error inesperado al procesar descarga.");
        }
    }

    /**
     * Ensambla un archivo completo a partir de sus chunks.
     */
    private Path ensamblarArchivo(ChunkSessionEntity session) throws IOException {
        Path sessionDir = Paths.get(config.getArchivos().getDirectorioTemp(), session.getSessionId());
        Path archivoCompleto = sessionDir.resolve("archivo_completo");
        
        // Asegurar que el directorio de sesión existe
        if (!Files.exists(sessionDir)) {
            logger.error("Directorio de sesión no existe: {}", sessionDir);
            throw new IOException("Directorio de sesión no encontrado: " + sessionDir);
        }
        
        List<Path> chunkPaths = new ArrayList<>();
        for (int i = 0; i < session.getTotalChunks(); i++) {
            chunkPaths.add(sessionDir.resolve("chunk_" + i));
        }
        
        // Verificar que todos los chunks existan
        for (Path chunkPath : chunkPaths) {
            if (!Files.exists(chunkPath)) {
                logger.error("Chunk faltante: {} (sesión: {})", chunkPath, session.getSessionId());
                throw new IOException("Chunk faltante: " + chunkPath.getFileName());
            }
        }
        
        logger.info("Ensamblando archivo de {} chunks para sesión {}", session.getTotalChunks(), session.getSessionId());
        
        // Concatenar chunks
        try (var outputStream = Files.newOutputStream(archivoCompleto)) {
            for (Path chunkPath : chunkPaths) {
                logger.debug("Copiando chunk: {}", chunkPath);
                Files.copy(chunkPath, outputStream);
            }
        }
        
        // Verificar que el archivo se creó correctamente
        if (!Files.exists(archivoCompleto)) {
            logger.error("No se pudo crear archivo completo: {}", archivoCompleto);
            throw new IOException("Error al ensamblar archivo completo");
        }
        
        long fileSize = Files.size(archivoCompleto);
        logger.info("Archivo ensamblado exitosamente: {} ({} bytes)", archivoCompleto, fileSize);
        
        return archivoCompleto;
    }

    /**
     * Calcula el hash SHA-256 de un array de bytes.
     */
    private String calculateSha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(data);
        StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Obtiene la extensión de un nombre de archivo.
     */
    private String obtenerExtension(String nombreArchivo) {
        int lastDot = nombreArchivo.lastIndexOf('.');
        if (lastDot > 0 && lastDot < nombreArchivo.length() - 1) {
            return nombreArchivo.substring(lastDot + 1).toLowerCase();
        }
        return null;
    }

    /**
     * Crea el directorio temporal para una sesión.
     */
    private void crearDirectorioSesion(String sessionId) throws IOException {
        Path sessionDir = Paths.get(config.getArchivos().getDirectorioTemp(), sessionId);
        Files.createDirectories(sessionDir);
    }

    /**
     * Elimina el directorio temporal de una sesión.
     */
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
                            logger.warn("No se pudo eliminar archivo temporal: {}", path, e);
                        }
                    });
            }
        } catch (IOException e) {
            logger.warn("Error al limpiar directorio temporal de sesión {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Inicializa los directorios necesarios para el chunking.
     */
    private void inicializarDirectorios() {
        try {
            Path baseDir = Paths.get(config.getArchivos().getDirectorioBase());
            Path tempDir = Paths.get(config.getArchivos().getDirectorioTemp());
            
            Files.createDirectories(baseDir.resolve("imagenes"));
            Files.createDirectories(baseDir.resolve("audios"));
            Files.createDirectories(baseDir.resolve("documentos"));
            Files.createDirectories(tempDir);
            
        } catch (IOException e) {
            logger.error("Error al crear directorios de chunking", e);
        }
    }

    // --- MÉTODOS PARA REGISTRO DE USUARIOS (SIN AUTENTICACIÓN) ---

    /**
     * Inicia una sesión de subida de archivo durante el proceso de registro.
     * Similar a iniciarSubida pero sin verificar que el usuario existe.
     *
     * @param dto DTO con los datos de inicio de subida
     * @return DTOResponse con el resultado de la operación
     */
    public DTOResponse iniciarSubidaParaRegistro(DTOIniciarSubida dto) {
        try {
            // 1. Validar datos (sin verificar usuario)
            com.unillanos.server.validation.ChunkValidator.validateIniciarSubidaParaRegistro(dto, config);
            
            // 2. Crear sesión (el usuarioId puede ser temporal)
            String sessionId = java.util.UUID.randomUUID().toString();
            com.unillanos.server.entity.ChunkSessionEntity session = new com.unillanos.server.entity.ChunkSessionEntity(
                sessionId,
                dto.getUsuarioId(), // Puede ser un ID temporal
                dto.getNombreArchivo(),
                dto.getTipoMime(),
                dto.getTamanoTotal(),
                dto.getTotalChunks(),
                new java.util.HashSet<>(),
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now(),
                com.unillanos.server.entity.EstadoSesion.ACTIVA
            );
            
            com.unillanos.server.entity.ChunkSessionEntity sessionGuardada = chunkSessionRepository.iniciarSesion(session);
            
            // 3. Crear directorio temporal para la sesión
            crearDirectorioSesion(sessionId);
            
            // 4. Preparar respuesta
            com.unillanos.server.dto.DTOIniciarSubidaResponse response = new com.unillanos.server.dto.DTOIniciarSubidaResponse(
                sessionId,
                config.getArchivos().getChunkSize(),
                new java.util.ArrayList<>()
            );
            
            loggerService.logInfo("iniciarSubidaParaRegistro", 
                String.format("Sesión de subida para registro iniciada: %s", sessionId));
            
            return com.unillanos.server.dto.DTOResponse.success("uploadFileForRegistration", "Sesión de subida para registro iniciada", response);
            
        } catch (com.unillanos.server.exception.ValidationException e) {
            logger.warn("Error de validación al iniciar subida para registro: {}", e.getMessage());
            return com.unillanos.server.dto.DTOResponse.error("uploadFileForRegistration", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al iniciar subida para registro: {}", e.getMessage(), e);
            return com.unillanos.server.dto.DTOResponse.error("uploadFileForRegistration", "Error interno del servidor al iniciar subida para registro.");
        }
    }

    /**
     * Sube un chunk de archivo durante el proceso de registro.
     * Similar a subirChunk pero sin verificar autenticación del usuario.
     *
     * @param dto DTO con los datos del chunk
     * @return DTOResponse con el resultado de la operación
     */
    public DTOResponse subirChunkParaRegistro(DTOSubirArchivoChunk dto) {
        try {
            // 1. Validar datos (sin verificar usuario)
            com.unillanos.server.validation.ChunkValidator.validateSubirChunkParaRegistro(dto, config);
            
            // 2. Buscar sesión
            Optional<com.unillanos.server.entity.ChunkSessionEntity> sessionOpt = 
                chunkSessionRepository.obtenerSesion(dto.getSessionId());
            
            if (sessionOpt.isEmpty()) {
                throw new com.unillanos.server.exception.NotFoundException("Sesión no encontrada", "SESSION_NOT_FOUND");
            }
            
            com.unillanos.server.entity.ChunkSessionEntity session = sessionOpt.get();
            
            // 3. Verificar que la sesión esté activa
            if (session.getEstadoSesion() != com.unillanos.server.entity.EstadoSesion.ACTIVA) {
                throw new com.unillanos.server.exception.ValidationException("La sesión no está activa", "session");
            }
            
            // 4. Verificar que el chunk no haya sido subido ya
            if (session.getChunksRecibidos().contains(dto.getNumeroChunk())) {
                logger.warn("Chunk {} ya fue subido para sesión {}", dto.getNumeroChunk(), dto.getSessionId());
                return com.unillanos.server.dto.DTOResponse.success("uploadFileChunkForRegistration", "Chunk ya fue subido", null);
            }
            
            // 5. Crear directorio de sesión si no existe
            Path sessionDir = Paths.get(config.getArchivos().getDirectorioTemp(), dto.getSessionId());
            if (!Files.exists(sessionDir)) {
                Files.createDirectories(sessionDir);
            }
            
            // 6. Decodificar y guardar chunk
            byte[] chunkData = Base64.getDecoder().decode(dto.getBase64ChunkData());
            Path chunkPath = sessionDir.resolve("chunk_" + (dto.getNumeroChunk() - 1)); // Convertir a 0-based
            
            Files.write(chunkPath, chunkData);
            
            // 7. Actualizar sesión
            session.getChunksRecibidos().add(dto.getNumeroChunk());
            session.setUltimaActividad(java.time.LocalDateTime.now());
            
            chunkSessionRepository.actualizarSesion(session);
            
            // 8. Preparar respuesta
            com.unillanos.server.dto.DTOSubirArchivoChunkResponse response = new com.unillanos.server.dto.DTOSubirArchivoChunkResponse(
                dto.getNumeroChunk(),
                dto.getTotalChunks(),
                session.getChunksRecibidos().size(),
                session.getChunksRecibidos().size() == dto.getTotalChunks()
            );
            
            loggerService.logInfo("subirChunkParaRegistro", 
                String.format("Chunk %d/%d subido para sesión %s", dto.getNumeroChunk(), dto.getTotalChunks(), dto.getSessionId()));
            
            return com.unillanos.server.dto.DTOResponse.success("uploadFileChunkForRegistration", "Chunk subido exitosamente", response);
            
        } catch (com.unillanos.server.exception.ValidationException | com.unillanos.server.exception.NotFoundException e) {
            logger.warn("Error al subir chunk para registro: {}", e.getMessage());
            return com.unillanos.server.dto.DTOResponse.error("uploadFileChunkForRegistration", e.getMessage());
        } catch (IOException e) {
            logger.error("Error de E/S al subir chunk para registro: {}", e.getMessage(), e);
            return com.unillanos.server.dto.DTOResponse.error("uploadFileChunkForRegistration", "Error interno del servidor al guardar chunk.");
        } catch (Exception e) {
            logger.error("Error inesperado al subir chunk para registro: {}", e.getMessage(), e);
            return com.unillanos.server.dto.DTOResponse.error("uploadFileChunkForRegistration", "Error inesperado al procesar chunk.");
        }
    }

    /**
     * Finaliza la subida de archivo durante el proceso de registro.
     * Similar a finalizarSubida pero sin verificar autenticación del usuario.
     *
     * @param dto DTO con los datos de finalización
     * @return DTOResponse con el resultado de la operación
     */
    public DTOResponse finalizarSubidaParaRegistro(DTOEndUpload dto) {
        try {
            // 1. Validar datos
            if (dto.getSessionId() == null || dto.getSessionId().trim().isEmpty()) {
                throw new com.unillanos.server.exception.ValidationException("El ID de sesión es requerido", "sessionId");
            }
            
            // 2. Buscar sesión
            Optional<com.unillanos.server.entity.ChunkSessionEntity> sessionOpt = 
                chunkSessionRepository.obtenerSesion(dto.getSessionId());
            
            if (sessionOpt.isEmpty()) {
                throw new com.unillanos.server.exception.NotFoundException("Sesión no encontrada", "SESSION_NOT_FOUND");
            }
            
            com.unillanos.server.entity.ChunkSessionEntity session = sessionOpt.get();
            
            // 3. Verificar que la sesión esté activa
            if (session.getEstadoSesion() != com.unillanos.server.entity.EstadoSesion.ACTIVA) {
                throw new com.unillanos.server.exception.ValidationException("La sesión no está activa", "session");
            }
            
            // 4. Verificar que todos los chunks hayan sido recibidos
            if (session.getChunksRecibidos().size() != session.getTotalChunks()) {
                throw new com.unillanos.server.exception.ValidationException(
                    String.format("Faltan chunks. Recibidos: %d, Esperados: %d", 
                        session.getChunksRecibidos().size(), session.getTotalChunks()), 
                    "chunks"
                );
            }
            
            // 5. Ensamblar archivo completo
            Path archivoCompleto = ensamblarArchivo(session);
            
            // 6. Calcular hash del archivo completo
            byte[] archivoData = Files.readAllBytes(archivoCompleto);
            String hashCompleto = calculateSha256(archivoData);
            
            // 7. Verificar si el archivo ya existe (deduplicación)
            Optional<com.unillanos.server.entity.ArchivoEntity> archivoExistente = 
                archivoRepository.findByHash(hashCompleto);
            
            String archivoId;
            if (archivoExistente.isPresent()) {
                // Archivo duplicado - usar el existente
                archivoId = archivoExistente.get().getId();
                loggerService.logInfo("finalizarSubidaParaRegistro", 
                    String.format("Archivo duplicado reutilizado: %s", archivoId));
            } else {
                // Archivo nuevo - crear entidad y mover a directorio final
                archivoId = java.util.UUID.randomUUID().toString();
                String extension = obtenerExtension(session.getNombreArchivo());
                String nombreAlmacenado = archivoId + (extension != null ? ("." + extension) : "");
                
                Path directorioDestino = Paths.get(config.getArchivos().getDirectorioFinal());
                Files.createDirectories(directorioDestino);
                Path archivoFinal = directorioDestino.resolve(nombreAlmacenado);
                
                Files.move(archivoCompleto, archivoFinal);
                
                // Crear entidad de archivo
                com.unillanos.server.entity.ArchivoEntity archivo = new com.unillanos.server.entity.ArchivoEntity(
                    archivoId,
                    session.getNombreArchivo(),
                    nombreAlmacenado,
                    session.getTipoMime(),
                    com.unillanos.server.entity.TipoArchivo.fromString(
                        com.unillanos.server.validation.TipoArchivoValidator.detectarTipo(session.getTipoMime())
                    ),
                    hashCompleto,
                    archivoData.length,
                    archivoFinal.toString(),
                    session.getUsuarioId(), // Puede ser un ID temporal
                    java.time.LocalDateTime.now()
                );
                
                archivoRepository.save(archivo);
                
                loggerService.logInfo("finalizarSubidaParaRegistro", 
                    String.format("Archivo nuevo creado: %s", archivoId));
            }
            
            // 8. Actualizar sesión como completada
            session.setEstadoSesion(com.unillanos.server.entity.EstadoSesion.COMPLETADA);
            session.setUltimaActividad(java.time.LocalDateTime.now());
            chunkSessionRepository.actualizarSesion(session);
            
            // 9. Limpiar archivos temporales
            eliminarDirectorioSesion(dto.getSessionId());
            
            // 10. Preparar respuesta
            com.unillanos.server.dto.DTOEndUploadResponse response = new com.unillanos.server.dto.DTOEndUploadResponse(
                archivoId,
                session.getNombreArchivo(),
                archivoData.length,
                hashCompleto,
                session.getTipoMime()
            );
            
            loggerService.logInfo("finalizarSubidaParaRegistro", 
                String.format("Subida para registro finalizada: %s", archivoId));
            
            return com.unillanos.server.dto.DTOResponse.success("endFileUploadForRegistration", "Archivo subido exitosamente para registro", response);
            
        } catch (com.unillanos.server.exception.ValidationException | com.unillanos.server.exception.NotFoundException e) {
            logger.warn("Error al finalizar subida para registro: {}", e.getMessage());
            return com.unillanos.server.dto.DTOResponse.error("endFileUploadForRegistration", e.getMessage());
        } catch (IOException e) {
            logger.error("Error de E/S al finalizar subida para registro: {}", e.getMessage(), e);
            return com.unillanos.server.dto.DTOResponse.error("endFileUploadForRegistration", "Error interno del servidor al procesar archivo.");
        } catch (Exception e) {
            logger.error("Error inesperado al finalizar subida para registro: {}", e.getMessage(), e);
            return com.unillanos.server.dto.DTOResponse.error("endFileUploadForRegistration", "Error inesperado al finalizar subida.");
        }
    }
}
