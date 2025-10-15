package com.unillanos.server.validation;

import com.unillanos.server.config.ServerConfigProperties;
import com.unillanos.server.dto.DTOIniciarSubida;
import com.unillanos.server.dto.DTOSubirArchivoChunk;
import com.unillanos.server.dto.DTODescargarArchivoChunk;
import com.unillanos.server.exception.ValidationException;
import com.unillanos.server.validation.TipoArchivoValidator;
import com.unillanos.server.validation.TamanoArchivoValidator;

import java.util.UUID;

/**
 * Validador para operaciones de chunking de archivos.
 * Valida sesiones, chunks y parámetros de subida/descarga.
 */
public class ChunkValidator {
    
    /**
     * Valida los datos para iniciar una subida de archivo.
     *
     * @param dto DTO con los datos de inicio de subida
     * @param config Configuración del servidor
     * @throws ValidationException si algún campo no es válido
     */
    public static void validateIniciarSubida(DTOIniciarSubida dto, ServerConfigProperties config) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("Los datos de inicio de subida son requeridos", "dto");
        }
        
        // Validar usuarioId
        if (dto.getUsuarioId() == null || dto.getUsuarioId().trim().isEmpty()) {
            throw new ValidationException("El ID del usuario es requerido", "usuarioId");
        }
        
        // Validar nombreArchivo
        if (dto.getNombreArchivo() == null || dto.getNombreArchivo().trim().isEmpty()) {
            throw new ValidationException("El nombre del archivo es requerido", "nombreArchivo");
        }
        TipoArchivoValidator.validateExtension(dto.getNombreArchivo());
        
        // Validar tipoMime
        TipoArchivoValidator.validateMimeType(dto.getTipoMime());
        
        // Validar tamanoTotal
        if (dto.getTamanoTotal() <= 0) {
            throw new ValidationException("El tamaño total del archivo debe ser mayor a 0", "tamanoTotal");
        }
        
        // Validar totalChunks
        if (dto.getTotalChunks() <= 0) {
            throw new ValidationException("El número total de chunks debe ser mayor a 0", "totalChunks");
        }
        
        // Validar que el tamaño total sea consistente con el número de chunks
        int chunkSize = config.getArchivos().getChunkSize();
        int expectedChunks = (int) Math.ceil((double) dto.getTamanoTotal() / chunkSize);
        if (dto.getTotalChunks() != expectedChunks) {
            throw new ValidationException(
                String.format("Número de chunks inconsistente. Esperado: %d, recibido: %d", 
                    expectedChunks, dto.getTotalChunks()), 
                "totalChunks"
            );
        }
        
        // Validar tamaño por tipo de archivo
        String tipoLogico = TipoArchivoValidator.detectarTipo(dto.getTipoMime());
        TamanoArchivoValidator.validate(dto.getTamanoTotal(), tipoLogico, config);
    }
    
    /**
     * Valida un chunk individual a subir.
     *
     * @param dto DTO con los datos del chunk
     * @param config Configuración del servidor
     * @throws ValidationException si algún campo no es válido
     */
    public static void validateSubirChunk(DTOSubirArchivoChunk dto, ServerConfigProperties config) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("Los datos del chunk son requeridos", "dto");
        }
        
        // Validar sessionId
        validateSessionId(dto.getSessionId());
        
        // Validar usuarioId
        if (dto.getUsuarioId() == null || dto.getUsuarioId().trim().isEmpty()) {
            throw new ValidationException("El ID del usuario es requerido", "usuarioId");
        }
        
        // Validar numeroChunk
        if (dto.getNumeroChunk() < 0) {
            throw new ValidationException("El número de chunk no puede ser negativo", "numeroChunk");
        }
        
        // Validar totalChunks
        if (dto.getTotalChunks() <= 0) {
            throw new ValidationException("El número total de chunks debe ser mayor a 0", "totalChunks");
        }
        
        // Validar que numeroChunk esté dentro del rango válido
        if (dto.getNumeroChunk() >= dto.getTotalChunks()) {
            throw new ValidationException(
                String.format("Número de chunk fuera de rango. Máximo: %d, recibido: %d", 
                    dto.getTotalChunks() - 1, dto.getNumeroChunk()), 
                "numeroChunk"
            );
        }
        
        // Validar base64ChunkData
        if (dto.getBase64ChunkData() == null || dto.getBase64ChunkData().trim().isEmpty()) {
            throw new ValidationException("Los datos del chunk son requeridos", "base64ChunkData");
        }
        
        // Validar hashChunk
        if (dto.getHashChunk() == null || dto.getHashChunk().trim().isEmpty()) {
            throw new ValidationException("El hash del chunk es requerido", "hashChunk");
        }
        
        // Validar tamaño del chunk (debe ser <= chunkSize, excepto el último)
        int chunkSize = config.getArchivos().getChunkSize();
        int expectedChunkSize = (dto.getNumeroChunk() == dto.getTotalChunks() - 1) ? 
            (int) (dto.getTamanoTotal() % chunkSize) : chunkSize;
        
        if (expectedChunkSize == 0) {
            expectedChunkSize = chunkSize; // Si es exactamente divisible, el último chunk tiene el tamaño completo
        }
        
        // Decodificar y verificar tamaño
        try {
            byte[] chunkData = java.util.Base64.getDecoder().decode(dto.getBase64ChunkData());
            if (chunkData.length != expectedChunkSize) {
                throw new ValidationException(
                    String.format("Tamaño de chunk incorrecto. Esperado: %d bytes, recibido: %d bytes", 
                        expectedChunkSize, chunkData.length), 
                    "base64ChunkData"
                );
            }
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Los datos del chunk no están en formato Base64 válido", "base64ChunkData");
        }
    }
    
    /**
     * Valida los datos para descargar un chunk.
     *
     * @param dto DTO con los datos de descarga
     * @throws ValidationException si algún campo no es válido
     */
    public static void validateDescargarChunk(DTODescargarArchivoChunk dto) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("Los datos de descarga son requeridos", "dto");
        }
        
        // Validar usuarioId
        if (dto.getUsuarioId() == null || dto.getUsuarioId().trim().isEmpty()) {
            throw new ValidationException("El ID del usuario es requerido", "usuarioId");
        }
        
        // Validar archivoId
        if (dto.getArchivoId() == null || dto.getArchivoId().trim().isEmpty()) {
            throw new ValidationException("El ID del archivo es requerido", "archivoId");
        }
        
        // Validar numeroChunk
        if (dto.getNumeroChunk() < 0) {
            throw new ValidationException("El número de chunk no puede ser negativo", "numeroChunk");
        }
    }
    
    /**
     * Valida el formato de un sessionId.
     *
     * @param sessionId ID de la sesión a validar
     * @throws ValidationException si el formato no es válido
     */
    public static void validateSessionId(String sessionId) throws ValidationException {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new ValidationException("El ID de la sesión es requerido", "sessionId");
        }
        
        try {
            UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("El ID de la sesión debe ser un UUID válido", "sessionId");
        }
    }
    
    /**
     * Valida que un número de chunk esté dentro del rango válido.
     *
     * @param numeroChunk Número del chunk
     * @param totalChunks Total de chunks
     * @throws ValidationException si está fuera de rango
     */
    public static void validateChunkNumber(int numeroChunk, int totalChunks) throws ValidationException {
        if (numeroChunk < 0) {
            throw new ValidationException("El número de chunk no puede ser negativo", "numeroChunk");
        }
        
        if (numeroChunk >= totalChunks) {
            throw new ValidationException(
                String.format("Número de chunk fuera de rango. Máximo: %d, recibido: %d", 
                    totalChunks - 1, numeroChunk), 
                "numeroChunk"
            );
        }
    }
}
