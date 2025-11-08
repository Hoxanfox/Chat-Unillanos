package com.arquitectura.DTO.p2p;

import java.util.UUID;

/**
 * DTO para reportar un heartbeat (latido) de un peer.
 * Los peers envían heartbeats periódicamente para indicar que están activos.
 */
public class ReportHeartbeatRequestDto {
    
    private UUID peerId;
    private String ip;
    private int puerto;

    // Constructores
    public ReportHeartbeatRequestDto() {
    }

    public ReportHeartbeatRequestDto(UUID peerId, String ip, int puerto) {
        this.peerId = peerId;
        this.ip = ip;
        this.puerto = puerto;
    }

    // Getters y Setters
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

    public int getPuerto() {
        return puerto;
    }

    public void setPuerto(int puerto) {
        this.puerto = puerto;
    }

    @Override
    public String toString() {
        return "ReportHeartbeatRequestDto{" +
                "peerId=" + peerId +
                ", ip='" + ip + '\'' +
                ", puerto=" + puerto +
                '}';
    }
}
