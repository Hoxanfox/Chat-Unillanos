package com.unillanos.server.dto;

/**
 * DTO para enviar un mensaje (directo o de canal).
 */
public class DTOEnviarMensaje {
    
    private String remitenteId;         // Requerido - Usuario que envía
    private String destinatarioId;      // Opcional - Si es mensaje directo
    private String canalId;             // Opcional - Si es mensaje de canal
    private String contenido;           // Requerido, 1-2000 caracteres
    private String fileId;              // Opcional - ID del archivo adjunto (para Épica 5)

    public DTOEnviarMensaje() {
    }

    public DTOEnviarMensaje(String remitenteId, String destinatarioId, String canalId, 
                            String contenido, String fileId) {
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.canalId = canalId;
        this.contenido = contenido;
        this.fileId = fileId;
    }

    // Getters y Setters
    public String getRemitenteId() {
        return remitenteId;
    }

    public void setRemitenteId(String remitenteId) {
        this.remitenteId = remitenteId;
    }

    public String getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(String destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    @Override
    public String toString() {
        return "DTOEnviarMensaje{" +
                "remitenteId='" + remitenteId + '\'' +
                ", destinatarioId='" + destinatarioId + '\'' +
                ", canalId='" + canalId + '\'' +
                ", contenido='" + (contenido != null && contenido.length() > 50 ? 
                    contenido.substring(0, 50) + "..." : contenido) + '\'' +
                ", fileId='" + fileId + '\'' +
                '}';
    }
}

