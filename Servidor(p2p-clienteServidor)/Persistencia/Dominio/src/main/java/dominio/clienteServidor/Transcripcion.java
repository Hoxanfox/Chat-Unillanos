package dominio.clienteServidor;

import dominio.merkletree.IMerkleEntity;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad que representa una transcripción de audio
 */
public class Transcripcion implements Serializable, IMerkleEntity {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID archivoId;           // Referencia al archivo de audio
    private UUID mensajeId;           // Referencia al mensaje (opcional)
    private String transcripcion;     // Texto transcrito
    private EstadoTranscripcion estado;
    private BigDecimal duracionSegundos;
    private String idioma;
    private BigDecimal confianza;     // 0-100
    private Instant fechaCreacion;
    private Instant fechaProcesamiento;
    private Instant fechaActualizacion;

    public enum EstadoTranscripcion {
        PENDIENTE, PROCESANDO, COMPLETADA, ERROR
    }

    public Transcripcion() {
        this.id = UUID.randomUUID();
        this.estado = EstadoTranscripcion.PENDIENTE;
        this.idioma = "es";
        this.fechaCreacion = Instant.now();
        this.fechaActualizacion = Instant.now();
    }

    public Transcripcion(UUID archivoId) {
        this();
        this.archivoId = archivoId;
    }

    public Transcripcion(UUID archivoId, UUID mensajeId) {
        this(archivoId);
        this.mensajeId = mensajeId;
    }

    // --- IMPLEMENTACIÓN MERKLE ---

    @Override
    public String getId() {
        return id.toString();
    }

    @Override
    public String getDatosParaHash() {
        return id.toString() + "|" +
                (archivoId != null ? archivoId.toString() : "") + "|" +
                (mensajeId != null ? mensajeId.toString() : "") + "|" +
                (transcripcion != null ? transcripcion : "") + "|" +
                (estado != null ? estado.name() : "");
    }

    // --- Getters y Setters ---

    public UUID getIdUUID() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getArchivoId() {
        return archivoId;
    }

    public void setArchivoId(UUID archivoId) {
        this.archivoId = archivoId;
    }

    public UUID getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(UUID mensajeId) {
        this.mensajeId = mensajeId;
    }

    public String getTranscripcion() {
        return transcripcion;
    }

    public void setTranscripcion(String transcripcion) {
        this.transcripcion = transcripcion;
        this.fechaActualizacion = Instant.now();
    }

    public EstadoTranscripcion getEstado() {
        return estado;
    }

    public void setEstado(EstadoTranscripcion estado) {
        this.estado = estado;
        this.fechaActualizacion = Instant.now();

        if (estado == EstadoTranscripcion.COMPLETADA || estado == EstadoTranscripcion.ERROR) {
            this.fechaProcesamiento = Instant.now();
        }
    }

    public BigDecimal getDuracionSegundos() {
        return duracionSegundos;
    }

    public void setDuracionSegundos(BigDecimal duracionSegundos) {
        this.duracionSegundos = duracionSegundos;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public BigDecimal getConfianza() {
        return confianza;
    }

    public void setConfianza(BigDecimal confianza) {
        this.confianza = confianza;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(Instant fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }

    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(Instant fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transcripcion that = (Transcripcion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transcripcion{" +
                "id=" + id +
                ", archivoId=" + archivoId +
                ", mensajeId=" + mensajeId +
                ", estado=" + estado +
                ", idioma='" + idioma + '\'' +
                ", transcripcion='" + (transcripcion != null ? transcripcion.substring(0, Math.min(50, transcripcion.length())) + "..." : "null") + '\'' +
                '}';
    }
}

