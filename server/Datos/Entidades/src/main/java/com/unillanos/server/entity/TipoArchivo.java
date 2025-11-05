package com.unillanos.server.entity;

/**
 * Enum que representa el tipo de archivo multimedia.
 */
public enum TipoArchivo {
    IMAGE,      // Imágenes (JPG, PNG, GIF, WEBP, BMP)
    AUDIO,      // Audios (MP3, WAV, OGG, M4A, AAC)
    DOCUMENT;   // Documentos (PDF, DOCX, TXT, XLSX, PPTX, ZIP)

    /**
     * Convierte un String a TipoArchivo.
     * 
     * @param tipo String a convertir
     * @return TipoArchivo correspondiente, o DOCUMENT por defecto
     */
    public static TipoArchivo fromString(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return DOCUMENT; // Por defecto
        }
        
        try {
            return TipoArchivo.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DOCUMENT;
        }
    }
}
