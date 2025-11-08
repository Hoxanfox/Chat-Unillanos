package com.arquitectura.DTO.peers;

import java.util.UUID;

public class PeerResponseDto {
    private UUID peerId;
    private String ip;
    private Integer puerto;
    private String conectado;

    public PeerResponseDto() {
    }

    public PeerResponseDto(UUID peerId, String ip, Integer puerto, String conectado) {
        this.peerId = peerId;
        this.ip = ip;
        this.puerto = puerto;
        this.conectado = conectado;
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
}

