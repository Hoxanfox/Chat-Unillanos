package com.arquitectura.transporte.peer;

import com.arquitectura.controlador.peer.IPeerHandler;
import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.events.*;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * PeerConnectionManager aún más simplificado: actúa como orquestador mínimo.
 * - No contiene lógica de negocio ni fallbacks que dupliquen otras clases.
 * - Depende de componentes especializados para toda la funcionalidad.
 */
@PropertySource(value = "file:./config/server.properties", ignoreResourceNotFound = true)
@Component
public class PeerConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(PeerConnectionManager.class);

    @Value("${peer.server.port:22100}")
    private int peerPort;

    @Value("${server.port:22100}")
    private int clientPort;

    @Value("${peer.bootstrap.nodes:}")
    private String bootstrapNodes;

    private final Gson gson;
    private final ApplicationEventPublisher eventPublisher;
    private final com.arquitectura.utils.network.NetworkUtils networkUtils;

    // ID único de este servidor (delegado a PeerRegistry preferentemente)
    private UUID localPeerId;

    // Componentes especializados (opcionales)
    @Autowired(required = false)
    private com.arquitectura.transporte.peer.server.PeerServer peerServer;

    @Autowired(required = false)
    private com.arquitectura.transporte.peer.outgoing.OutgoingConnectionManager outgoingManager;

    @Autowired(required = false)
    private com.arquitectura.transporte.peer.maintenance.MaintenanceService maintenanceService;

    @Autowired(required = false)
    private com.arquitectura.transporte.peer.registry.PeerRegistry peerRegistry;

    @Autowired(required = false)
    private com.arquitectura.transporte.peer.lifecycle.PeerLifecycleManager lifecycleManager;

    @Autowired(required = false)
    private com.arquitectura.transporte.peer.comm.PeerCommunicator communicator;

    @Autowired(required = false)
    private com.arquitectura.transporte.peer.messaging.PeerMessageProcessor messageProcessor;

    @Autowired
    public PeerConnectionManager(
            Gson gson,
            ApplicationEventPublisher eventPublisher,
            com.arquitectura.utils.network.NetworkUtils networkUtils) {
        this.gson = gson;
        this.eventPublisher = eventPublisher;
        this.networkUtils = networkUtils;
    }

    @PostConstruct
    public void init() {
        log.info("PeerConnectionManager inicializado (delegador). Puerto P2P: {}", peerPort);

        if (peerRegistry != null) {
            try {
                this.localPeerId = peerRegistry.initializeLocalPeerId();
                peerRegistry.initializePeersOnStartup();
            } catch (Exception e) {
                log.warn("Error inicializando PeerRegistry: {}", e.getMessage());
            }
        } else {
            log.warn("PeerRegistry no disponible — no se inicializa localPeerId automáticamente");
            this.localPeerId = null;
        }

        if (maintenanceService != null) {
            try { maintenanceService.start(); } catch (Exception e) { log.warn("No se pudo iniciar MaintenanceService: {}", e.getMessage()); }
        } else {
            log.warn("MaintenanceService no disponible — tareas periódicas no se ejecutarán automáticamente");
        }

        if (peerServer != null) {
             try { peerServer.start(); } catch (Exception e) { log.warn("No se pudo iniciar PeerServer: {}", e.getMessage()); }
         } else {
             log.warn("PeerServer no inyectado. No se aceptarán conexiones entrantes en este nodo");
         }

        if (peerRegistry != null && bootstrapNodes != null && !bootstrapNodes.trim().isEmpty()) {
            try { peerRegistry.connectToBootstrapPeers(); } catch (Exception e) { log.warn("Error conectando a bootstrap peers: {}", e.getMessage()); }
        }
    }

    public ApplicationEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    public com.arquitectura.utils.network.NetworkUtils getNetworkUtils() {
        return networkUtils;
    }

    public void initializePeersOnStartup() {
        if (peerRegistry != null) {
            peerRegistry.initializePeersOnStartup();
        } else {
            log.warn("initializePeersOnStartup(): PeerRegistry no disponible");
        }
    }

    public void startPeerServer() {
        if (peerServer != null) {
             peerServer.start();
             return;
         }
         log.warn("startPeerServer(): PeerServer no disponible — operación omitida");
     }

    public void connectToPeer(UUID peerId, String ip, int port) {
        if (peerId == null) return;
        if (localPeerId != null && peerId.equals(localPeerId)) {
            log.debug("Ignorando conexión a sí mismo");
            return;
        }

        if (outgoingManager != null) {
            outgoingManager.connectToPeer(peerId, ip, port);
            return;
        }

        log.warn("connectToPeer(): OutgoingConnectionManager no disponible — no se realizará la conexión a {}:{} (peer {})", ip, port, peerId);
    }

    public void connectToAllKnownPeers() {
        if (outgoingManager != null) {
            outgoingManager.connectToAllKnownPeers();
            return;
        }
        if (peerRegistry != null) {
            peerRegistry.connectToBootstrapPeers();
            return;
        }
        log.warn("connectToAllKnownPeers(): No hay componente disponible para gestionar conexiones salientes");
    }

    public void onPeerAuthenticated(IPeerHandler handler) {
        if (lifecycleManager != null) {
            lifecycleManager.onPeerAuthenticated(handler);
            return;
        }

        UUID peerId = handler.getPeerId();
        log.info("Peer {} autenticado — publicando evento (delegador)", peerId);
        eventPublisher.publishEvent(new PeerConnectedEvent(peerId, handler.getPeerIp(), handler.getPeerPort()));
    }

    public void removePeerConnection(IPeerHandler handler) {
        if (lifecycleManager != null) {
            lifecycleManager.removePeerConnection(handler);
            return;
        }

        UUID peerId = handler.getPeerId();
        if (peerId != null) {
            log.info("Peer {} removido — publicando evento (delegador)", peerId);
            eventPublisher.publishEvent(new PeerDisconnectedEvent(peerId, "Conexión cerrada"));
        }
    }

    public void processPeerRequest(IPeerHandler handler, DTORequest request) {
        if (messageProcessor != null) {
            messageProcessor.processPeerRequest(handler, request);
            return;
        }

        log.warn("No hay MessageProcessor disponible — request P2P '{}' de peer {} ignorado", request.getAction(), handler.getPeerId());
    }

    public void handleRetransmitRequest(IPeerHandler handler, DTORequest request) {
        if (messageProcessor != null) {
            messageProcessor.handleRetransmitRequest(handler, request);
            return;
        }
        log.warn("handleRetransmitRequest(): MessageProcessor no disponible — solicitud ignorada");
    }

    public void handleSyncRequest(IPeerHandler handler, DTORequest request) {
        if (messageProcessor != null) {
            messageProcessor.handleSyncRequest(handler, request);
            return;
        }
        log.warn("handleSyncRequest(): MessageProcessor no disponible — solicitud ignorada");
    }

    public boolean sendToPeer(UUID peerId, String message) {
        if (communicator != null) {
            return communicator.sendToPeer(peerId, message);
        }
        log.warn("sendToPeer(): PeerCommunicator no disponible — no se puede enviar mensaje a {}", peerId);
        return false;
    }

    public void markPeerAsOfflineAfterFailure(UUID peerId) {
        if (lifecycleManager != null) {
            lifecycleManager.markPeerAsOfflineAfterFailure(peerId);
            return;
        }
        log.warn("markPeerAsOfflineAfterFailure(): No hay LifecycleManager — se requiere un componente para marcar peers como offline: {}", peerId);
    }

    public void registerDiscoveredPeers(Object payload) {
        if (peerRegistry != null) {
            peerRegistry.registerDiscoveredPeers(payload);
            return;
        }
        log.warn("registerDiscoveredPeers(): PeerRegistry no disponible — payload ignorado");
    }

    public void broadcastToAllPeers(String message) {
        if (communicator != null) {
            communicator.broadcastToAllPeers(message);
            return;
        }
        log.warn("broadcastToAllPeers(): PeerCommunicator no disponible — broadcast omitido");
    }

    public boolean isConnectedToPeer(UUID peerId) {
        if (communicator != null) return communicator.isConnectedToPeer(peerId);
        if (outgoingManager != null) return outgoingManager.isConnectedToPeer(peerId);
        return false;
    }

    public Set<UUID> getConnectedPeerIds() {
        if (communicator != null) return communicator.getConnectedPeerIds();
        return Collections.emptySet();
    }

    public Map<String, Object> getLocalPeerInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("peerId", localPeerId != null ? localPeerId.toString() : null);
        info.put("port", peerPort);
        info.put("clientPort", clientPort);
        info.put("timestamp", System.currentTimeMillis());

        try {
            String ip = networkUtils != null ? networkUtils.getServerIPAddress() : null;
            if (ip == null || ip.isEmpty()) ip = "localhost";
            info.put("ip", ip);
        } catch (Exception e) {
            info.put("ip", "localhost");
        }

        info.put("puerto", peerPort);

        return info;
    }

    public UUID getLocalPeerId() {
        return localPeerId;
    }

    public int getActivePeerCount() {
        if (communicator != null) return communicator.getActivePeerCount();
        return 0;
    }

    @EventListener
    public void handleRetransmitToOriginEvent(RetransmitToOriginPeerEvent event) {
        log.info("Retransmitiendo respuesta a peer origen: {}", event.getOriginPeerId());
        DTOResponse response = new DTOResponse(
                "retransmit_response",
                "success",
                "Respuesta retransmitida",
                event.getResponse()
        );
        sendToPeer(event.getOriginPeerId(), gson.toJson(response));
    }

    @PreDestroy
    public void shutdown() {
        log.info("Cerrando PeerConnectionManager (delegador)...");
        if (outgoingManager != null) {
            try { outgoingManager.disconnectAll(); } catch (Exception ignored) {}
        }

        if (maintenanceService != null) {
            try { maintenanceService.stop(); } catch (Exception ignored) {}
        }

        if (peerServer != null) {
            try { peerServer.stop(); } catch (Exception ignored) {}
        }

        log.info("PeerConnectionManager cerrado correctamente (delegador)");
    }
}
