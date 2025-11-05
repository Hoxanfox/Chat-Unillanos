package com.arquitectura.domain;

import com.arquitectura.domain.enums.EstadoMembresia;
import jakarta.persistence.*;

@Entity
@Table(name = "membresia_canal")
public class MembresiaCanal {

    @EmbeddedId // Usa la clase de clave compuesta como ID
    private MembresiaCanalId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("IdUsuario") // Mapea el campo 'idUsuario' de nuestra clave compuesta a esta relación
    @JoinColumn(name = "user_id")
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("IdCanal") // Mapea el campo 'idCanal' de nuestra clave compuesta a esta relación
    @JoinColumn(name = "channel_id")
    private Channel canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMembresia estado;

    public MembresiaCanal() {
    }

    public MembresiaCanal(MembresiaCanalId id, User usuario, Channel canal, EstadoMembresia estado) {
        this.id = id;
        this.usuario = usuario;
        this.canal = canal;
        this.estado = estado;
    }

    public MembresiaCanalId getId() {
        return id;
    }

    public void setId(MembresiaCanalId id) {
        this.id = id;
    }

    public User getUsuario() {
        return usuario;
    }

    public void setUsuario(User usuario) {
        this.usuario = usuario;
    }

    public Channel getCanal() {
        return canal;
    }

    public void setCanal(Channel canal) {
        this.canal = canal;
    }

    public EstadoMembresia getEstado() {
        return estado;
    }

    public void setEstado(EstadoMembresia estado) {
        this.estado = estado;
    }
}