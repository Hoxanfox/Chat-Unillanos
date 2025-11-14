package com.arquitectura.transporte.peer.outgoing;

import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.logicaPeers.IPeerService;
import com.arquitectura.transporte.peer.PeerConnectionManager;
import com.arquitectura.transporte.peer.PeerOutgoingConnection;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Gestor de conexiones salientes P2P (subpaquete outgoing)
 */
public class OutgoingConnectionManager {
    private static final Logger log = LoggerFactory.getLogger(OutgoingConnectionManager.class);

    private final Map<UUID, PeerOutgoingConnection> outgoingConnections;
    private final Gson gson;
    private final PeerConnectionManager manager;
    private final IPeerService peerService;
    private final ExecutorService peerPool;
    private final int reconnectAttempts;
    private final long reconnectDelayMs;

    public OutgoingConnectionManager(Map<UUID, PeerOutgoingConnection> outgoingConnections,
                                     Gson gson,
                                     PeerConnectionManager manager,
                                     IPeerService peerService,
                                     ExecutorService peerPool,
                                     int reconnectAttempts,
                                     long reconnectDelayMs) {
        this.outgoingConnections = outgoingConnections;
        this.gson = gson;
        this.manager = manager;
        this.peerService = peerService;
        this.peerPool = peerPool;
        this.reconnectAttempts = reconnectAttempts;
        this.reconnectDelayMs = reconnectDelayMs;
    }

    public void connectToPeer(UUID peerId, String ip, int port) {
        if (peerId.equals(manager.getLocalPeerId())) {
            log.debug("OutgoingManager: Ignorando conexión a sí mismo");
            return;
        }

        if (outgoingConnections.containsKey(peerId)) {
            PeerOutgoingConnection outgoing = outgoingConnections.get(peerId);
            if (outgoing != null && outgoing.isConnected()) {
                log.debug("OutgoingManager: Ya existe una conexión saliente activa con peer {}", peerId);
                return;
            } else {
                outgoingConnections.remove(peerId);
            }
        }

        log.info("OutgoingManager: Iniciando conexión saliente a peer {} ({}:{})", peerId, ip, port);
        PeerOutgoingConnection outgoing = new PeerOutgoingConnection(
                peerId, ip, port, gson, manager, reconnectAttempts, reconnectDelayMs
        );
        outgoingConnections.put(peerId, outgoing);
        peerPool.submit(outgoing);
    }

    public void connectToAllKnownPeers() {
        List<PeerResponseDto> peers = peerService.listarPeersDisponibles();
        log.info("OutgoingManager: Conectando a {} peers conocidos en BD...", peers == null ? 0 : peers.size());
        if (peers == null || peers.isEmpty()) {
            log.warn("OutgoingManager: No hay peers registrados en la base de datos.");
            return;
        }

        for (PeerResponseDto peer : peers) {
            if (peer.getPeerId().equals(manager.getLocalPeerId())) continue;
            if (peer.getIp() == null || peer.getIp().trim().isEmpty()) continue;
            if (peer.getPuerto() <= 0 || peer.getPuerto() > 65535) continue;
            connectToPeer(peer.getPeerId(), peer.getIp(), peer.getPuerto());
        }
    }

    public void disconnectAll() {
        outgoingConnections.values().forEach(PeerOutgoingConnection::disconnect);
        outgoingConnections.clear();
    }

    public boolean isConnectedToPeer(UUID peerId) {
        PeerOutgoingConnection outgoing = outgoingConnections.get(peerId);
        return outgoing != null && outgoing.isConnected();
    }
}

