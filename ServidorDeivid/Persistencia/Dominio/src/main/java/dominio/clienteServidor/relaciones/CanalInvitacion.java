package dominio.clienteServidor.relaciones;

import dominio.merkletree.IMerkleEntity;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Representa una invitación pendiente a un canal.
 * Tabla: canal_invitaciones
 * Una invitación se crea cuando un admin invita a un usuario.
 * Se elimina cuando el usuario acepta o rechaza la invitación.
 */
public class CanalInvitacion implements Serializable, IMerkleEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID canalId;
    private UUID invitadorId;  // Usuario que envía la invitación (admin del canal)
    private UUID invitadoId;   // Usuario que recibe la invitación
    private Instant fechaCreacion;
    private String estado;     // "PENDIENTE", "ACEPTADA", "RECHAZADA"

    public CanalInvitacion() {
        this.id = UUID.randomUUID();
        this.fechaCreacion = Instant.now();
        this.estado = "PENDIENTE";
    }

    public CanalInvitacion(UUID canalId, UUID invitadorId, UUID invitadoId) {
        this();
        this.canalId = canalId;
        this.invitadorId = invitadorId;
        this.invitadoId = invitadoId;
    }

    public CanalInvitacion(UUID id, UUID canalId, UUID invitadorId, UUID invitadoId,
                          Instant fechaCreacion, String estado) {
        this.id = id;
        this.canalId = canalId;
        this.invitadorId = invitadorId;
        this.invitadoId = invitadoId;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
    }

    // Getters y Setters
    public UUID getIdUUID() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCanalId() { return canalId; }
    public void setCanalId(UUID canalId) { this.canalId = canalId; }

    public UUID getInvitadorId() { return invitadorId; }
    public void setInvitadorId(UUID invitadorId) { this.invitadorId = invitadorId; }

    public UUID getInvitadoId() { return invitadoId; }
    public void setInvitadoId(UUID invitadoId) { this.invitadoId = invitadoId; }

    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    // --- IMPLEMENTACIÓN MERKLE ---
    @Override
    public String getId() {
        return id.toString();
    }

    @Override
    public String getDatosParaHash() {
        return id.toString() + canalId.toString() + invitadorId.toString() +
               invitadoId.toString() + estado;
    }

    @Override
    public String toString() {
        return "CanalInvitacion{" +
                "id=" + id +
                ", canalId=" + canalId +
                ", invitadorId=" + invitadorId +
                ", invitadoId=" + invitadoId +
                ", fechaCreacion=" + fechaCreacion +
                ", estado='" + estado + '\'' +
                '}';
    }
}
