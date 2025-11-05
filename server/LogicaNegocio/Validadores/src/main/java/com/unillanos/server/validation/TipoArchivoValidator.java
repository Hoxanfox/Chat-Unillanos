package com.unillanos.server.validation;

import com.unillanos.server.exception.ValidationException;

import java.util.Set;

/**
 * Validador de tipos de archivo multimedia.
 * Valida extensiones y tipos MIME permitidos.
 */
public class TipoArchivoValidator {
    
    // Extensiones permitidas
    private static final Set<String> EXTENSIONES_IMAGEN = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> EXTENSIONES_AUDIO = Set.of("mp3", "wav", "ogg", "m4a", "aac");
    private static final Set<String> EXTENSIONES_DOCUMENTO = Set.of("pdf", "docx", "txt", "xlsx", "pptx", "zip");
    
    // MIME types permitidos
    private static final Set<String> MIME_IMAGEN = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );
    private static final Set<String> MIME_AUDIO = Set.of(
        "audio/mpeg", "audio/wav", "audio/ogg", "audio/mp4", "audio/aac"
    );
    private static final Set<String> MIME_DOCUMENTO = Set.of(
        "application/pdf", 
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain",
        "application/zip"
    );
    
    /**
     * Valida la extensión del archivo.
     */
    public static void validateExtension(String nombreArchivo) throws ValidationException {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            throw new ValidationException("El archivo debe tener una extensión válida", "nombreArchivo");
        }
        String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf(".") + 1).toLowerCase();
        if (!EXTENSIONES_IMAGEN.contains(extension) && 
            !EXTENSIONES_AUDIO.contains(extension) && 
            !EXTENSIONES_DOCUMENTO.contains(extension)) {
            throw new ValidationException("Extensión de archivo no permitida: " + extension, "nombreArchivo");
        }
    }
    
    /**
     * Valida el tipo MIME del archivo.
     */
    public static void validateMimeType(String tipoMime) throws ValidationException {
        if (tipoMime == null || tipoMime.trim().isEmpty()) {
            throw new ValidationException("El tipo MIME es requerido", "tipoMime");
        }
        if (!MIME_IMAGEN.contains(tipoMime) && 
            !MIME_AUDIO.contains(tipoMime) && 
            !MIME_DOCUMENTO.contains(tipoMime)) {
            throw new ValidationException("Tipo MIME no permitido: " + tipoMime, "tipoMime");
        }
    }
    
    /**
     * Determina el tipo de archivo lógico (IMAGE, AUDIO, DOCUMENT) según el MIME type.
     * Retorna el tipo como String para evitar dependencias con otras capas.
     */
    public static String detectarTipo(String tipoMime) {
        if (MIME_IMAGEN.contains(tipoMime)) {
            return "IMAGE";
        } else if (MIME_AUDIO.contains(tipoMime)) {
            return "AUDIO";
        } else if (MIME_DOCUMENTO.contains(tipoMime)) {
            return "DOCUMENT";
        }
        return null;
    }
}
