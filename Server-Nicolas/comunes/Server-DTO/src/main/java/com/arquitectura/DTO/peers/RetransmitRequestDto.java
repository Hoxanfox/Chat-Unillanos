package com.arquitectura.DTO.peers;

import java.util.UUID;

/**
 * DTO para retransmitir una petición de un cliente a través de otro peer
 */
public class RetransmitRequestDto {
    private PeerInfo peerOrigen;
    private Object peticionCliente; // La petición original del cliente (puede ser cualquier JSON)

    public RetransmitRequestDto() {
    }

    public RetransmitRequestDto(PeerInfo peerOrigen, Object peticionCliente) {
        this.peerOrigen = peerOrigen;
        this.peticionCliente = peticionCliente;
    }

    public PeerInfo getPeerOrigen() {
        return peerOrigen;
    }

    public void setPeerOrigen(PeerInfo peerOrigen) {
        this.peerOrigen = peerOrigen;
    }

    public Object getPeticionCliente() {
        return peticionCliente;
    }

    public void setPeticionCliente(Object peticionCliente) {
        this.peticionCliente = peticionCliente;
    }

    /**
     * Clase interna para representar la información del peer de origen
     */
    public static class PeerInfo {
        private UUID peerId;
        private String ip;
        private Integer puerto;

        public PeerInfo() {
        }

        public PeerInfo(UUID peerId, String ip, Integer puerto) {
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
}

