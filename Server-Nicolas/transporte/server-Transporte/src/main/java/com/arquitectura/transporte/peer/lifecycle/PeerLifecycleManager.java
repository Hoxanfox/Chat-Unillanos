package com.arquitectura.transporte.peer.lifecycle;

import com.arquitectura.controlador.peer.IPeerHandler;
import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.logicaPeers.IPeerService;
import com.arquitectura.transporte.peer.PeerConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Maneja el ciclo de vida de peers (autenticación, desconexión, limpieza)
 */
public class PeerLifecycleManager {
    private static final Logger log = LoggerFactory.getLogger(PeerLifecycleManager.class);

    private final PeerConnectionManager manager;
    private final IPeerService peerService;
    private final Map<UUID, com.arquitectura.controlador.peer.IPeerHandler> activePeerConnections;

    public PeerLifecycleManager(PeerConnectionManager manager,
                                IPeerService peerService,
                                Map<UUID, com.arquitectura.controlador.peer.IPeerHandler> activePeerConnections) {
        this.manager = manager;
        this.peerService = peerService;
        this.activePeerConnections = activePeerConnections;
    }

    public void onPeerAuthenticated(IPeerHandler handler) {
        UUID peerId = handler.getPeerId();

        if (activePeerConnections.containsKey(peerId)) {
            IPeerHandler existing = activePeerConnections.get(peerId);
            if (existing != null && existing != handler && existing.isConnected()) {
                log.warn("Ya existe una conexión activa para peer {}. Cerrando conexión duplicada.", peerId);
                handler.forceDisconnect();
                return;
            }
        }

        activePeerConnections.put(peerId, handler);
        log.info("Peer {} autenticado y agregado al pool. Total peers activos: {}", peerId, activePeerConnections.size());

        manager.getEventPublisher().publishEvent(new com.arquitectura.events.PeerConnectedEvent(
                peerId, handler.getPeerIp(), handler.getPeerPort()
        ));

        try {
            PeerResponseDto peerDto = peerService.registrarPeerAutenticado(
                    peerId,
                    handler.getPeerIp(),
                    handler.getPeerPort()
            );

            log.info("✓ Peer {} guardado/actualizado en BD ({}:{})", peerId, peerDto.getIp(), peerDto.getPuerto());
        } catch (Exception e) {
            log.error("Error al registrar peer autenticado: {}", e.getMessage(), e);
        }
    }

    public void removePeerConnection(IPeerHandler handler) {
        UUID peerId = handler.getPeerId();
        if (peerId != null) {
            activePeerConnections.remove(peerId);
            log.info("Peer {} removido del pool. Peers activos restantes: {}", peerId, activePeerConnections.size());

            manager.getEventPublisher().publishEvent(new com.arquitectura.events.PeerDisconnectedEvent(peerId, "Conexión cerrada"));

            try {
                peerService.marcarPeerComoDesconectado(peerId);
            } catch (Exception e) {
                log.warn("Error marcando peer {} como desconectado: {}", peerId, e.getMessage());
            }
        }
    }

    public void markPeerAsOfflineAfterFailure(UUID peerId) {
        try {
            log.info("Marcando peer {} como OFFLINE tras fallos consecutivos de conexión", peerId);
            peerService.marcarPeerComoDesconectado(peerId);
        } catch (Exception e) {
            log.warn("Error al marcar peer {} como OFFLINE: {}", peerId, e.getMessage());
        }
    }

    // Expose some state if needed
    public Set<UUID> getActivePeerIds() {
        return activePeerConnections.keySet();
    }
}

