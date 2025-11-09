package com.arquitectura.logicaPeers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.domain.Peer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de heartbeat automático para la red P2P.
 * (¡¡DESHABILITADO PARA EVITAR CONFLICTOS CON PeerConnectionManager!!)
 */
@Service
public class HeartbeatService {

    private final IPeerService peerService;
    private boolean heartbeatEnabled = true;

    @Autowired
    public HeartbeatService(@Qualifier("peerServiceP2P") IPeerService peerService) {
        this.peerService = peerService;
        System.out.println("✓ [HeartbeatService] Servicio de heartbeat inicializado (TAREAS PROGRAMADAS DESHABILITADAS)");
    }

    /**
     * Envía heartbeats a todos los peers activos en la red.
     * Se ejecuta cada 30 segundos.
     */
    // ==================================================================
    // DESHABILITADO - PeerConnectionManager lo maneja
    // ==================================================================
    //@Scheduled(fixedRate = 30000)
    public void enviarHeartbeats() {
        /*
        if (!heartbeatEnabled) {
            return;
        }
        // ...
        */
    }

    /**
     * Verifica qué peers han excedido el timeout de heartbeat
     * y los marca como OFFLINE.
     * Se ejecuta cada 60 segundos.
     */
    // ==================================================================
    // DESHABILITADO - PeerConnectionManager lo maneja
    // ==================================================================
    //@Scheduled(fixedRate = 60000)
    public void verificarPeersInactivos() {
        /*
        if (!heartbeatEnabled) {
            return;
        }

        try {
            System.out.println("→ [HeartbeatService] Verificando peers inactivos...");

            int peersInactivos = peerService.verificarPeersInactivos();

            if (peersInactivos > 0) {
                System.out.println("⚠ [HeartbeatService] " + peersInactivos +
                    " peer(s) marcado(s) como OFFLINE por timeout");
            } else {
                System.out.println("✓ [HeartbeatService] Todos los peers están activos");
            }

            long totalPeers = peerService.contarTotalPeers();
            long peersActivos = peerService.contarPeersActivos();
            long peersOffline = peerService.contarPeersInactivos();

            System.out.println("ℹ [HeartmbeatService] Estadísticas de red P2P:");
            System.out.println("  - Total de peers: " + totalPeers);
            System.out.println("  - Peers activos: " + peersActivos);
            System.out.println("  - Peers offline: " + peersOffline);

        } catch (Exception e) {
            System.err.println("✗ [HeartbeatService] Error al verificar peers inactivos: " + e.getMessage());
            e.printStackTrace();
        }
        */
    }

    // ... (el resto de los métodos pueden quedarse como están) ...

    public void habilitarHeartbeat() {
        this.heartbeatEnabled = true;
        System.out.println("✓ [HeartbeatService] Heartbeat automático habilitado");
    }

    public void deshabilitarHeartbeat() {
        this.heartbeatEnabled = false;
        System.out.println("⚠ [HeartbeatService] Heartbeat automático deshabilitado");
    }

    public boolean isHeartbeatEnabled() {
        return heartbeatEnabled;
    }

    public void forzarEnvioHeartbeats() {
        System.out.println("→ [HeartMbeatService] Forzando envío inmediato de heartbeats...");
        enviarHeartbeats();
    }

    public void forzarVerificacionPeers() {
        System.out.println("→ [HeartbeatService] Forzando verificación inmediata de peers...");
        verificarPeersInactivos();
    }
}