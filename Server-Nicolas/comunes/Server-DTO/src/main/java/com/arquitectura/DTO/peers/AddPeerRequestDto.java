package com.arquitectura.DTO.peers;

public class AddPeerRequestDto {
    private String ip;
    private Integer puerto;

    public AddPeerRequestDto() {
    }

    public AddPeerRequestDto(String ip, Integer puerto) {
        this.ip = ip;
        this.puerto = puerto;
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

