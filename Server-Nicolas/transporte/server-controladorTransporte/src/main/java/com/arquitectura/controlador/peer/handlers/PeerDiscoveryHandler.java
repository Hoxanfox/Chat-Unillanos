package com.arquitectura.controlador.peer.handlers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Handler especializado para operaciones de descubrimiento de peers
 */
@Component
public class PeerDiscoveryHandler {

    private final IChatFachada chatFachada;
    private final Gson gson;
    private final PeerResponseHelper responseHelper;

    public PeerDiscoveryHandler(IChatFachada chatFachada, Gson gson, PeerResponseHelper responseHelper) {
        this.chatFachada = chatFachada;
        this.gson = gson;
        this.responseHelper = responseHelper;
    }

    public void handleDescubrirPeers(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerDiscoveryHandler] Procesando descubrirPeers");

        if (!validatePayload(request.getPayload(), handler)) {
            return;
        }

        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            PeerConnectionData connectionData = extractConnectionData(payload, handler);
            if (connectionData == null) return;

            UUID peerSolicitanteId = registerOrUpdatePeer(connectionData, payload);
            List<Map<String, Object>> peersDisponibles = getActivePeersExcluding(peerSolicitanteId);
            Map<String, Object> responseData = buildDiscoveryResponse(peerSolicitanteId, peersDisponibles, connectionData.isNuevo);

            String mensaje = connectionData.isNuevo ? "Peer registrado y peers descubiertos" : "Peers descubiertos exitosamente";
            System.out.println("✓ [PeerDiscoveryHandler] Descubrimiento completado: " + peersDisponibles.size() + " peers disponibles");
            responseHelper.sendSuccess(handler, "descubrirPeers", mensaje, responseData);

        } catch (Exception e) {
            System.err.println("✗ [PeerDiscoveryHandler] Error al descubrir peers: " + e.getMessage());
            responseHelper.sendError(handler, "descubrirPeers", "Error al descubrir peers: " + e.getMessage(), null);
        }
    }

    public void handleListarPeersDisponibles(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerDiscoveryHandler] Procesando listarPeersDisponibles");

        try {
            List<PeerResponseDto> peers = chatFachada.p2p().listarPeersDisponibles();
            List<Map<String, Object>> peersData = convertPeersToMapList(peers);

            System.out.println("✓ [PeerDiscoveryHandler] Lista de peers obtenida: " + peers.size() + " peers");
            responseHelper.sendSuccess(handler, "listarPeersDisponibles", "Lista de peers y su estado obtenida", peersData);

        } catch (Exception e) {
            System.err.println("✗ [PeerDiscoveryHandler] Error al listar peers: " + e.getMessage());
            responseHelper.sendError(handler, "listarPeersDisponibles", "Error al obtener la lista de peers", null);
        }
    }

    private PeerConnectionData extractConnectionData(JsonObject payload, IClientHandler handler) {
        if (!payload.has("ip") || !payload.has("puerto")) {
            responseHelper.sendError(handler, "descubrirPeers", "Datos del peer inválidos", Map.of("campo", "ip/puerto", "motivo", "Campos requeridos"));
            return null;
        }

        String ip = payload.get("ip").getAsString();
        int puerto = payload.get("puerto").getAsInt();

        if (ip == null || ip.trim().isEmpty()) {
            responseHelper.sendError(handler, "descubrirPeers", "Datos del peer inválidos", Map.of("campo", "ip", "motivo", "Formato de IP inválido"));
            return null;
        }

        if (puerto <= 0 || puerto > 65535) {
            responseHelper.sendError(handler, "descubrirPeers", "Puerto inválido", Map.of("campo", "puerto", "motivo", "El puerto debe estar entre 1 y 65535"));
            return null;
        }

        return new PeerConnectionData(ip, puerto);
    }

    private UUID registerOrUpdatePeer(PeerConnectionData connectionData, JsonObject payload) throws Exception {
        UUID peerSolicitanteId = null;

        if (payload.has("peerId") && !payload.get("peerId").isJsonNull()) {
            try {
                String peerIdStr = payload.get("peerId").getAsString();
                peerSolicitanteId = UUID.fromString(peerIdStr);
                chatFachada.p2p().reportarLatido(peerSolicitanteId, connectionData.ip, connectionData.puerto);
                System.out.println("→ [PeerDiscoveryHandler] Peer existente actualizado: " + peerSolicitanteId);
            } catch (IllegalArgumentException e) {
                System.out.println("⚠ [PeerDiscoveryHandler] PeerId inválido, se creará uno nuevo");
                peerSolicitanteId = null;
            }
        }

        if (peerSolicitanteId == null) {
            PeerResponseDto nuevoPeer = chatFachada.p2p().agregarPeer(connectionData.ip, connectionData.puerto);
            peerSolicitanteId = nuevoPeer.getPeerId();
            connectionData.isNuevo = true;
            System.out.println("→ [PeerDiscoveryHandler] Nuevo peer registrado: " + peerSolicitanteId);
        }

        return peerSolicitanteId;
    }

    private List<Map<String, Object>> getActivePeersExcluding(UUID excludePeerId) {
        List<PeerResponseDto> todosLosPeers = chatFachada.p2p().listarPeersActivos();
        List<Map<String, Object>> peersDisponibles = new ArrayList<>();

        for (PeerResponseDto peer : todosLosPeers) {
            if (!peer.getPeerId().equals(excludePeerId)) {
                peersDisponibles.add(convertPeerToMap(peer));
            }
        }

        return peersDisponibles;
    }

    private Map<String, Object> buildDiscoveryResponse(UUID peerId, List<Map<String, Object>> peers, boolean isNuevo) {
        Map<String, Object> peerSolicitanteInfo = new HashMap<>();
        peerSolicitanteInfo.put("peerId", peerId.toString());
        peerSolicitanteInfo.put("registrado", true);
        if (isNuevo) {
            peerSolicitanteInfo.put("esNuevo", true);
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("peersDisponibles", peers);
        responseData.put("totalPeers", peers.size());
        responseData.put("peerSolicitante", peerSolicitanteInfo);

        return responseData;
    }

    private List<Map<String, Object>> convertPeersToMapList(List<PeerResponseDto> peers) {
        List<Map<String, Object>> peersData = new ArrayList<>();
        for (PeerResponseDto peer : peers) {
            peersData.add(convertPeerToMap(peer));
        }
        return peersData;
    }

    private Map<String, Object> convertPeerToMap(PeerResponseDto peer) {
        Map<String, Object> peerMap = new HashMap<>();
        peerMap.put("peerId", peer.getPeerId().toString());
        peerMap.put("ip", peer.getIp());
        peerMap.put("puerto", peer.getPuerto());
        peerMap.put("conectado", peer.getConectado());
        return peerMap;
    }

    private boolean validatePayload(Object payload, IClientHandler handler) {
        if (payload == null) {
            responseHelper.sendError(handler, "descubrirPeers", "Falta payload", null);
            return false;
        }
        return true;
    }

    private static class PeerConnectionData {
        String ip;
        int puerto;
        boolean isNuevo = false;

        PeerConnectionData(String ip, int puerto) {
            this.ip = ip;
            this.puerto = puerto;
        }
    }
}

