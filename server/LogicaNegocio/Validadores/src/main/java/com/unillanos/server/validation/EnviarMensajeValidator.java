package com.unillanos.server.validation;

import com.unillanos.server.dto.DTOEnviarMensaje;
import com.unillanos.server.exception.ValidationException;

/**
 * Validador compuesto para el envío de mensajes.
 * Valida todos los campos del DTOEnviarMensaje usando los validadores específicos.
 */
public class EnviarMensajeValidator {
    
    /**
     * Valida todos los datos de envío de un mensaje.
     *
     * @param dto DTO con los datos del mensaje a enviar
     * @throws ValidationException si algún campo no es válido
     */
    public static void validate(DTOEnviarMensaje dto) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("Los datos del mensaje son requeridos", "dto");
        }
        
        // Validar remitenteId
        if (dto.getRemitenteId() == null || dto.getRemitenteId().trim().isEmpty()) {
            throw new ValidationException("El ID del remitente es requerido", "remitenteId");
        }
        
        // Validar que haya EXACTAMENTE un destinatario (directo XOR canal)
        boolean tieneDestinatario = dto.getDestinatarioId() != null && !dto.getDestinatarioId().trim().isEmpty();
        boolean tieneCanal = dto.getCanalId() != null && !dto.getCanalId().trim().isEmpty();
        
        if (!tieneDestinatario && !tieneCanal) {
            throw new ValidationException(
                "Debe especificar un destinatario o un canal", 
                "destinatario"
            );
        }
        
        if (tieneDestinatario && tieneCanal) {
            throw new ValidationException(
                "No puede enviar a un destinatario y a un canal simultáneamente", 
                "destinatario"
            );
        }
        
        // Validar contenido
        ContenidoMensajeValidator.validate(dto.getContenido());
        
        // fileId es opcional, no se valida
    }
}

