package com.arquitectura.transporte.peer.maintenance;

import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.controlador.peer.IPeerHandler;
import com.arquitectura.logicaPeers.IPeerService;
import com.arquitectura.transporte.peer.PeerConnectionManager;
import com.arquitectura.transporte.peer.PeerOutgoingConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Servicio que agrupa y ejecuta las tareas periódicas de mantenimiento P2P:
 * - checkHeartbeats
 * - attemptReconnections
 * - syncWithDatabase
 */
public class MaintenanceService {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);

    private final IPeerService peerService;
    private final PeerConnectionManager manager;
    private final Map<UUID, IPeerHandler> activePeerConnections;
    private final Map<UUID, PeerOutgoingConnection> outgoingConnections;
    private final long heartbeatIntervalMs;
    private final long heartbeatTimeoutSeconds;
    private final long reconnectDelayMs;

    private ScheduledExecutorService scheduled;

    public MaintenanceService(IPeerService peerService,
                              PeerConnectionManager manager,
                              Map<UUID, IPeerHandler> activePeerConnections,
                              Map<UUID, PeerOutgoingConnection> outgoingConnections,
                              long heartbeatIntervalMs,
                              long heartbeatTimeoutSeconds,
                              long reconnectDelayMs) {
        this.peerService = peerService;
        this.manager = manager;
        this.activePeerConnections = activePeerConnections;
        this.outgoingConnections = outgoingConnections;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        this.reconnectDelayMs = reconnectDelayMs;
    }

    public void start() {
        if (scheduled != null && !scheduled.isShutdown()) return;
        scheduled = Executors.newScheduledThreadPool(3);

        scheduled.scheduleAtFixedRate(this::checkHeartbeats,
                heartbeatIntervalMs,
                heartbeatIntervalMs,
                TimeUnit.MILLISECONDS);

        scheduled.scheduleAtFixedRate(this::attemptReconnections,
                reconnectDelayMs * 2,
                reconnectDelayMs * 2,
                TimeUnit.MILLISECONDS);

        scheduled.scheduleAtFixedRate(this::syncWithDatabase,
                30000,
                60000,
                TimeUnit.MILLISECONDS);

        log.info("MaintenanceService started: heartbeats={}, reconnectDelayMs={}", heartbeatIntervalMs, reconnectDelayMs);
    }

    public void stop() {
        if (scheduled == null) return;
        scheduled.shutdown();
        try {
            if (!scheduled.awaitTermination(3, TimeUnit.SECONDS)) scheduled.shutdownNow();
        } catch (InterruptedException e) {
            scheduled.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("MaintenanceService stopped");
    }

    /**
     * Implementación trasladada desde PeerConnectionManager.checkHeartbeats
     */
    private void checkHeartbeats() {
        long now = System.currentTimeMillis();
        long timeoutMs = heartbeatTimeoutSeconds * 1000;

        List<UUID> peersToRemove = new ArrayList<>();

        for (Map.Entry<UUID, IPeerHandler> entry : activePeerConnections.entrySet()) {
            IPeerHandler handler = entry.getValue();
            long timeSinceLastHeartbeat = now - handler.getLastHeartbeat();

            if (timeSinceLastHeartbeat > timeoutMs) {
                log.warn("Peer {} sin heartbeat por {} ms. Desconectando...",
                        entry.getKey(), timeSinceLastHeartbeat);
                peersToRemove.add(entry.getKey());
            }
        }

        for (UUID peerId : peersToRemove) {
            IPeerHandler handler = activePeerConnections.get(peerId);
            if (handler != null) {
                handler.forceDisconnect();
            }
        }

        if (!peersToRemove.isEmpty()) {
            log.info("Verificación de heartbeats: {} peers desconectados", peersToRemove.size());
        }
    }

    /**
     * Implementación trasladada desde PeerConnectionManager.attemptReconnections
     */
    private void attemptReconnections() {
        List<PeerResponseDto> allPeers = peerService.listarPeersDisponibles();

        List<PeerResponseDto> offlinePeers = allPeers.stream()
                .filter(peer -> !peer.getPeerId().equals(manager.getLocalPeerId()))
                .filter(peer -> "ONLINE".equals(peer.getConectado()))
                .filter(peer -> !manager.isConnectedToPeer(peer.getPeerId()))
                .toList();

        if (!offlinePeers.isEmpty()) {
            log.debug("Intentando reconectar a {} peers ONLINE desconectados", offlinePeers.size());

            for (PeerResponseDto peer : offlinePeers) {
                manager.connectToPeer(peer.getPeerId(), peer.getIp(), peer.getPuerto());
            }
        }
    }

    /**
     * Implementación trasladada desde PeerConnectionManager.syncWithDatabase
     */
    private void syncWithDatabase() {
        try {
            log.debug("━━━━━━━━━━━ INICIANDO SINCRONIZACIÓN CON BD ━━━━━━━━━━━");

            Set<UUID> connectedPeerIds = manager.getConnectedPeerIds();
            log.debug("Peers conectados en memoria: {}", connectedPeerIds.size());

            connectedPeerIds.add(manager.getLocalPeerId());

            Set<UUID> peersToRemove = new HashSet<>();

            log.debug("Reportando latidos para {} peers...", connectedPeerIds.size());
            for (UUID peerId : connectedPeerIds) {
                try {
                    peerService.reportarLatido(peerId);
                    log.trace("  ✓ Latido reportado para peer: {}", peerId);
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("Peer no encontrado")) {
                        log.warn("  ✗ Peer {} NO EXISTE EN BD - Marcado para limpieza", peerId);
                        log.debug("     └─ Mensaje de error: {}", e.getMessage());
                        peersToRemove.add(peerId);
                    } else {
                        log.warn("  ⚠ Error actualizando latido de peer {}: {}", peerId, e.getMessage());
                    }
                }
            }

            if (!peersToRemove.isEmpty()) {
                log.info("━━━━━━━━━━━ LIMPIANDO {} PEERS INEXISTENTES ━━━━━━━━━━━", peersToRemove.size());

                for (UUID peerId : peersToRemove) {
                    log.info("Limpiando peer inexistente: {}", peerId);

                    IPeerHandler handler = activePeerConnections.remove(peerId);
                    if (handler != null) {
                        try {
                            log.debug("  ├─ Cerrando conexión ENTRANTE...");
                            handler.disconnect();
                            log.info("  ├─ ✓ Conexión entrante cerrada y removida");
                        } catch (Exception e) {
                            log.debug("  ├─ ⚠ Error cerrando conexión entrante: {}", e.getMessage());
                        }
                    } else {
                        log.debug("  ├─ No hay conexión entrante activa");
                    }

                    PeerOutgoingConnection outgoing = outgoingConnections.remove(peerId);
                    if (outgoing != null) {
                        try {
                            log.debug("  ├─ Cerrando conexión SALIENTE...");
                            outgoing.disconnect();
                            log.info("  └─ ✓ Conexión saliente cerrada y removida");
                        } catch (Exception e) {
                            log.debug("  └─ ⚠ Error cerrando conexión saliente: {}", e.getMessage());
                        }
                    } else {
                        log.debug("  └─ No hay conexión saliente activa");
                    }

                    log.info("Peer inexistente {} completamente removido de memoria", peerId);
                }

                log.info("━━━━━━━━━━━ LIMPIEZA COMPLETADA ━━━━━━━━━━━");
            } else {
                log.trace("No hay peers inexistentes para limpiar");
            }

            log.debug("Verificando peers en BD para marcar OFFLINE...");
            List<PeerResponseDto> allPeers = peerService.listarPeersDisponibles();
            log.debug("Total peers en BD: {}", allPeers.size());

            int offlineCount = 0;
            for (PeerResponseDto peer : allPeers) {
                if (peer.getPeerId().equals(manager.getLocalPeerId())) {
                    log.trace("  - Peer local {} - SKIP", peer.getPeerId());
                    continue;
                }

                if (!connectedPeerIds.contains(peer.getPeerId())) {
                    if ("ONLINE".equals(peer.getConectado())) {
                        log.debug("  - Peer {} está ONLINE en BD pero desconectado - marcando OFFLINE", peer.getPeerId());
                        peerService.marcarPeerComoDesconectado(peer.getPeerId());
                        log.info("Peer {} marcado como OFFLINE en BD", peer.getPeerId());
                        offlineCount++;
                    } else {
                        log.trace("  - Peer {} ya está OFFLINE", peer.getPeerId());
                    }
                } else {
                    log.trace("  - Peer {} está conectado y ONLINE", peer.getPeerId());
                }
            }

            if (offlineCount > 0) {
                log.info("Total peers marcados como OFFLINE: {}", offlineCount);
            }

            log.debug("━━━━━━━━━━━ SINCRONIZACIÓN COMPLETADA ━━━━━━━━━━━");
            log.info("Resumen: {} peers conectados, {} limpiados, {} marcados offline",
                    connectedPeerIds.size() - 1, peersToRemove.size(), offlineCount);

        } catch (Exception e) {
            log.error("━━━━━━━━━━━ ERROR EN SINCRONIZACIÓN ━━━━━━━━━━━");
            log.error("Error sincronizando con base de datos: {}", e.getMessage(), e);
        }
    }
}

