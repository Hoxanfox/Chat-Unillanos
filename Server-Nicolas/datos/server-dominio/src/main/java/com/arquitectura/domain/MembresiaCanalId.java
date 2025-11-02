package com.arquitectura.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class MembresiaCanalId implements Serializable {
    @Column(name = "user_id")
    private UUID idUsuario;

    @Column(name = "channel_id")
    private UUID idCanal;

    public MembresiaCanalId() {
    }

    public MembresiaCanalId(UUID idCanal, UUID idUsuario) {
        this.idCanal = idCanal;
        this.idUsuario = idUsuario;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(UUID idUsuario) {
        this.idUsuario = idUsuario;
    }

    public UUID getIdCanal() {
        return idCanal;
    }

    public void setIdCanal(UUID idCanal) {
        this.idCanal = idCanal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MembresiaCanalId that = (MembresiaCanalId) o;
        return Objects.equals(idCanal, that.idCanal) && Objects.equals(idUsuario, that.idUsuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuario, idCanal);
    }
}
