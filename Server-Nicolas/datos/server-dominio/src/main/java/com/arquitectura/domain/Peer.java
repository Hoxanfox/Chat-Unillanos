package com.arquitectura.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "peers")
public class Peer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID peerId;

    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    @Column(name = "puerto", nullable = false)
    private Integer puerto;

    @Column(name = "conectado", nullable = false, length = 20)
    private String conectado;

    @Column(name = "ultimo_latido")
    private LocalDateTime ultimoLatido;

    public Peer() {
    }

    public Peer(String ip, Integer puerto, String conectado) {
        this.ip = ip;
        this.puerto = puerto;
        this.conectado = conectado;
        this.ultimoLatido = LocalDateTime.now();
    }

    public UUID getPeerId() {
        return peerId;
    }

    public void setPeerId(UUID peerId) {
        this.peerId = peerId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPuerto() {
        return puerto;
    }

    public void setPuerto(Integer puerto) {
        this.puerto = puerto;
    }

    public String getConectado() {
        return conectado;
    }

    public void setConectado(String conectado) {
        this.conectado = conectado;
    }

    public LocalDateTime getUltimoLatido() {
        return ultimoLatido;
    }

    public void setUltimoLatido(LocalDateTime ultimoLatido) {
        this.ultimoLatido = ultimoLatido;
    }

    public void actualizarLatido() {
        this.ultimoLatido = LocalDateTime.now();
        this.conectado = "ONLINE";
    }
}