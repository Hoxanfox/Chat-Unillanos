package dominio.clienteServidor;

import dominio.merkletree.IMerkleEntity;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Mensaje implements Serializable, IMerkleEntity {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID remitenteId;
    private UUID destinatarioUsuarioId; // null si el destinatario es un canal
    private UUID canalId; // null si el destinatario es un usuario

    public enum Tipo { AUDIO, TEXTO }

    private Tipo tipo;
    private String contenido; // texto o enlace al audio
    private Instant fechaEnvio;

    public Mensaje() {
        this.id = UUID.randomUUID();
        this.fechaEnvio = Instant.now();
        this.tipo = Tipo.TEXTO;
    }

    public Mensaje(UUID id, UUID remitenteId, UUID destinatarioUsuarioId, UUID canalId, Tipo tipo, String contenido, Instant fechaEnvio) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.remitenteId = remitenteId;
        this.destinatarioUsuarioId = destinatarioUsuarioId;
        this.canalId = canalId;
        this.tipo = tipo == null ? Tipo.TEXTO : tipo;
        this.contenido = contenido;
        this.fechaEnvio = fechaEnvio == null ? Instant.now() : fechaEnvio;
    }

    // --- IMPLEMENTACIÓN MERKLE ---

    @Override
    public String getId() {
        return id.toString();
    }

    @Override
    public String getDatosParaHash() {
        return id.toString() + "|" +
                (remitenteId != null ? remitenteId.toString() : "") + "|" +
                (destinatarioUsuarioId != null ? destinatarioUsuarioId.toString() : "") + "|" +
                (canalId != null ? canalId.toString() : "") + "|" +
                (tipo != null ? tipo.name() : "") + "|" +
                (contenido != null ? contenido : "") + "|" +
                (fechaEnvio != null ? fechaEnvio.toString() : "");
    }

    // --- Getters y Setters ---

    public void setId(UUID id) { this.id = id; }

    // Helper para obtener el UUID tipado cuando sea necesario en la lógica de negocio
    public UUID getUuid() { return id; }

    public UUID getRemitenteId() { return remitenteId; }
    public void setRemitenteId(UUID remitenteId) { this.remitenteId = remitenteId; }

    public UUID getDestinatarioUsuarioId() { return destinatarioUsuarioId; }
    public void setDestinatarioUsuarioId(UUID destinatarioUsuarioId) { this.destinatarioUsuarioId = destinatarioUsuarioId; }

    public UUID getCanalId() { return canalId; }
    public void setCanalId(UUID canalId) { this.canalId = canalId; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public Instant getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(Instant fechaEnvio) { this.fechaEnvio = fechaEnvio; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mensaje mensaje = (Mensaje) o;
        return Objects.equals(id, mensaje.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Mensaje{" +
                "id=" + id +
                ", remitenteId=" + remitenteId +
                ", canalId=" + canalId +
                ", tipo=" + tipo +
                ", fechaEnvio=" + fechaEnvio +
                '}';
    }
}