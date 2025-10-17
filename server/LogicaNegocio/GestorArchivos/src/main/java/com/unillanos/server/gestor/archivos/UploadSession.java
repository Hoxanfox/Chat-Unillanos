package com.unillanos.server.gestor.archivos;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa una sesión de subida de archivo por chunks.
 * Almacena el estado y los chunks recibidos durante la transferencia.
 *
 * @author Chat-Unillanos Team
 * @version 1.0.0
 */
public class UploadSession {

    private final String uploadId;
    private final String usuarioId;
    private final String nombreArchivo;
    private final String tipoMime;
    private final int totalChunks;
    private final LocalDateTime inicioSesion;
    private final Map<Integer, byte[]> chunks;
    private LocalDateTime ultimaActividad;

    public UploadSession(String uploadId, String usuarioId, String nombreArchivo,
                        String tipoMime, int totalChunks) {
        this.uploadId = uploadId;
        this.usuarioId = usuarioId;
        this.nombreArchivo = nombreArchivo;
        this.tipoMime = tipoMime;
        this.totalChunks = totalChunks;
        this.inicioSesion = LocalDateTime.now();
        this.ultimaActividad = LocalDateTime.now();
        this.chunks = new ConcurrentHashMap<>();
    }

    /**
     * Agrega un chunk a la sesión
     */
    public void agregarChunk(int numeroChunk, byte[] data) {
        chunks.put(numeroChunk, data);
        ultimaActividad = LocalDateTime.now();
    }

    /**
     * Verifica si todos los chunks han sido recibidos
     */
    public boolean estaCompleta() {
        if (chunks.size() != totalChunks) {
            return false;
        }

        // Verificar que todos los números de chunk estén presentes
        for (int i = 1; i <= totalChunks; i++) {
            if (!chunks.containsKey(i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Ensambla todos los chunks en un solo array de bytes
     */
    public byte[] ensamblarArchivo() {
        if (!estaCompleta()) {
            throw new IllegalStateException("No se puede ensamblar: faltan chunks");
        }

        // Calcular tamaño total
        int tamanoTotal = chunks.values().stream()
            .mapToInt(chunk -> chunk.length)
            .sum();

        byte[] archivoCompleto = new byte[tamanoTotal];
        int offset = 0;

        // Ensamblar en orden
        for (int i = 1; i <= totalChunks; i++) {
            byte[] chunk = chunks.get(i);
            System.arraycopy(chunk, 0, archivoCompleto, offset, chunk.length);
            offset += chunk.length;
        }

        return archivoCompleto;
    }

    /**
     * Calcula el progreso de la subida en porcentaje
     */
    public int calcularProgreso() {
        return (chunks.size() * 100) / totalChunks;
    }

    /**
     * Verifica si la sesión ha expirado (más de 30 minutos sin actividad)
     */
    public boolean haExpirado() {
        return LocalDateTime.now().isAfter(ultimaActividad.plusMinutes(30));
    }

    // Getters
    public String getUploadId() {
        return uploadId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getTipoMime() {
        return tipoMime;
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

    public int getChunksRecibidos() {
        return chunks.size();
    }

    @Override
    public String toString() {
        return "UploadSession{" +
                "uploadId='" + uploadId + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                ", nombreArchivo='" + nombreArchivo + '\'' +
                ", progreso=" + calcularProgreso() + "%" +
                ", chunks=" + chunks.size() + "/" + totalChunks +
                '}';
    }
}

