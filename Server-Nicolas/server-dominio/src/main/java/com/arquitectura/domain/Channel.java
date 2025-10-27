package com.arquitectura.domain;

import com.arquitectura.domain.enums.TipoCanal;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

@Entity
@Table(name = "channels")
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_id")
    private int channelId;

    @Column(name = "channel_name",nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY) // Relación: Muchos canales pueden tener un mismo dueño.
    @JoinColumn(name = "owner_id", nullable = false) // Clave foránea en la tabla 'channels'.
    private User owner;

    @Column(nullable = false) // ¡AÑADIDO!
    @Enumerated(EnumType.STRING) // Indica a JPA cómo guardar el Enum
    private TipoCanal tipo; // Necesitarás crear un Enum ChannelType { DIRECTO, GRUPO }

    @OneToMany(mappedBy = "canal", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MembresiaCanal> membresias;



    // Constructor para crear un nuevo canal
    public Channel(  String name,User owner, TipoCanal tipo) {
        this.name = name;
        this.owner = owner;
        this.tipo = tipo;
        this.membresias = new HashSet<>();
    }

    public Channel() {

    }

    // --- Getters y Setters ---

    public TipoCanal getTipo() {
        return tipo;
    }

    public void setTipo(TipoCanal tipo) {
        this.tipo = tipo;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int id) {
        this.channelId = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Set<MembresiaCanal> getMembresias() {
        return membresias;
    }

    public void setMembresias(Set<MembresiaCanal> membresias) {
        this.membresias = membresias;
    }
}