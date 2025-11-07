package com.arquitectura.DTO.p2p;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import java.util.UUID;

/**
 * DTO para retransmitir una petición de un cliente a otro peer.
 * Permite que un servidor reenvíe peticiones a otros servidores en la red P2P.
 */
public class RetransmitRequestDto {
    
    private UUID peerDestinoId;
    private PeerOriginDto peerOrigen;
    private DTORequest peticionOriginal;

    // Constructores
    public RetransmitRequestDto() {
    }

    public RetransmitRequestDto(UUID peerDestinoId, PeerOriginDto peerOrigen, DTORequest peticionOriginal) {
        this.peerDestinoId = peerDestinoId;
        this.peerOrigen = peerOrigen;
        this.peticionOriginal = peticionOriginal;
    }

    // Getters y Setters
    public UUID getPeerDestinoId() {
        return peerDestinoId;
    }

    public void setPeerDestinoId(UUID peerDestinoId) {
        this.peerDestinoId = peerDestinoId;
    }

    public PeerOriginDto getPeerOrigen() {
        return peerOrigen;
    }

    public void setPeerOrigen(PeerOriginDto peerOrigen) {
        this.peerOrigen = peerOrigen;
    }

    public DTORequest getPeticionOriginal() {
        return peticionOriginal;
    }

    public void setPeticionOriginal(DTORequest peticionOriginal) {
        this.peticionOriginal = peticionOriginal;
    }

    @Override
    public String toString() {
        return "RetransmitRequestDto{" +
                "peerDestinoId=" + peerDestinoId +
                ", peerOrigen=" + peerOrigen +
                ", peticionOriginal=" + peticionOriginal +
                '}';
    }

    /**
     * Clase interna para representar el peer de origen
     */
    public static class PeerOriginDto {
        private UUID peerId;
        private String ip;
        private int puerto;

        public PeerOriginDto() {
        }

        public PeerOriginDto(UUID peerId, String ip, int puerto) {
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

        public int getPuerto() {
            return puerto;
        }

        public void setPuerto(int puerto) {
            this.puerto = puerto;
        }

        @Override
        public String toString() {
            return "PeerOriginDto{" +
                    "peerId=" + peerId +
                    ", ip='" + ip + '\'' +
                    ", puerto=" + puerto +
                    '}';
        }
    }
}
