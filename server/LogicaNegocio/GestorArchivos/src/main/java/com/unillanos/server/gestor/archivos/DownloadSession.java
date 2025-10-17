package com.unillanos.server.gestor.archivos;

import java.time.LocalDateTime;

/**
 * Representa una sesión de descarga de archivo por chunks.
 * Mantiene el estado de la descarga en progreso.
 * 
 * @author Chat-Unillanos Team
 * @version 1.0.0
 */
public class DownloadSession {
    
    private final String downloadId;
    private final String usuarioId;
    private final String archivoId;
    private final String nombreArchivo;
    private final String tipoMime;
    private final long tamanoBytes;
    private final int totalChunks;
    private final byte[] contenidoArchivo;
    private final LocalDateTime inicioSesion;
    private LocalDateTime ultimaActividad;
    private int ultimoChunkEnviado;
    
    public DownloadSession(String downloadId, String usuarioId, String archivoId,
                          String nombreArchivo, String tipoMime, long tamanoBytes,
                          int totalChunks, byte[] contenidoArchivo) {
        this.downloadId = downloadId;
        this.usuarioId = usuarioId;
        this.archivoId = archivoId;
        this.nombreArchivo = nombreArchivo;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
        this.totalChunks = totalChunks;
        this.contenidoArchivo = contenidoArchivo;
        this.inicioSesion = LocalDateTime.now();
        this.ultimaActividad = LocalDateTime.now();
        this.ultimoChunkEnviado = 0;
    }
    
    /**
     * Obtiene un chunk específico del archivo
     */
    public byte[] obtenerChunk(int numeroChunk, int chunkSize) {
        if (numeroChunk < 1 || numeroChunk > totalChunks) {
            throw new IllegalArgumentException("Número de chunk inválido: " + numeroChunk);
        }
        
        int offset = (numeroChunk - 1) * chunkSize;
        int length = Math.min(chunkSize, contenidoArchivo.length - offset);
        
        byte[] chunk = new byte[length];
        System.arraycopy(contenidoArchivo, offset, chunk, 0, length);
        
        ultimoChunkEnviado = numeroChunk;
        ultimaActividad = LocalDateTime.now();
        
        return chunk;
    }
    
    /**
     * Verifica si la descarga está completa
     */
    public boolean estaCompleta() {
        return ultimoChunkEnviado >= totalChunks;
    }
    
    /**
     * Calcula el progreso de la descarga en porcentaje
     */
    public int calcularProgreso() {
        return (ultimoChunkEnviado * 100) / totalChunks;
    }
    
    /**
     * Verifica si la sesión ha expirado (más de 30 minutos sin actividad)
     */
    public boolean haExpirado() {
        return LocalDateTime.now().isAfter(ultimaActividad.plusMinutes(30));
    }
    
    // Getters
    public String getDownloadId() {
        return downloadId;
    }
    
    public String getUsuarioId() {
        return usuarioId;
    }
    
    public String getArchivoId() {
        return archivoId;
    }
    
    public String getNombreArchivo() {
        return nombreArchivo;
    }
    
    public String getTipoMime() {
        return tipoMime;
    }
    
    public long getTamanoBytes() {
        return tamanoBytes;
    }
    
    public int getTotalChunks() {
        return totalChunks;
    }
    
    public LocalDateTime getInicioSesion() {
        return inicioSesion;
    }
    
    public LocalDateTime getUltimaActividad() {
        return ultimaActividad;
    }
    
    public int getUltimoChunkEnviado() {
        return ultimoChunkEnviado;
    }
    
    @Override
    public String toString() {
        return "DownloadSession{" +
                "downloadId='" + downloadId + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                ", archivoId='" + archivoId + '\'' +
                ", nombreArchivo='" + nombreArchivo + '\'' +
                ", progreso=" + calcularProgreso() + "%" +
                ", chunks=" + ultimoChunkEnviado + "/" + totalChunks +
                '}';
    }
}

