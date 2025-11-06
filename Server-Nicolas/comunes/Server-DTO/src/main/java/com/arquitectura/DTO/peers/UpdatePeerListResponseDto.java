package com.arquitectura.DTO.peers;

import java.util.List;

/**
 * DTO para responder a una actualización de la lista de peers
 */
public class UpdatePeerListResponseDto {
    private List<PeerResponseDto> listaPeers;

    public UpdatePeerListResponseDto() {
    }

    public UpdatePeerListResponseDto(List<PeerResponseDto> listaPeers) {
        this.listaPeers = listaPeers;
    }

    public List<PeerResponseDto> getListaPeers() {
        return listaPeers;
    }

    public void setListaPeers(List<PeerResponseDto> listaPeers) {
        this.listaPeers = listaPeers;
    }
}

