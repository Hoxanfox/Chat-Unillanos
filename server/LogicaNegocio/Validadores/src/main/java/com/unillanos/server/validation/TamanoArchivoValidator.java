package com.unillanos.server.validation;

import com.unillanos.server.exception.ValidationException;

/**
 * Validador de tamaño de archivos multimedia.
 * Aplica límites de tamaño según el tipo de archivo.
 */
public class TamanoArchivoValidator {
    
    private static final long MAX_SIZE_IMAGEN = 10 * 1024 * 1024;     // 10 MB
    private static final long MAX_SIZE_AUDIO = 20 * 1024 * 1024;      // 20 MB
    private static final long MAX_SIZE_DOCUMENTO = 50 * 1024 * 1024;  // 50 MB
    
    /**
     * Valida el tamaño del archivo según su tipo lógico (IMAGEN, AUDIO, DOCUMENTO).
     */
    public static void validate(long tamanoBytes, String tipoLogico) throws ValidationException {
        if (tamanoBytes <= 0) {
            throw new ValidationException("El tamaño del archivo debe ser mayor a 0", "tamanoBytes");
        }
        if (tipoLogico == null) {
            throw new ValidationException("Tipo de archivo no reconocido", "tipoArchivo");
        }
        long maxSize;
        switch (tipoLogico) {
            case "IMAGEN" -> maxSize = MAX_SIZE_IMAGEN;
            case "AUDIO" -> maxSize = MAX_SIZE_AUDIO;
            case "DOCUMENTO" -> maxSize = MAX_SIZE_DOCUMENTO;
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

