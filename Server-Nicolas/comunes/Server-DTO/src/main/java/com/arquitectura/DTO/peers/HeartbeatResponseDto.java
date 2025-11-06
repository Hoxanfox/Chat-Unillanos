package com.arquitectura.DTO.peers;

public class HeartbeatResponseDto {
    private Long proximoLatidoMs;

    public HeartbeatResponseDto() {
    }

    public HeartbeatResponseDto(Long proximoLatidoMs) {
        this.proximoLatidoMs = proximoLatidoMs;
    }

    public Long getProximoLatidoMs() {
        return proximoLatidoMs;
    }

    public void setProximoLatidoMs(Long proximoLatidoMs) {
        this.proximoLatidoMs = proximoLatidoMs;
    }
}
