package dominio.clienteServidor;

import dominio.merkletree.IMerkleEntity;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad que representa un archivo almacenado en el servidor.
 * Contiene metadatos del archivo, NO el contenido binario completo.
 * El contenido físico se almacena en Bucket/ y aquí solo guardamos la ruta relativa.
 */
public class Archivo implements Serializable, IMerkleEntity {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String fileId;           // ID único del archivo en el sistema (ej: "user_photos/1.jpg")
    private String nombreArchivo;    // Nombre original del archivo
    private String rutaRelativa;     // Ruta relativa desde Bucket/ (ej: "user_photos/1.jpg")
    private String mimeType;
    private long tamanio;            // Tamaño en bytes
    private String hashSHA256;
    private Instant fechaCreacion;
    private Instant fechaUltimaActualizacion;

    public Archivo() {
        this.id = UUID.randomUUID();
        this.fechaCreacion = Instant.now();
        this.fechaUltimaActualizacion = Instant.now();
    }

    public Archivo(String fileId, String nombreArchivo, String mimeType, long tamanio) {
        this();
        this.fileId = fileId;
        this.nombreArchivo = nombreArchivo;
        this.rutaRelativa = fileId; // Por defecto, la ruta es igual al fileId
        this.mimeType = mimeType;
        this.tamanio = tamanio;
    }

    // Getters y Setters
    public UUID getIdUUID() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getRutaRelativa() {
        return rutaRelativa;
    }

    public void setRutaRelativa(String rutaRelativa) {
        this.rutaRelativa = rutaRelativa;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getTamanio() {
        return tamanio;
    }

    public void setTamanio(long tamanio) {
        this.tamanio = tamanio;
    }

    public String getHashSHA256() {
        return hashSHA256;
    }

    public void setHashSHA256(String hashSHA256) {
        this.hashSHA256 = hashSHA256;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaUltimaActualizacion() {
        return fechaUltimaActualizacion;
    }

    public void setFechaUltimaActualizacion(Instant fechaUltimaActualizacion) {
        this.fechaUltimaActualizacion = fechaUltimaActualizacion;
    }

    @Override
    public String getId() {
        return id.toString();
    }

    @Override
    public String getDatosParaHash() {
        return id.toString() +
               (fileId != null ? fileId : "") +
               (nombreArchivo != null ? nombreArchivo : "") +
               (hashSHA256 != null ? hashSHA256 : "") +
               tamanio ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Archivo archivo = (Archivo) o;
        return Objects.equals(id, archivo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Archivo{" +
                "id=" + id +
                ", fileId='" + fileId + '\'' +
                ", nombreArchivo='" + nombreArchivo + '\'' +
                ", tamanio=" + tamanio +
                '}';
    }
}
