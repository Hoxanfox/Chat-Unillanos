package com.arquitectura.domain;

import jakarta.persistence.*;
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

    public Peer() {
    }

    public Peer(String ip) {
        this.ip = ip;
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
}