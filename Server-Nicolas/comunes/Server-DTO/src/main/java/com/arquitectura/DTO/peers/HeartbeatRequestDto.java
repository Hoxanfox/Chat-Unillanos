package com.arquitectura.DTO.peers;

import java.util.UUID;

public class HeartbeatRequestDto {
    private UUID peerId;
    private String ip;
    private Integer puerto;

    public HeartbeatRequestDto() {
    }

    public HeartbeatRequestDto(UUID peerId, String ip, Integer puerto) {
        this.peerId = peerId;
        this.ip = ip;
        this.puerto = puerto;
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
}

