package com.unillanos.server.dto.response;

import java.time.LocalDateTime;

/**
 * DTO para mensajes de canal en respuestas.
 * Estructura específica que espera el cliente.
 */
public class DTOMensajeCanalResponse {
    private String id;
    private String canalId;
    private String usuarioId;
    private String nombreUsuario;
    private String contenido;
    private String tipo;
    private String fileId;
    private LocalDateTime timestamp;
    private String nombreArchivo; // Para archivos

    public DTOMensajeCanalResponse() {}

    public DTOMensajeCanalResponse(String id, String canalId, String usuarioId, String nombreUsuario,
                                 String contenido, String tipo, String fileId, LocalDateTime timestamp,
                                 String nombreArchivo) {
        this.id = id;
        this.canalId = canalId;
        this.usuarioId = usuarioId;
        this.nombreUsuario = nombreUsuario;
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

    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
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
