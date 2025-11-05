package dominio;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de dominio que representa un archivo almacenado localmente.
 */
public class Archivo {
    private UUID idArchivo;
    private String fileIdServidor;  // ID que asigna el servidor
    private String nombreArchivo;
    private String mimeType;
    private long tamanioBytes;
    private String contenidoBase64;
    private String hashSHA256;
    private LocalDateTime fechaDescarga;
    private LocalDateTime fechaUltimaActualizacion;
    private String asociadoA;  // 'perfil', 'mensaje', 'canal', etc.
    private UUID idAsociado;
    private String estado;  // 'descargando', 'completo', 'error'

    public Archivo() {
        this.idArchivo = UUID.randomUUID();
        this.fechaDescarga = LocalDateTime.now();
        this.fechaUltimaActualizacion = LocalDateTime.now();
        this.estado = "descargando";
    }

    public Archivo(String fileIdServidor, String nombreArchivo, String mimeType, long tamanioBytes) {
        this();
        this.fileIdServidor = fileIdServidor;
        this.nombreArchivo = nombreArchivo;
        this.mimeType = mimeType;
        this.tamanioBytes = tamanioBytes;
    }

    // Getters y Setters
    public UUID getIdArchivo() {
        return idArchivo;
    }

    public void setIdArchivo(UUID idArchivo) {
        this.idArchivo = idArchivo;
    }

    public String getFileIdServidor() {
        return fileIdServidor;
    }

    public void setFileIdServidor(String fileIdServidor) {
        this.fileIdServidor = fileIdServidor;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getTamanioBytes() {
        return tamanioBytes;
    }

    public void setTamanioBytes(long tamanioBytes) {
        this.tamanioBytes = tamanioBytes;
    }

    public String getContenidoBase64() {
        return contenidoBase64;
    }

    public void setContenidoBase64(String contenidoBase64) {
        this.contenidoBase64 = contenidoBase64;
    }

    public String getHashSHA256() {
        return hashSHA256;
    }

    public void setHashSHA256(String hashSHA256) {
        this.hashSHA256 = hashSHA256;
    }

    public LocalDateTime getFechaDescarga() {
        return fechaDescarga;
    }

    public void setFechaDescarga(LocalDateTime fechaDescarga) {
        this.fechaDescarga = fechaDescarga;
    }

    public LocalDateTime getFechaUltimaActualizacion() {
        return fechaUltimaActualizacion;
    }

    public void setFechaUltimaActualizacion(LocalDateTime fechaUltimaActualizacion) {
        this.fechaUltimaActualizacion = fechaUltimaActualizacion;
    }

    public String getAsociadoA() {
        return asociadoA;
    }

    public void setAsociadoA(String asociadoA) {
        this.asociadoA = asociadoA;
    }

    public UUID getIdAsociado() {
        return idAsociado;
    }

    public void setIdAsociado(UUID idAsociado) {
        this.idAsociado = idAsociado;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Archivo{" +
                "fileIdServidor='" + fileIdServidor + '\'' +
                ", nombreArchivo='" + nombreArchivo + '\'' +
                ", tamanioBytes=" + tamanioBytes +
                ", estado='" + estado + '\'' +
                '}';
    }
}

