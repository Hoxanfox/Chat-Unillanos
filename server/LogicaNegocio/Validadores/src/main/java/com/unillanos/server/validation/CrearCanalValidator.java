package com.unillanos.server.validation;

import com.unillanos.server.dto.DTOCrearCanal;
import com.unillanos.server.exception.ValidationException;

/**
 * Validador compuesto para la creación de canales.
 * Valida todos los campos del DTOCrearCanal usando los validadores específicos.
 */
public class CrearCanalValidator {
    
    /**
     * Valida todos los datos de creación de un canal.
     *
     * @param dto DTO con los datos de creación del canal
     * @throws ValidationException si algún campo no es válido
     */
    public static void validate(DTOCrearCanal dto) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("Los datos del canal son requeridos", "dto");
        }
        
        // Validar creadorId
        if (dto.getCreadorId() == null || dto.getCreadorId().trim().isEmpty()) {
            throw new ValidationException("El ID del creador es requerido", "creadorId");
        }
        
        // Validar nombre
        NombreCanalValidator.validate(dto.getNombre());
        
        // Validar descripción (opcional)
        DescripcionCanalValidator.validate(dto.getDescripcion());
    }
}

