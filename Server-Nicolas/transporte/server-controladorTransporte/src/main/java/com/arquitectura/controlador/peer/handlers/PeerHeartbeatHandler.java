package com.arquitectura.controlador.peer.handlers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler especializado para operaciones de heartbeat de peers
 */
@Component
public class PeerHeartbeatHandler {

    private final IChatFachada chatFachada;
    private final Gson gson;
    private final PeerResponseHelper responseHelper;

    public PeerHeartbeatHandler(IChatFachada chatFachada, Gson gson, PeerResponseHelper responseHelper) {
        this.chatFachada = chatFachada;
        this.gson = gson;
        this.responseHelper = responseHelper;
    }

    public void handleReportarLatido(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerHeartbeatHandler] Procesando reportarLatido");

        if (!validatePayload(request.getPayload(), handler)) {
            return;
        }

        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            UUID peerId = extractAndValidatePeerId(payload, handler);
            if (peerId == null) return;

            reportHeartbeat(peerId, payload);
            long intervaloHeartbeat = chatFachada.p2p().obtenerIntervaloHeartbeat();

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("proximoLatidoMs", intervaloHeartbeat);

            System.out.println("✓ [PeerHeartbeatHandler] Latido reportado para peer: " + peerId);
            responseHelper.sendSuccess(handler, "reportarLatido", "Latido recibido", responseData);

        } catch (Exception e) {
            System.err.println("✗ [PeerHeartbeatHandler] Error al reportar latido: " + e.getMessage());
            handleHeartbeatError(e, handler);
        }
    }

    public void handleVerificarConexion(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerHeartbeatHandler] Procesando verificarConexion/ping");

        try {
            UUID peerActualId = chatFachada.p2p().obtenerPeerActualId();

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("peerId", peerActualId.toString());
            responseData.put("timestamp", System.currentTimeMillis());
            responseData.put("estado", "ACTIVO");

            responseHelper.sendSuccess(handler, "verificarConexion", "Conexión verificada", responseData);

        } catch (Exception e) {
            System.err.println("✗ [PeerHeartbeatHandler] Error al verificar conexión: " + e.getMessage());
            responseHelper.sendError(handler, "verificarConexion", "Error al verificar conexión", null);
        }
    }

    private UUID extractAndValidatePeerId(JsonObject payload, IClientHandler handler) {
        if (!payload.has("peerId")) {
            responseHelper.sendError(handler, "reportarLatido", "Peer no reconocido o no registrado", Map.of("peerId", "DESCONOCIDO"));
            return null;
        }

        String peerIdStr = payload.get("peerId").getAsString();

        try {
            return UUID.fromString(peerIdStr);
        } catch (IllegalArgumentException e) {
            responseHelper.sendError(handler, "reportarLatido", "Peer no reconocido o no registrado", Map.of("peerId", peerIdStr));
            return null;
        }
    }

    private void reportHeartbeat(UUID peerId, JsonObject payload) throws Exception {
        if (payload.has("ip") && payload.has("puerto")) {
            String ip = payload.get("ip").getAsString();
            int puerto = payload.get("puerto").getAsInt();
            chatFachada.p2p().reportarLatido(peerId, ip, puerto);
        } else {
            chatFachada.p2p().reportarLatido(peerId);
        }
    }

    private void handleHeartbeatError(Exception e, IClientHandler handler) {
        if (e.getMessage() != null && (e.getMessage().contains("no encontrado") || e.getMessage().contains("no existe"))) {
            responseHelper.sendError(handler, "reportarLatido", "Peer no reconocido o no registrado", Map.of("peerId", "DESCONOCIDO"));
        } else {
            responseHelper.sendError(handler, "reportarLatido", "Error al reportar latido", null);
        }
    }

    private boolean validatePayload(Object payload, IClientHandler handler) {
        if (payload == null) {
            responseHelper.sendError(handler, "reportarLatido", "Falta payload", null);
            return false;
        }
        return true;
    }
}

