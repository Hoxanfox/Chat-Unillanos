package dominio.clienteServidor.relaciones;

import dominio.merkletree.IMerkleEntity;
import java.io.Serializable;
import java.util.UUID;

/**
 * Representa una fila de la tabla 'canal_miembros'.
 * Es necesaria para sincronizar quién pertenece a qué canal.
 */
public class CanalMiembro implements Serializable, IMerkleEntity {

    private static final long serialVersionUID = 1L;

    private UUID canalId;
    private UUID usuarioId;

    public CanalMiembro() {}

    public CanalMiembro(UUID canalId, UUID usuarioId) {
        this.canalId = canalId;
        this.usuarioId = usuarioId;
    }

    public UUID getCanalId() { return canalId; }
    public void setCanalId(UUID canalId) { this.canalId = canalId; }
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }

    // --- IMPLEMENTACIÓN MERKLE ---
    @Override
    public String getId() {
        // ID compuesto virtual
        return canalId.toString() + "_" + usuarioId.toString();
    }

    @Override
    public String getDatosParaHash() {
        // La existencia de la relación es el dato en sí
        return getId();
    }
}