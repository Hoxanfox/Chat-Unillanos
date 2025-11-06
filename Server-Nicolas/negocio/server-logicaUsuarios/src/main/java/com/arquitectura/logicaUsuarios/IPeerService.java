package com.arquitectura.logicaUsuarios;

import com.arquitectura.DTO.peers.AddPeerRequestDto;
import com.arquitectura.DTO.peers.HeartbeatRequestDto;
import com.arquitectura.DTO.peers.HeartbeatResponseDto;
import com.arquitectura.DTO.peers.PeerResponseDto;
import com.arquitectura.DTO.peers.RetransmitRequestDto;
import com.arquitectura.DTO.peers.RetransmitResponseDto;
import com.arquitectura.DTO.peers.UpdatePeerListRequestDto;
import com.arquitectura.DTO.peers.UpdatePeerListResponseDto;

import java.util.List;
import java.util.UUID;

public interface IPeerService {
    List<PeerResponseDto> listarPeersDisponibles(UUID excludePeerId);
    HeartbeatResponseDto reportarLatido(HeartbeatRequestDto requestDto) throws Exception;
    PeerResponseDto añadirPeer(AddPeerRequestDto requestDto) throws Exception;
    PeerResponseDto verificarEstadoPeer(UUID peerId) throws Exception;
    RetransmitResponseDto retransmitirPeticion(RetransmitRequestDto requestDto) throws Exception;
    UpdatePeerListResponseDto actualizarListaPeers(UpdatePeerListRequestDto requestDto) throws Exception;
}
