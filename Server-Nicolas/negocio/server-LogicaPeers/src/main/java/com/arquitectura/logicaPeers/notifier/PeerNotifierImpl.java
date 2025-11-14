package com.arquitectura.logicaPeers.notifier;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.logicaPeers.transfer.FileTransferService;
import com.arquitectura.domain.Peer;
import com.arquitectura.persistence.repository.PeerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PeerNotifierImpl implements PeerNotifier {

    private final PeerRepository peerRepository;
    private final com.arquitectura.utils.p2p.PeerConnectionPool peerConnectionPool;

    @Autowired
    public PeerNotifierImpl(PeerRepository peerRepository,
                            com.arquitectura.utils.p2p.PeerConnectionPool peerConnectionPool) {
        this.peerRepository = peerRepository;
        this.peerConnectionPool = peerConnectionPool;
    }

    @Override
    public void notificarCambioUsuarioATodosLosPeers(UUID usuarioId, String username, String nuevoEstado, UUID peerId, String peerIp, Integer peerPuerto) {
        System.out.println("🔔 [PeerNotifier] Notificando cambio de usuario a todos los peers: " + username + " -> " + nuevoEstado);

        try {
            // Obtener todos los peers disponibles (no solo activos) porque usamos conexiones efímeras
            List<com.arquitectura.domain.Peer> peers = peerRepository.findAll();
            java.util.Map<UUID, String[]> peersParaBroadcast = new java.util.HashMap<>();

            UUID peerLocalId = null; // no tenemos acceso directo al peer local aquí
            for (com.arquitectura.domain.Peer peer : peers) {
                if (peer.getPeerId() != null && !peer.getPeerId().equals(peerLocalId)) {
                    peersParaBroadcast.put(
                        peer.getPeerId(),
                        new String[]{peer.getIp(), String.valueOf(peer.getPuerto())}
                    );
                }
            }

            if (peersParaBroadcast.isEmpty()) {
                System.out.println("ℹ [PeerNotifier] No hay peers remotos disponibles para notificar");
                return;
            }

            System.out.println("→ [PeerNotifier] Enviando notificación PUSH a " + peersParaBroadcast.size() + " peers remotos");

            // Preparar la notificación PUSH
            java.util.Map<String, Object> notificationData = new java.util.HashMap<>();
            notificationData.put("usuarioId", usuarioId.toString());
            notificationData.put("username", username);
            notificationData.put("nuevoEstado", nuevoEstado);
            notificationData.put("peerId", peerId != null ? peerId.toString() : null);
            notificationData.put("peerIp", peerIp);
            notificationData.put("peerPuerto", peerPuerto);
            notificationData.put("timestamp", LocalDateTime.now().toString());

            DTORequest notificationRequest = new DTORequest(
                "notificacionCambioUsuario",
                notificationData
            );

            // Enviar broadcast a todos los peers
            java.util.Map<String, java.util.concurrent.Future<DTOResponse>> futures =
                peerConnectionPool.broadcast(peersParaBroadcast, notificationRequest);

            int exitosos = 0;
            int fallidos = 0;

            for (java.util.Map.Entry<String, java.util.concurrent.Future<DTOResponse>> entry : futures.entrySet()) {
                try {
                    DTOResponse response = entry.getValue().get(2, java.util.concurrent.TimeUnit.SECONDS);
                    if ("success".equals(response.getStatus())) {
                        exitosos++;
                    } else {
                        fallidos++;
                        System.out.println("⚠ [PeerNotifier] Peer " + entry.getKey() + " respondió con error: " + response.getMessage());
                    }
                } catch (java.util.concurrent.TimeoutException e) {
                    fallidos++;
                    System.out.println("⚠ [PeerNotifier] Timeout al notificar peer " + entry.getKey());
                } catch (Exception e) {
                    fallidos++;
                    System.out.println("⚠ [PeerNotifier] Error al notificar peer " + entry.getKey() + ": " + e.getMessage());
                }
            }

            System.out.println("✓ [PeerNotifier] Notificación completada: " + exitosos + " exitosas, " + fallidos + " fallidas");

        } catch (Exception e) {
            System.err.println("✗ [PeerNotifier] Error al enviar notificaciones push: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

