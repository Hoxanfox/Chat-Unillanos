package com.arquitectura.DTO.p2p;

import java.util.List;

/**
 * DTO de respuesta que contiene una lista de peers.
 * Se utiliza para enviar la lista completa de peers conocidos.
 */
public class PeerListResponseDto {
    
    private List<PeerResponseDto> peers;
    private int totalPeers;
    private int peersActivos;
    private int peersInactivos;

    // Constructores
    public PeerListResponseDto() {
    }

    public PeerListResponseDto(List<PeerResponseDto> peers) {
        this.peers = peers;
        this.totalPeers = peers != null ? peers.size() : 0;
        calcularEstadisticas();
    }

    // Método auxiliar para calcular estadísticas
    private void calcularEstadisticas() {
        if (peers == null) {
            this.peersActivos = 0;
            this.peersInactivos = 0;
            return;
        }
        
        this.peersActivos = (int) peers.stream()
                .filter(p -> "ONLINE".equals(p.getConectado()))
                .count();
        
        this.peersInactivos = (int) peers.stream()
                .filter(p -> "OFFLINE".equals(p.getConectado()))
                .count();
    }

    // Getters y Setters
    public List<PeerResponseDto> getPeers() {
        return peers;
    }

    public void setPeers(List<PeerResponseDto> peers) {
        this.peers = peers;
        this.totalPeers = peers != null ? peers.size() : 0;
        calcularEstadisticas();
    }

    public int getTotalPeers() {
        return totalPeers;
    }

    public void setTotalPeers(int totalPeers) {
        this.totalPeers = totalPeers;
    }

    public int getPeersActivos() {
        return peersActivos;
    }

    public void setPeersActivos(int peersActivos) {
        this.peersActivos = peersActivos;
    }

    public int getPeersInactivos() {
        return peersInactivos;
    }

    public void setPeersInactivos(int peersInactivos) {
        this.peersInactivos = peersInactivos;
    }

    @Override
    public String toString() {
        return "PeerListResponseDto{" +
                "totalPeers=" + totalPeers +
                ", peersActivos=" + peersActivos +
                ", peersInactivos=" + peersInactivos +
                ", peers=" + peers +
                '}';
    }
}
