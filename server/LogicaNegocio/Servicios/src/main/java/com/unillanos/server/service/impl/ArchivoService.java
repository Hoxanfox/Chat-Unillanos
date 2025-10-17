package com.unillanos.server.service.impl;

import com.unillanos.server.config.ServerConfigProperties;
import com.unillanos.server.dto.*;
import com.unillanos.server.exception.AuthenticationException;
import com.unillanos.server.exception.NotFoundException;
import com.unillanos.server.exception.ValidationException;
import com.unillanos.server.repository.interfaces.IArchivoRepository;
import com.unillanos.server.repository.interfaces.IUsuarioRepository;
import com.unillanos.server.entity.ArchivoEntity;
import com.unillanos.server.entity.TipoArchivo;
import com.unillanos.server.entity.UsuarioEntity;
import com.unillanos.server.validation.SubirArchivoValidator;
import com.unillanos.server.validation.TipoArchivoValidator;
import com.unillanos.server.validation.TamanoArchivoValidator;
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
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio para gestión de archivos multimedia: subir, descargar, listar y deduplicación por hash.
 */
@Service
public class ArchivoService {

    private static final Logger logger = LoggerFactory.getLogger(ArchivoService.class);

    private final IArchivoRepository archivoRepository;
    private final IUsuarioRepository usuarioRepository;
    private final LoggerService loggerService;
    private final ServerConfigProperties config;

    public ArchivoService(IArchivoRepository archivoRepository,
                          IUsuarioRepository usuarioRepository,
                          LoggerService loggerService,
                          ServerConfigProperties config) {
        this.archivoRepository = archivoRepository;
        this.usuarioRepository = usuarioRepository;
        this.loggerService = loggerService;
        this.config = config;
        inicializarDirectorios();
    }

    /**
     * Sube un archivo al sistema con validaciones y deduplicación por hash.
     */
    public DTOResponse subirArchivo(DTOSubirArchivo dto) {
        try {
            // 1. Validar datos
            SubirArchivoValidator.validate(dto, config);

            // 2. Verificar usuario
            Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findById(dto.getUsuarioId());
            if (usuarioOpt.isEmpty()) {
                throw new NotFoundException("Usuario no encontrado", "USER_NOT_FOUND");
            }

            // 3. Decodificar Base64
            byte[] data;
            try {
                data = Base64.getDecoder().decode(dto.getBase64Data());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Contenido Base64 inválido", "base64Data");
            }

            // 4. Calcular hash
            String hash = calcularHash(data);

            // 5. Deduplicación
            Optional<ArchivoEntity> existente = archivoRepository.findByHash(hash);
            if (existente.isPresent()) {
                DTOArchivo dtoArchivo = existente.get().toDTO(true);
                loggerService.logInfo("subirArchivo", "Archivo duplicado reutilizado: " + dtoArchivo.getId());
                return DTOResponse.success("subirArchivo", "Archivo ya existe, se reutilizó la copia existente", dtoArchivo);
            }

            // 6. Determinar tipo y validar tamaño por tipo
            String tipoLogico = TipoArchivoValidator.detectarTipo(dto.getTipoMime());
            if (tipoLogico == null) {
                throw new ValidationException("Tipo MIME no permitido", "tipoMime");
            }
            TamanoArchivoValidator.validate(dto.getTamanoBytes(), tipoLogico, config);

            // 7. Generar IDs y ruta de almacenamiento
            String id = UUID.randomUUID().toString();
            String extension = obtenerExtension(dto.getNombreArchivo());
            String nombreAlmacenado = id + (extension != null ? ("." + extension) : "");
            TipoArchivo tipoEnum = TipoArchivo.fromString(tipoLogico);
            String subdirectorio = subdirectorioPorTipo(tipoEnum);
            String rutaRelativa = Paths.get(subdirectorio, nombreAlmacenado).toString();

            // 8. Guardar en disco
            guardarEnDisco(data, rutaRelativa);

            // 9. Persistir en BD
            ArchivoEntity archivo = new ArchivoEntity();
            archivo.setId(id);
            archivo.setNombreOriginal(dto.getNombreArchivo());
            archivo.setNombreAlmacenado(nombreAlmacenado);
            archivo.setTipoMime(dto.getTipoMime());
            archivo.setTipoArchivo(tipoEnum);
            archivo.setHashSha256(hash);
            archivo.setTamanoBytes(dto.getTamanoBytes());
            archivo.setRutaAlmacenamiento(rutaRelativa);
            archivo.setUsuarioId(dto.getUsuarioId());
            archivo.setFechaSubida(LocalDateTime.now());

            ArchivoEntity guardado = archivoRepository.save(archivo);

            // 10. Log y respuesta
            DTOArchivo dtoArchivo = guardado.toDTO(false);
            loggerService.logInfo("subirArchivo", "Archivo subido: " + dtoArchivo.getId());
            return DTOResponse.success("subirArchivo", "Archivo subido exitosamente", dtoArchivo);

        } catch (ValidationException | NotFoundException e) {
            throw e;
        } catch (NoSuchAlgorithmException e) {
            logger.error("Algoritmo de hash no disponible", e);
            throw new RuntimeException("Error al calcular hash del archivo", e);
        } catch (IOException e) {
            logger.error("Error de E/S al guardar archivo", e);
            throw new RuntimeException("Error al guardar archivo en disco", e);
        } catch (Exception e) {
            logger.error("Error inesperado al subir archivo", e);
            throw new RuntimeException("Error al subir archivo", e);
        }
    }

    /**
     * Descarga un archivo del sistema y retorna su contenido en Base64.
     */
    public DTOResponse descargarArchivo(DTODescargarArchivo dto) {
        try {
            if (dto == null || dto.getArchivoId() == null || dto.getArchivoId().trim().isEmpty()) {
                throw new ValidationException("El ID del archivo es requerido", "archivoId");
            }
            if (dto.getUsuarioId() == null || dto.getUsuarioId().trim().isEmpty()) {
                throw new ValidationException("El ID del usuario es requerido", "usuarioId");
            }

            Optional<ArchivoEntity> archivoOpt = archivoRepository.findById(dto.getArchivoId());
            if (archivoOpt.isEmpty()) {
                throw new NotFoundException("Archivo no encontrado", "FILE_NOT_FOUND");
            }
            ArchivoEntity archivo = archivoOpt.get();

            // Permisos básicos: dueño del archivo
            if (!archivo.getUsuarioId().equals(dto.getUsuarioId())) {
                // En el futuro: validar si el archivo está en un mensaje visible para el usuario
                throw new AuthenticationException("No tienes permisos para descargar este archivo", "FILE_DOWNLOAD_FORBIDDEN");
            }

            byte[] data = leerDesdeDisco(archivo.getRutaAlmacenamiento());
            String base64 = Base64.getEncoder().encodeToString(data);

            DTOArchivoData dataDTO = new DTOArchivoData(
                archivo.getId(),
                archivo.getNombreOriginal(),
                archivo.getTipoMime(),
                archivo.getTamanoBytes(),
                base64
            );

            loggerService.logInfo("descargarArchivo", "Archivo descargado: " + archivo.getId());
            return DTOResponse.success("descargarArchivo", "Archivo descargado exitosamente", dataDTO);
        } catch (ValidationException | NotFoundException | AuthenticationException e) {
            throw e;
        } catch (IOException e) {
            logger.error("Error al leer archivo desde disco", e);
            throw new RuntimeException("Error al leer archivo desde disco", e);
        } catch (Exception e) {
            logger.error("Error inesperado al descargar archivo", e);
            throw new RuntimeException("Error al descargar archivo", e);
        }
    }

    /**
     * Lista archivos del usuario con opción de filtrar por tipo.
     */
    public DTOResponse listarArchivos(DTOListarArchivos dto) {
        try {
            if (dto == null || dto.getUsuarioId() == null || dto.getUsuarioId().trim().isEmpty()) {
                throw new ValidationException("El ID del usuario es requerido", "usuarioId");
            }

            int limit = dto.getLimit() > 0 && dto.getLimit() <= 100 ? dto.getLimit() : 50;
            int offset = dto.getOffset() >= 0 ? dto.getOffset() : 0;

            List<ArchivoEntity> archivos;
            if (dto.getTipoArchivo() != null && !dto.getTipoArchivo().trim().isEmpty()) {
                TipoArchivo tipo = TipoArchivo.fromString(dto.getTipoArchivo());
                archivos = archivoRepository.findByUsuarioYTipo(dto.getUsuarioId(), tipo, limit, offset);
            } else {
                archivos = archivoRepository.findByUsuario(dto.getUsuarioId(), limit, offset);
            }

            List<DTOArchivo> respuesta = archivos.stream()
                .map(a -> a.toDTO(false))
                .toList();

            return DTOResponse.success("listarArchivos", String.format("%d archivos encontrados", respuesta.size()), respuesta);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al listar archivos", e);
            throw new RuntimeException("Error al listar archivos", e);
        }
    }

    private String calcularHash(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private void inicializarDirectorios() {
        try {
            Path baseDir = Paths.get(config.getArchivos().getDirectorioBase());
            Files.createDirectories(baseDir.resolve("imagenes"));
            Files.createDirectories(baseDir.resolve("audios"));
            Files.createDirectories(baseDir.resolve("documentos"));
        } catch (IOException e) {
            logger.error("Error al crear directorios de almacenamiento", e);
            throw new RuntimeException("Error al inicializar directorios", e);
        }
    }

    private void guardarEnDisco(byte[] data, String rutaRelativa) throws IOException {
        Path rutaCompleta = Paths.get(config.getArchivos().getDirectorioBase(), rutaRelativa);
        Files.createDirectories(rutaCompleta.getParent());
        Files.write(rutaCompleta, data);
    }

    private byte[] leerDesdeDisco(String rutaRelativa) throws IOException {
        Path rutaCompleta = Paths.get(config.getArchivos().getDirectorioBase(), rutaRelativa);
        return Files.readAllBytes(rutaCompleta);
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null) return null;
        int idx = nombreArchivo.lastIndexOf('.');
        if (idx == -1 || idx == nombreArchivo.length() - 1) return null;
        return nombreArchivo.substring(idx + 1).toLowerCase();
    }

    private String subdirectorioPorTipo(TipoArchivo tipo) {
        return switch (tipo) {
            case IMAGE -> "imagenes";
            case AUDIO -> "audios";
            case DOCUMENT -> "documentos";
        };
    }
}
