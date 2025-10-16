package com.unillanos.server.dto.response;

import java.time.LocalDateTime;

/**
 * DTO para mensajes privados en respuestas.
 * Estructura específica que espera el cliente.
 */
public class DTOMensajePrivadoResponse {
    private String id;
    private String remitenteId;
    private String destinatarioId;
    private String nombreRemitente;
    private String nombreDestinatario;
    private String contenido;
    private String tipo;
    private String fileId;
    private LocalDateTime timestamp;
    private String nombreArchivo; // Para archivos

    public DTOMensajePrivadoResponse() {}

    public DTOMensajePrivadoResponse(String id, String remitenteId, String destinatarioId,
                                   String nombreRemitente, String nombreDestinatario,
                                   String contenido, String tipo, String fileId, 
                                   LocalDateTime timestamp, String nombreArchivo) {
        this.id = id;
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.nombreRemitente = nombreRemitente;
        this.nombreDestinatario = nombreDestinatario;
        this.contenido = contenido;
        this.tipo = tipo;
        this.fileId = fileId;
        this.timestamp = timestamp;
        this.nombreArchivo = nombreArchivo;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getNombreRemitente() {
        return nombreRemitente;
    }

    public void setNombreRemitente(String nombreRemitente) {
        this.nombreRemitente = nombreRemitente;
    }

    public String getNombreDestinatario() {
        return nombreDestinatario;
    }

    public void setNombreDestinatario(String nombreDestinatario) {
        this.nombreDestinatario = nombreDestinatario;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }
}
