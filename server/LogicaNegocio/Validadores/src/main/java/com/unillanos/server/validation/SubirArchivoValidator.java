package com.unillanos.server.validation;

import com.unillanos.server.dto.DTOSubirArchivo;
import com.unillanos.server.exception.ValidationException;

/**
 * Validador compuesto para subir archivos.
 * Valida todos los aspectos de un archivo a subir.
 */
public class SubirArchivoValidator {
    
    /**
     * Valida todos los datos de subida de un archivo.
     *
     * @param dto DTO con los datos del archivo a subir
     * @throws ValidationException si algún campo no es válido
     */
    public static void validate(DTOSubirArchivo dto) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("Los datos del archivo son requeridos", "dto");
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
        
        // Validar tamaño con tipo lógico (String)
        String tipoLogico = TipoArchivoValidator.detectarTipo(dto.getTipoMime());
        TamanoArchivoValidator.validate(dto.getTamanoBytes(), tipoLogico);
        
        // Validar base64Data
        if (dto.getBase64Data() == null || dto.getBase64Data().trim().isEmpty()) {
            throw new ValidationException("El contenido del archivo es requerido", "base64Data");
        }
    }
}

