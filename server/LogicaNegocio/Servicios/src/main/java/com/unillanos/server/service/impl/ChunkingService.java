package com.unillanos.server.service.impl;

import com.unillanos.server.config.ServerConfigProperties;
import com.unillanos.server.dto.*;
import com.unillanos.server.exception.NotFoundException;
import com.unillanos.server.exception.ValidationException;
import com.unillanos.server.repository.interfaces.IArchivoRepository;
import com.unillanos.server.repository.interfaces.IChunkSessionRepository;
import com.unillanos.server.repository.interfaces.IUsuarioRepository;
import com.unillanos.server.repository.models.ArchivoEntity;
import com.unillanos.server.repository.models.ChunkSessionEntity;
import com.unillanos.server.repository.models.EstadoSesion;
import com.unillanos.server.repository.models.TipoArchivo;
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
    public DTOResponse iniciarSubida(DTOIniciarSubida dto) {
        try {
            // 1. Validar datos
            ChunkValidator.validateIniciarSubida(dto, config);
            
            // 2. Verificar usuario
            if (usuarioRepository.findById(dto.getUsuarioId()).isEmpty()) {
                throw new NotFoundException("Usuario no encontrado", "USER_NOT_FOUND");
            }
            
            // 3. Crear sesión
            String sessionId = UUID.randomUUID().toString();
            ChunkSessionEntity session = new ChunkSessionEntity(
                sessionId,
                dto.getUsuarioId(),
                dto.getNombreArchivo(),
                dto.getTipoMime(),
                dto.getTamanoTotal(),
                dto.getTotalChunks(),
                new HashSet<>(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                EstadoSesion.ACTIVA
            );
            
            ChunkSessionEntity sessionGuardada = chunkSessionRepository.iniciarSesion(session);
            
            // 4. Crear directorio temporal para la sesión
            crearDirectorioSesion(sessionId);
            
            // 5. Preparar respuesta
            DTOIniciarSubidaResponse response = new DTOIniciarSubidaResponse(
                sessionId,
                config.getArchivos().getChunkSize(),
                new ArrayList<>()
            );
            
            loggerService.logInfo("iniciarSubida", 
                String.format("Sesión de subida iniciada: %s para usuario %s", sessionId, dto.getUsuarioId()));
            
            return DTOResponse.success("iniciar_subida", "Sesión de subida iniciada", response);
            
        } catch (ValidationException | NotFoundException e) {
            logger.warn("Error al iniciar subida: {}", e.getMessage());
            return DTOResponse.error("iniciar_subida", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al iniciar subida: {}", e.getMessage(), e);
            return DTOResponse.error("iniciar_subida", "Error interno del servidor al iniciar subida.");
        }
    }

    /**
     * Inicia una nueva sesión de subida de archivo para el proceso de registro.
     * Permite subir archivos sin validar que el usuario existe en la base de datos.
     *
     * @param dto Datos para iniciar la subida
     * @return Respuesta con sessionId y configuración
     */
    public DTOResponse iniciarSubidaParaRegistro(DTOIniciarSubida dto) {
        try {
            // 1. Validar datos básicos (sin validar usuario)
            ChunkValidator.validateIniciarSubidaParaRegistro(dto, config);
            
            // 2. Crear sesión (sin validar usuario)
            String sessionId = UUID.randomUUID().toString();
            ChunkSessionEntity session = new ChunkSessionEntity(
                sessionId,
                dto.getUsuarioId(),
                dto.getNombreArchivo(),
                dto.getTipoMime(),
                dto.getTamanoTotal(),
                dto.getTotalChunks(),
                new HashSet<>(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                EstadoSesion.ACTIVA
            );
            
            ChunkSessionEntity sessionGuardada = chunkSessionRepository.iniciarSesion(session);
            
            // 3. Crear directorio temporal para la sesión
            crearDirectorioSesion(sessionId);
            
            // 4. Preparar respuesta específica para registro
            DTOIniciarSubidaParaRegistroResponse response = new DTOIniciarSubidaParaRegistroResponse(
                sessionId,
                config.getArchivos().getChunkSize(),
                new ArrayList<>()
            );
            
            loggerService.logInfo("iniciarSubidaParaRegistro", 
                String.format("Sesión de subida para registro iniciada: %s para usuario temporal %s", sessionId, dto.getUsuarioId()));
            
            return DTOResponse.success("uploadFileForRegistration", "Sesión de subida para registro iniciada", response);
            
        } catch (ValidationException e) {
            logger.warn("Error al iniciar subida para registro: {}", e.getMessage());
            return DTOResponse.error("uploadFileForRegistration", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al iniciar subida para registro", e);
            return DTOResponse.error("uploadFileForRegistration", "Error interno del servidor al iniciar subida para registro.");
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
     * Sube un chunk individual de un archivo durante el proceso de registro.
     * Similar a subirChunk pero sin validar hash ni usuario.
     *
     * @param dto Datos del chunk a subir
     * @return Respuesta de confirmación
     */
    public DTOResponse subirChunkParaRegistro(DTOSubirArchivoChunk dto) {
        try {
            // 1. Validar chunk (sin validar usuario ni hash)
            ChunkValidator.validateSubirChunkParaRegistro(dto, config);
            
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
                return DTOResponse.success("subir_chunk_para_registro", "Chunk ya recibido", null);
            }
            
            // 5. NO verificar hash del chunk para registro (se omite por simplicidad)
            
            // 6. Guardar chunk en disco
            byte[] chunkData = Base64.getDecoder().decode(dto.getBase64ChunkData());
            // Convertir de 1-based (cliente) a 0-based (servidor interno)
            int chunkIndex = dto.getNumeroChunk() - 1;
            Path chunkPath = Paths.get(config.getArchivos().getDirectorioTemp(), dto.getSessionId(), 
                "chunk_" + chunkIndex);
            Files.write(chunkPath, chunkData);
            
            // 7. Registrar chunk en la sesión
            chunkSessionRepository.registrarChunk(dto.getSessionId(), dto.getNumeroChunk());
            
            logger.debug("Chunk {} guardado para sesión de registro {}",
                dto.getNumeroChunk(), dto.getSessionId());
            
            return DTOResponse.success("subir_chunk_para_registro", "Chunk recibido exitosamente", null);
            
        } catch (ValidationException | NotFoundException e) {
            logger.warn("Error al subir chunk para registro: {}", e.getMessage());
            return DTOResponse.error("subir_chunk_para_registro", e.getMessage());
        } catch (IOException e) {
            logger.error("Error de E/S al guardar chunk para registro: {}", e.getMessage(), e);
            return DTOResponse.error("subir_chunk_para_registro", "Error interno del servidor al guardar chunk.");
        } catch (Exception e) {
            logger.error("Error inesperado al subir chunk para registro: {}", e.getMessage(), e);
            return DTOResponse.error("subir_chunk_para_registro", "Error inesperado al procesar chunk.");
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
            Path rutaFinal = Paths.get(config.getArchivos().getDirectorioBase(), 
                tipoLogico.toLowerCase(), nombreAlmacenado);
            
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
        Path archivoCompleto = Paths.get(config.getArchivos().getDirectorioTemp(), 
            session.getSessionId(), "archivo_completo");
        
        List<Path> chunkPaths = new ArrayList<>();
        for (int i = 0; i < session.getTotalChunks(); i++) {
            chunkPaths.add(Paths.get(config.getArchivos().getDirectorioTemp(), 
                session.getSessionId(), "chunk_" + i));
        }
        
        // Verificar que todos los chunks existan
        for (Path chunkPath : chunkPaths) {
            if (!Files.exists(chunkPath)) {
                throw new IOException("Chunk faltante: " + chunkPath.getFileName());
            }
        }
        
        // Concatenar chunks
        try (var outputStream = Files.newOutputStream(archivoCompleto)) {
            for (Path chunkPath : chunkPaths) {
                Files.copy(chunkPath, outputStream);
            }
        }
        
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
}
