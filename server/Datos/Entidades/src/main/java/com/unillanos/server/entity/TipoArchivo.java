package com.unillanos.server.entity;

/**
 * Enum que representa el tipo de archivo multimedia.
 */
public enum TipoArchivo {
    IMAGEN,      // Imágenes (JPG, PNG, GIF, WEBP, BMP)
    AUDIO,       // Audios (MP3, WAV, OGG, M4A, AAC)
    DOCUMENTO;   // Documentos (PDF, DOCX, TXT, XLSX, PPTX, ZIP)
    
    /**
     * Convierte un String a TipoArchivo.
     * 
     * @param tipo String a convertir
     * @return TipoArchivo correspondiente, o DOCUMENTO por defecto
     */
    public static TipoArchivo fromString(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return DOCUMENTO; // Por defecto
        }
        
        try {
            return TipoArchivo.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DOCUMENTO;
        }
    }
}
