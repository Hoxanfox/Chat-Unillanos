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
        if (!heartbeatEnabled) {
            return;
        }
        
        try {
            System.out.println("→ [HeartbeatService] Iniciando envío de heartbeats...");
            
            // Obtener peer actual
            Peer peerActual = peerService.obtenerPeerActual();
            if (peerActual == null) {
                System.out.println("⚠ [HeartbeatService] No se pudo obtener información del peer actual");
                return;
            }
            
            // Obtener lista de peers activos (excluyendo el actual)
            List<PeerResponseDto> peersActivos = peerService.listarPeersActivos();
            
            if (peersActivos.isEmpty()) {
                System.out.println("ℹ [HeartbeatService] No hay peers activos para enviar heartbeat");
                return;
            }
            
            System.out.println("→ [HeartbeatService] Enviando heartbeat a " + peersActivos.size() + " peers");
            
            int exitosos = 0;
            int fallidos = 0;
            
            // Enviar heartbeat a cada peer
            for (PeerResponseDto peer : peersActivos) {
                // No enviar heartbeat a sí mismo
                if (peer.getPeerId().equals(peerActual.getPeerId())) {
                    continue;
                }
                
                try {
                    // Crear petición de heartbeat
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("peerId", peerActual.getPeerId().toString());
                    payload.put("ip", peerActual.getIp());
                    payload.put("puerto", peerActual.getPuerto());
                    
                    DTORequest heartbeatRequest = new DTORequest("reportarLatido", payload);
                    
                    // Enviar heartbeat usando el cliente P2P
                    peerService.retransmitirPeticion(peer.getPeerId(), heartbeatRequest);
                    
                    exitosos++;
                    System.out.println("  ✓ Heartbeat enviado a peer: " + peer.getPeerId() + 
                        " (" + peer.getIp() + ":" + peer.getPuerto() + ")");
                    
                } catch (Exception e) {
                    fallidos++;
                    System.err.println("  ✗ Error al enviar heartbeat a peer " + peer.getPeerId() + 
                        ": " + e.getMessage());
                    
                    // El peer será marcado como OFFLINE por el servicio de retransmisión
                }
            }
            
            System.out.println("✓ [HeartbeatService] Heartbeats enviados: " + exitosos + 
                " exitosos, " + fallidos + " fallidos");
            
        } catch (Exception e) {
            System.err.println("✗ [HeartbeatService] Error en envío de heartbeats: " + e.getMessage());
            e.printStackTrace();
        }
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
