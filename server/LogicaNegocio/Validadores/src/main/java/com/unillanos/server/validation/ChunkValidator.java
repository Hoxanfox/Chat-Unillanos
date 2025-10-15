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
     * Valida los datos para iniciar una subida de archivo durante el registro.
     * Similar a validateIniciarSubida pero sin validar que el usuario existe.
     *
     * @param dto DTO con los datos de inicio de subida
     * @param config Configuración del servidor
     * @throws ValidationException si algún campo no es válido
     */
    public static void validateIniciarSubidaParaRegistro(DTOIniciarSubida dto, ServerConfigProperties config) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("Los datos de inicio de subida son requeridos", "dto");
        }
        
        // Validar usuarioId (pero no verificar que existe en BD)
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
        if (dto.getNumeroChunk() < 1) {
            throw new ValidationException("El número de chunk debe ser mayor a 0", "numeroChunk");
        }
        
        // Validar totalChunks
        if (dto.getTotalChunks() <= 0) {
            throw new ValidationException("El número total de chunks debe ser mayor a 0", "totalChunks");
        }
        
        // Validar que numeroChunk esté dentro del rango válido
        // El cliente usa numeración basada en 1 (1, 2, 3...) no basada en 0 (0, 1, 2...)
        if (dto.getNumeroChunk() > dto.getTotalChunks()) {
            throw new ValidationException(
                String.format("Número de chunk fuera de rango. Rango válido: 1-%d, recibido: %d", 
                    dto.getTotalChunks(), dto.getNumeroChunk()), 
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
        
        // Validar tamaño del chunk (para registro, solo verificamos que no exceda el chunk size)
        int chunkSize = config.getArchivos().getChunkSize();
        
        // Decodificar y verificar que el chunk no exceda el tamaño máximo permitido
        try {
            byte[] chunkData = java.util.Base64.getDecoder().decode(dto.getBase64ChunkData());
            
            // Para archivos de registro, solo verificamos que el chunk no sea más grande que el chunk size
            // y que no esté vacío
            if (chunkData.length == 0) {
                throw new ValidationException("El chunk no puede estar vacío", "base64ChunkData");
            }
            
            if (chunkData.length > chunkSize) {
                throw new ValidationException(
                    String.format("El chunk excede el tamaño máximo permitido. Máximo: %d bytes, recibido: %d bytes", 
                        chunkSize, chunkData.length), 
                    "base64ChunkData"
                );
            }
            
            // Para archivos de un solo chunk, verificamos que el tamaño sea razonable
            if (dto.getTotalChunks() == 1 && dto.getTamanoTotal() > chunkSize) {
                // Si el archivo reporta ser más grande que el chunk size pero solo tiene un chunk,
                // hay una inconsistencia en los datos
                throw new ValidationException(
                    String.format("Inconsistencia en los datos: archivo de %d bytes reportado como un solo chunk", 
                        dto.getTamanoTotal()), 
                    "base64ChunkData"
                );
            }
            
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Los datos del chunk no están en formato Base64 válido", "base64ChunkData");
        }
    }

    /**
     * Valida un chunk individual a subir durante el proceso de registro.
     * Similar a validateSubirChunk pero sin validar usuarioId ni hash.
     *
     * @param dto DTO con los datos del chunk
     * @param config Configuración del servidor
     * @throws ValidationException si algún campo no es válido
     */
    public static void validateSubirChunkParaRegistro(DTOSubirArchivoChunk dto, ServerConfigProperties config) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("Los datos del chunk son requeridos", "dto");
        }
        
        // Validar sessionId
        validateSessionId(dto.getSessionId());
        
        // NO validar usuarioId para registro (se usa usuario temporal)
        
        // Validar numeroChunk
        if (dto.getNumeroChunk() < 1) {
            throw new ValidationException("El número de chunk debe ser mayor a 0", "numeroChunk");
        }
        
        // Validar totalChunks
        if (dto.getTotalChunks() <= 0) {
            throw new ValidationException("El número total de chunks debe ser mayor a 0", "totalChunks");
        }
        
        // Validar que numeroChunk esté dentro del rango válido
        // El cliente usa numeración basada en 1 (1, 2, 3...) no basada en 0 (0, 1, 2...)
        if (dto.getNumeroChunk() > dto.getTotalChunks()) {
            throw new ValidationException(
                String.format("Número de chunk fuera de rango. Rango válido: 1-%d, recibido: %d", 
                    dto.getTotalChunks(), dto.getNumeroChunk()), 
                "numeroChunk"
            );
        }
        
        // Validar base64ChunkData
        if (dto.getBase64ChunkData() == null || dto.getBase64ChunkData().trim().isEmpty()) {
            throw new ValidationException("Los datos del chunk son requeridos", "base64ChunkData");
        }
        
        // NO validar hashChunk para registro (se omite por simplicidad)
        
        // Validar tamaño del chunk (para registro, solo verificamos que no exceda el chunk size)
        int chunkSize = config.getArchivos().getChunkSize();
        
        // Decodificar y verificar que el chunk no exceda el tamaño máximo permitido
        try {
            byte[] chunkData = java.util.Base64.getDecoder().decode(dto.getBase64ChunkData());
            
            // Para archivos de registro, solo verificamos que el chunk no sea más grande que el chunk size
            // y que no esté vacío
            if (chunkData.length == 0) {
                throw new ValidationException("El chunk no puede estar vacío", "base64ChunkData");
            }
            
            if (chunkData.length > chunkSize) {
                throw new ValidationException(
                    String.format("El chunk excede el tamaño máximo permitido. Máximo: %d bytes, recibido: %d bytes", 
                        chunkSize, chunkData.length), 
                    "base64ChunkData"
                );
            }
            
            // Para archivos de un solo chunk, verificamos que el tamaño sea razonable
            if (dto.getTotalChunks() == 1 && dto.getTamanoTotal() > chunkSize) {
                // Si el archivo reporta ser más grande que el chunk size pero solo tiene un chunk,
                // hay una inconsistencia en los datos
                throw new ValidationException(
                    String.format("Inconsistencia en los datos: archivo de %d bytes reportado como un solo chunk", 
                        dto.getTamanoTotal()), 
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
        if (numeroChunk < 1) {
            throw new ValidationException("El número de chunk debe ser mayor a 0", "numeroChunk");
        }
        
        if (numeroChunk > totalChunks) {
            throw new ValidationException(
                String.format("Número de chunk fuera de rango. Rango válido: 1-%d, recibido: %d", 
                    totalChunks, numeroChunk), 
                "numeroChunk"
            );
        }
    }
}
