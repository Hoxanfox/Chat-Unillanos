package com.arquitectura.fachada.p2p;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.logicaPeers.IPeerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Implementación de la fachada P2P.
 * Coordina las operaciones de la red peer-to-peer del sistema.
 */
@Component
public class P2PFachadaImpl implements IP2PFachada {

    private final IPeerService peerService;

    @Autowired
    public P2PFachadaImpl(@Qualifier("peerServiceP2P") IPeerService peerService) {
        this.peerService = peerService;
    }

    @Override
    public com.arquitectura.DTO.p2p.PeerResponseDto agregarPeer(String ip, int puerto) throws Exception {
        return peerService.agregarPeer(ip, puerto);
    }

    @Override
    public com.arquitectura.DTO.p2p.PeerResponseDto agregarPeer(String ip, int puerto, String nombreServidor) throws Exception {
        return peerService.agregarPeer(ip, puerto, nombreServidor);
    }

    @Override
    public List<com.arquitectura.DTO.p2p.PeerResponseDto> listarPeersDisponibles() {
        return peerService.listarPeersDisponibles();
    }

    @Override
    public List<com.arquitectura.DTO.p2p.PeerResponseDto> listarPeersActivos() {
        return peerService.listarPeersActivos();
    }

    @Override
    public void reportarLatido(UUID peerId) throws Exception {
        peerService.reportarLatido(peerId);
    }

    @Override
    public void reportarLatido(UUID peerId, String ip, int puerto) throws Exception {
        peerService.reportarLatido(peerId, ip, puerto);
    }

    @Override
    public long obtenerIntervaloHeartbeat() {
        return peerService.obtenerIntervaloHeartbeat();
    }

    @Override
    public DTOResponse retransmitirPeticion(UUID peerDestinoId, DTORequest peticionOriginal) throws Exception {
        return peerService.retransmitirPeticion(peerDestinoId, peticionOriginal);
    }

    @Override
    public byte[] descargarArchivoDesdePeer(UUID peerDestinoId, String fileId) throws Exception {
        return peerService.descargarArchivoDesdePeer(peerDestinoId, fileId);
    }

    @Override
    public com.arquitectura.DTO.p2p.UserLocationResponseDto buscarUsuario(UUID usuarioId) throws Exception {
        return peerService.buscarUsuario(usuarioId);
    }
}

