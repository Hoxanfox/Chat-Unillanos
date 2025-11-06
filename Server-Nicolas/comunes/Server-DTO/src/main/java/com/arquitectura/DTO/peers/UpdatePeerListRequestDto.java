package com.arquitectura.DTO.peers;

import java.util.List;

/**
 * DTO para recibir actualizaciones push de la lista completa de peers desde otro servidor
 */
public class UpdatePeerListRequestDto {
    private List<PeerInfo> listaPeers;

    public UpdatePeerListRequestDto() {
    }

    public UpdatePeerListRequestDto(List<PeerInfo> listaPeers) {
        this.listaPeers = listaPeers;
    }

    public List<PeerInfo> getListaPeers() {
        return listaPeers;
    }

    public void setListaPeers(List<PeerInfo> listaPeers) {
        this.listaPeers = listaPeers;
    }

    public static class PeerInfo {
        private String peerId;
        private String ip;
        private Integer puerto;
        private String conectado;

        public PeerInfo() {
        }

        public PeerInfo(String peerId, String ip, Integer puerto, String conectado) {
            this.peerId = peerId;
            this.ip = ip;
            this.puerto = puerto;
            this.conectado = conectado;
        }

        public String getPeerId() {
            return peerId;
        }

        public void setPeerId(String peerId) {
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
}

