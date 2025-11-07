package com.arquitectura.logicaPeers;

import com.arquitectura.domain.Peer;
import com.arquitectura.persistence.repository.PeerRepository;
import com.arquitectura.utils.network.NetworkUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para mapear usuarios a sus peers correspondientes.
 * Mantiene un registro de qué usuarios pertenecen a qué servidor.
 */
@Service
public class UserPeerMappingService {

    private final PeerRepository peerRepository;
    private final NetworkUtils networkUtils;

    // Mapa: UUID de usuario -> UUID de peer al que pertenece
    private final Map<UUID, UUID> userToPeerMap = new ConcurrentHashMap<>();

    // Mapa: UUID de peer -> Set de UUIDs de usuarios que le pertenecen
    private final Map<UUID, Set<UUID>> peerToUsersMap = new ConcurrentHashMap<>();

    @Autowired
    public UserPeerMappingService(PeerRepository peerRepository, NetworkUtils networkUtils) {
        this.peerRepository = peerRepository;
        this.networkUtils = networkUtils;
        System.out.println("✓ [UserPeerMappingService] Servicio de mapeo Usuario-Peer inicializado");
    }

    /**
     * Registra que un usuario pertenece a un peer específico.
     */
    public void registerUserToPeer(UUID userId, UUID peerId) {
        if (userId == null || peerId == null) {
            return;
        }

        userToPeerMap.put(userId, peerId);
        peerToUsersMap.computeIfAbsent(peerId, k -> ConcurrentHashMap.newKeySet()).add(userId);

        System.out.println("✓ [UserPeerMapping] Usuario " + userId + " registrado en peer " + peerId);
    }

    /**
     * Registra múltiples usuarios para un peer.
     */
    public void registerUsersToPeer(Set<UUID> userIds, UUID peerId) {
        if (userIds == null || peerId == null) {
            return;
        }

        for (UUID userId : userIds) {
            registerUserToPeer(userId, peerId);
        }

        System.out.println("✓ [UserPeerMapping] " + userIds.size() + " usuarios registrados en peer " + peerId);
    }

    /**
     * Obtiene el peer al que pertenece un usuario.
     * @return Optional con el UUID del peer, o vacío si no se encuentra
     */
    public Optional<UUID> getPeerForUser(UUID userId) {
        return Optional.ofNullable(userToPeerMap.get(userId));
    }

    /**
     * Obtiene todos los usuarios que pertenecen a un peer.
     */
    public Set<UUID> getUsersForPeer(UUID peerId) {
        return new HashSet<>(peerToUsersMap.getOrDefault(peerId, Collections.emptySet()));
    }

    /**
     * Verifica si un usuario es local (pertenece a este servidor).
     */
    public boolean isLocalUser(UUID userId) {
        try {
            String serverIp = networkUtils.getServerIPAddress();
            Optional<Peer> localPeer = peerRepository.findByIp(serverIp);

            if (localPeer.isEmpty()) {
                // Si no hay peer local registrado, asumimos que es local
                return true;
            }

            Optional<UUID> userPeerId = getPeerForUser(userId);

            // Si no está mapeado, asumimos que es local
            if (userPeerId.isEmpty()) {
                return true;
            }

            // Comparar con el peer local
            return userPeerId.get().equals(localPeer.get().getPeerId());

        } catch (Exception e) {
            System.err.println("✗ [UserPeerMapping] Error al verificar si usuario es local: " + e.getMessage());
            return true; // En caso de error, asumimos local por seguridad
        }
    }

    /**
     * Verifica si un usuario es remoto (pertenece a otro servidor).
     */
    public boolean isRemoteUser(UUID userId) {
        return !isLocalUser(userId);
    }

    /**
     * Elimina un usuario del mapeo.
     */
    public void removeUser(UUID userId) {
        UUID peerId = userToPeerMap.remove(userId);
        if (peerId != null) {
            Set<UUID> users = peerToUsersMap.get(peerId);
            if (users != null) {
                users.remove(userId);
            }
        }
    }

    /**
     * Elimina todos los usuarios de un peer.
     */
    public void removeAllUsersFromPeer(UUID peerId) {
        Set<UUID> users = peerToUsersMap.remove(peerId);
        if (users != null) {
            for (UUID userId : users) {
                userToPeerMap.remove(userId);
            }
        }
        System.out.println("✓ [UserPeerMapping] Eliminados todos los usuarios del peer " + peerId);
    }

    /**
     * Limpia todo el mapeo (útil para reiniciar).
     */
    public void clearAll() {
        userToPeerMap.clear();
        peerToUsersMap.clear();
        System.out.println("✓ [UserPeerMapping] Mapeo limpiado completamente");
    }

    /**
     * Obtiene estadísticas del mapeo.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsuarios", userToPeerMap.size());
        stats.put("totalPeers", peerToUsersMap.size());

        Map<String, Integer> usuariosPorPeer = new HashMap<>();
        peerToUsersMap.forEach((peerId, users) ->
            usuariosPorPeer.put(peerId.toString(), users.size())
        );
        stats.put("usuariosPorPeer", usuariosPorPeer);

        return stats;
    }

    /**
     * Imprime el estado actual del mapeo (para debugging).
     */
    public void printMappingState() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           ESTADO DEL MAPEO USUARIO-PEER                   ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║ Total Usuarios: " + userToPeerMap.size());
        System.out.println("║ Total Peers:    " + peerToUsersMap.size());
        System.out.println("╠════════════════════════════════════════════════════════════╣");

        peerToUsersMap.forEach((peerId, users) -> {
            System.out.println("║ Peer " + peerId + ": " + users.size() + " usuarios");
        });

        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }
}

