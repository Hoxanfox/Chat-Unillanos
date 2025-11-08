package com.arquitectura.DTO.p2p;

import java.util.UUID;

/**
 * DTO de respuesta que contiene la ubicación de un usuario en la red P2P.
 * Indica en qué peer está conectado un usuario específico.
 */
public class UserLocationResponseDto {
    
    private UUID usuarioId;
    private String username;
    private UUID peerId;
    private String peerIp;
    private Integer peerPuerto;
    private boolean conectado;

    // Constructores
    public UserLocationResponseDto() {
    }

    public UserLocationResponseDto(UUID usuarioId, String username, UUID peerId, 
                                   String peerIp, Integer peerPuerto, boolean conectado) {
        this.usuarioId = usuarioId;
        this.username = username;
        this.peerId = peerId;
        this.peerIp = peerIp;
        this.peerPuerto = peerPuerto;
        this.conectado = conectado;
    }

    // Getters y Setters
    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UUID getPeerId() {
        return peerId;
    }

    public void setPeerId(UUID peerId) {
        this.peerId = peerId;
    }

    public String getPeerIp() {
        return peerIp;
    }

    public void setPeerIp(String peerIp) {
        this.peerIp = peerIp;
    }

    public Integer getPeerPuerto() {
        return peerPuerto;
    }

    public void setPeerPuerto(Integer peerPuerto) {
        this.peerPuerto = peerPuerto;
    }

    public boolean isConectado() {
        return conectado;
    }

    public void setConectado(boolean conectado) {
        this.conectado = conectado;
    }

    @Override
    public String toString() {
        return "UserLocationResponseDto{" +
                "usuarioId=" + usuarioId +
                ", username='" + username + '\'' +
                ", peerId=" + peerId +
                ", peerIp='" + peerIp + '\'' +
                ", peerPuerto=" + peerPuerto +
                ", conectado=" + conectado +
                '}';
    }
}
