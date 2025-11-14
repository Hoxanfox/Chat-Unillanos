package com.arquitectura.logicaPeers.manager;

import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.domain.Peer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeerManager {
    PeerResponseDto agregarPeer(String ip, int puerto) throws Exception;
    PeerResponseDto agregarPeer(String ip, int puerto, String nombreServidor) throws Exception;
    Optional<PeerResponseDto> buscarPeerPorIpYPuerto(String ip, int puerto);
    Optional<PeerResponseDto> buscarPeerPorId(UUID peerId);
    PeerResponseDto registrarPeerAutenticado(UUID peerId, String ip, Integer puerto);
    void marcarPeerComoDesconectado(UUID peerId);
    PeerResponseDto obtenerOCrearPeerLocal(String ip, int puerto);
    List<PeerResponseDto> listarPeersDisponibles();
    List<PeerResponseDto> listarPeersActivos();
    PeerResponseDto obtenerPeer(UUID peerId) throws Exception;
    void actualizarEstadoPeer(UUID peerId, String estado) throws Exception;
    void eliminarPeer(UUID peerId) throws Exception;
    void reportarLatido(UUID peerId) throws Exception;
    void reportarLatido(UUID peerId, String ip, int puerto) throws Exception;
    int verificarPeersInactivos();
    long obtenerIntervaloHeartbeat();
    byte[] descargarArchivoDesdePeer(UUID peerDestinoId, String fileId) throws Exception;
    com.arquitectura.domain.Peer obtenerPeerActual();
    UUID obtenerPeerActualId();
    long contarTotalPeers();
    long contarPeersActivos();
    long contarPeersInactivos();
}

