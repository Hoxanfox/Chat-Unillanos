package com.arquitectura.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MembresiaCanalId implements Serializable {
    @Column(name = "user_id")
    private int idUsuario;

    @Column(name = "channel_id")
    private int idCanal;

    public MembresiaCanalId() {
    }

    public MembresiaCanalId(int idCanal, int idUsuario) {
        this.idCanal = idCanal;
        this.idUsuario = idUsuario;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public int getIdCanal() {
        return idCanal;
    }

    public void setIdCanal(int idCanal) {
        this.idCanal = idCanal;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MembresiaCanalId that = (MembresiaCanalId) o;
        return idUsuario == that.idUsuario && idCanal == that.idCanal;
    }
    @Override
    public int hashCode() {
        return Objects.hash(idUsuario, idCanal);
    }
}
