package com.unillanos.server.entity;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Entidad que representa una sesión de subida de archivo por chunks.
 * Gestiona el estado de la subida y los chunks recibidos.
 */
public class ChunkSessionEntity {
    
    private String sessionId;
    private String usuarioId;
    private String nombreArchivo;
    private String tipoMime;
    private long tamanoTotal;
    private int totalChunks;
    private Set<Integer> chunksRecibidos;
    private LocalDateTime fechaInicio;
    private LocalDateTime ultimaActividad;
    private EstadoSesion estadoSesion;

    // Constructor por defecto
    public ChunkSessionEntity() {}

    // Constructor con parámetros
    public ChunkSessionEntity(String sessionId, String usuarioId, String nombreArchivo, 
                             String tipoMime, long tamanoTotal, int totalChunks, 
                             Set<Integer> chunksRecibidos, LocalDateTime fechaInicio, 
                             LocalDateTime ultimaActividad, EstadoSesion estadoSesion) {
        this.sessionId = sessionId;
        this.usuarioId = usuarioId;
        this.nombreArchivo = nombreArchivo;
        this.tipoMime = tipoMime;
        this.tamanoTotal = tamanoTotal;
        this.totalChunks = totalChunks;
        this.chunksRecibidos = chunksRecibidos;
        this.fechaInicio = fechaInicio;
        this.ultimaActividad = ultimaActividad;
        this.estadoSesion = estadoSesion;
    }

    // Getters y Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(String tipoMime) {
        this.tipoMime = tipoMime;
    }

    public long getTamanoTotal() {
        return tamanoTotal;
    }

    public void setTamanoTotal(long tamanoTotal) {
        this.tamanoTotal = tamanoTotal;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public Set<Integer> getChunksRecibidos() {
        return chunksRecibidos;
    }

    public void setChunksRecibidos(Set<Integer> chunksRecibidos) {
        this.chunksRecibidos = chunksRecibidos;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getUltimaActividad() {
        return ultimaActividad;
    }

    public void setUltimaActividad(LocalDateTime ultimaActividad) {
        this.ultimaActividad = ultimaActividad;
    }

    public EstadoSesion getEstadoSesion() {
        return estadoSesion;
    }

    public void setEstadoSesion(EstadoSesion estadoSesion) {
        this.estadoSesion = estadoSesion;
    }

    @Override
    public String toString() {
        return "ChunkSessionEntity{" +
                "sessionId='" + sessionId + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                ", nombreArchivo='" + nombreArchivo + '\'' +
                ", tipoMime='" + tipoMime + '\'' +
                ", tamanoTotal=" + tamanoTotal +
                ", totalChunks=" + totalChunks +
                ", chunksRecibidos=" + chunksRecibidos +
                ", fechaInicio=" + fechaInicio +
                ", ultimaActividad=" + ultimaActividad +
                ", estadoSesion=" + estadoSesion +
                '}';
    }
}
