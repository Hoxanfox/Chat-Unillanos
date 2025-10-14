package com.unillanos.server.dto;

/**
 * DTO de respuesta con información de un mensaje.
 */
public class DTOMensaje {
    
    private Long id;                    // ID del mensaje
    private String remitenteId;
    private String remitenteNombre;
    private String destinatarioId;      // null si es mensaje de canal
    private String destinatarioNombre;  // null si es mensaje de canal
    private String canalId;             // null si es mensaje directo
    private String canalNombre;         // null si es mensaje directo
    private String tipo;                // "DIRECT" o "CHANNEL"
    private String contenido;
    private String fileId;              // null si no tiene archivo
    private String fileName;            // null si no tiene archivo
    private String fechaEnvio;          // ISO-8601

    public DTOMensaje() {
    }

    public DTOMensaje(Long id, String remitenteId, String remitenteNombre, 
                      String destinatarioId, String destinatarioNombre,
                      String canalId, String canalNombre, String tipo, 
                      String contenido, String fileId, String fileName, String fechaEnvio) {
        this.id = id;
        this.remitenteId = remitenteId;
        this.remitenteNombre = remitenteNombre;
        this.destinatarioId = destinatarioId;
        this.destinatarioNombre = destinatarioNombre;
        this.canalId = canalId;
        this.canalNombre = canalNombre;
        this.tipo = tipo;
        this.contenido = contenido;
        this.fileId = fileId;
        this.fileName = fileName;
        this.fechaEnvio = fechaEnvio;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRemitenteId() {
        return remitenteId;
    }

    public void setRemitenteId(String remitenteId) {
        this.remitenteId = remitenteId;
    }

    public String getRemitenteNombre() {
        return remitenteNombre;
    }

    public void setRemitenteNombre(String remitenteNombre) {
        this.remitenteNombre = remitenteNombre;
    }

    public String getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(String destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public String getDestinatarioNombre() {
        return destinatarioNombre;
    }

    public void setDestinatarioNombre(String destinatarioNombre) {
        this.destinatarioNombre = destinatarioNombre;
    }

    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    public String getCanalNombre() {
        return canalNombre;
    }

    public void setCanalNombre(String canalNombre) {
        this.canalNombre = canalNombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(String fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    @Override
    public String toString() {
        return "DTOMensaje{" +
                "id=" + id +
                ", remitenteNombre='" + remitenteNombre + '\'' +
                ", destinatarioNombre='" + destinatarioNombre + '\'' +
                ", canalNombre='" + canalNombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", contenido='" + (contenido != null && contenido.length() > 50 ? 
                    contenido.substring(0, 50) + "..." : contenido) + '\'' +
                ", fechaEnvio='" + fechaEnvio + '\'' +
                '}';
    }
}

