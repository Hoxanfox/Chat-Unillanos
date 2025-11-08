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
 * 
 * Funcionalidades:
 * - Envía heartbeats periódicos a todos los peers conocidos
 * - Verifica peers inactivos y los marca como OFFLINE
 * - Mantiene la red P2P sincronizada
 */
@Service
public class HeartbeatService {
    
    private final IPeerService peerService;
    private boolean heartbeatEnabled = true;
    
    @Autowired
    public HeartbeatService(@Qualifier("peerServiceP2P") IPeerService peerService) {
        this.peerService = peerService;
        System.out.println("✓ [HeartbeatService] Servicio de heartbeat inicializado");
    }

    /**
     * Envía heartbeats a todos los peers activos en la red.
     * Se ejecuta cada 30 segundos.
     */
    @Scheduled(fixedRate = 30000) // Cada 30 segundos
    public void enviarHeartbeats() {

        // ==================================================================
        // SOLUCIÓN: Comenta todo este método.
        // Esta lógica de "heartbeat activo" está causando los Timeouts
        // al crear conexiones no autenticadas.
        // El PeerConnectionManager ya maneja heartbeats pasivos.
        // ==================================================================

        /*
        if (!heartbeatEnabled) {
            return;
        }

        try {
            System.out.println("→ [HeartbeatService] Iniciando envío de heartbeats...");

            // ... (el resto de tu código)...

            System.out.println("✓ [HeartbeatService] Heartbeats enviados: " + exitosos +
                " exitosos, " + fallidos + " fallidos");

        } catch (Exception e) {
            System.err.println("✗ [HeartbeatService] Error en envío de heartbeats: " + e.getMessage());
            e.printStackTrace();
        }
        */
    }
    
    /**
     * Verifica qué peers han excedido el timeout de heartbeat
     * y los marca como OFFLINE.
     * Se ejecuta cada 60 segundos.
     */
    @Scheduled(fixedRate = 60000) // Cada 60 segundos
    public void verificarPeersInactivos() {
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
            
            // Mostrar estadísticas
            long totalPeers = peerService.contarTotalPeers();
            long peersActivos = peerService.contarPeersActivos();
            long peersOffline = peerService.contarPeersInactivos();
            
            System.out.println("ℹ [HeartbeatService] Estadísticas de red P2P:");
            System.out.println("  - Total de peers: " + totalPeers);
            System.out.println("  - Peers activos: " + peersActivos);
            System.out.println("  - Peers offline: " + peersOffline);
            
        } catch (Exception e) {
            System.err.println("✗ [HeartbeatService] Error al verificar peers inactivos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Habilita el envío automático de heartbeats.
     */
    public void habilitarHeartbeat() {
        this.heartbeatEnabled = true;
        System.out.println("✓ [HeartbeatService] Heartbeat automático habilitado");
    }
    
    /**
     * Deshabilita el envío automático de heartbeats.
     * Útil para testing o mantenimiento.
     */
    public void deshabilitarHeartbeat() {
        this.heartbeatEnabled = false;
        System.out.println("⚠ [HeartbeatService] Heartbeat automático deshabilitado");
    }
    
    /**
     * Verifica si el heartbeat automático está habilitado.
     * 
     * @return true si está habilitado, false en caso contrario
     */
    public boolean isHeartbeatEnabled() {
        return heartbeatEnabled;
    }
    
    /**
     * Fuerza el envío inmediato de heartbeats sin esperar el schedule.
     * Útil para testing o sincronización manual.
     */
    public void forzarEnvioHeartbeats() {
        System.out.println("→ [HeartbeatService] Forzando envío inmediato de heartbeats...");
        enviarHeartbeats();
    }
    
    /**
     * Fuerza la verificación inmediata de peers inactivos sin esperar el schedule.
     * Útil para testing o limpieza manual.
     */
    public void forzarVerificacionPeers() {
        System.out.println("→ [HeartbeatService] Forzando verificación inmediata de peers...");
        verificarPeersInactivos();
    }
}
