package com.unillanos.server.validation;

import com.unillanos.server.config.ServerConfigProperties;
import com.unillanos.server.exception.ValidationException;

/**
 * Validador de tamaño de archivos multimedia.
 * Aplica límites de tamaño según el tipo de archivo.
 */
public class TamanoArchivoValidator {
    
    /**
     * Valida el tamaño del archivo según su tipo lógico (IMAGE, AUDIO, DOCUMENT).
     */
    public static void validate(long tamanoBytes, String tipoLogico, ServerConfigProperties config) throws ValidationException {
        if (tamanoBytes <= 0) {
            throw new ValidationException("El tamaño del archivo debe ser mayor a 0", "tamanoBytes");
        }
        if (tipoLogico == null) {
            throw new ValidationException("Tipo de archivo no reconocido", "tipoArchivo");
        }
        long maxSize;
        switch (tipoLogico) {
            case "IMAGE" -> maxSize = config.getArchivos().getMaxTamanoImagen();
            case "AUDIO" -> maxSize = config.getArchivos().getMaxTamanoAudio();
            case "DOCUMENT" -> maxSize = config.getArchivos().getMaxTamanoDocumento();
            default -> throw new ValidationException("Tipo de archivo no reconocido", "tipoArchivo");
        }
        if (tamanoBytes > maxSize) {
            throw new ValidationException(
                String.format("El archivo excede el tamaño máximo permitido (%d MB)", maxSize / (1024 * 1024)),
                "tamanoBytes"
            );
        }
    }
}
