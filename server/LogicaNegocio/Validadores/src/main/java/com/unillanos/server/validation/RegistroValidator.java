package com.unillanos.server.validation;

import com.unillanos.server.config.ServerConfigProperties;
import com.unillanos.server.dto.DTORegistro;
import com.unillanos.server.exception.ValidationException;

/**
 * Validador compuesto para el registro de usuarios.
 * Valida todos los campos del DTORegistro usando los validadores específicos.
 */
public class RegistroValidator {
    
    /**
     * Valida todos los datos de registro de un usuario.
     *
     * @param dto DTO con los datos de registro
     * @param config Configuración del servidor
     * @throws ValidationException si algún campo no es válido
     */
    public static void validate(DTORegistro dto, ServerConfigProperties config) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("Los datos de registro son requeridos", "dto");
        }
        
        // Validar nombre
        NombreValidator.validate(dto.getNombre());
        
        // Validar email
        EmailValidator.validate(dto.getEmail());
        
        // Validar contraseña
        PasswordValidator.validate(dto.getPassword(), config);
        
        // photoId es opcional, no se valida si es null
    }
}

