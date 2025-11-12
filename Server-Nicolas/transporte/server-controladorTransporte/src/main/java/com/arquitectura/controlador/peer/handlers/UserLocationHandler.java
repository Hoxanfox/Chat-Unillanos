package com.arquitectura.controlador.peer.handlers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Handler para búsqueda y enrutamiento de usuarios
 */
@Component
public class UserLocationHandler {

    private final IChatFachada chatFachada;
    private final Gson gson;
    private final PeerResponseHelper responseHelper;

    public UserLocationHandler(IChatFachada chatFachada, Gson gson, PeerResponseHelper responseHelper) {
        this.chatFachada = chatFachada;
        this.gson = gson;
        this.responseHelper = responseHelper;
    }

    public void handleBuscarUsuario(DTORequest request, IClientHandler handler) {
        System.out.println("→ [UserLocationHandler] Procesando buscarUsuario");

        if (!validatePayload(request.getPayload(), handler)) return;

        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            UUID usuarioId = extractAndValidateUserId(payload, handler, "buscarUsuario");
            if (usuarioId == null) return;

            com.arquitectura.DTO.p2p.UserLocationResponseDto userLocation = chatFachada.p2p().buscarUsuario(usuarioId);
            Map<String, Object> responseData = buildUserLocationResponse(userLocation);

            System.out.println("✓ [UserLocationHandler] Usuario encontrado");
            responseHelper.sendSuccess(handler, "buscarUsuario", "Usuario encontrado exitosamente", responseData);

        } catch (Exception e) {
            System.err.println("✗ [UserLocationHandler] Error: " + e.getMessage());
            handleUserNotFoundError(e, handler, "buscarUsuario");
        }
    }

    public void handleEnrutarMensaje(DTORequest request, IClientHandler handler) {
        System.out.println("→ [UserLocationHandler] Procesando enrutarMensaje");

        if (!validatePayload(request.getPayload(), handler)) return;

        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            MessageRoutingData data = extractMessageRoutingData(payload, handler);
            if (data == null) return;

            com.arquitectura.DTO.p2p.UserLocationResponseDto userLocation = chatFachada.p2p().buscarUsuario(data.destinatarioId);

            if (!userLocation.isConectado() || userLocation.getPeerId() == null) {
                sendMessageNotDeliveredResponse(handler, userLocation, "Usuario no está conectado");
                return;
            }

            Map<String, Object> mensajeData = new HashMap<>();
            mensajeData.put("remitenteId", data.remitenteId.toString());
            mensajeData.put("destinatarioId", data.destinatarioId.toString());
            mensajeData.put("contenido", data.contenido);
            mensajeData.put("tipo", data.tipo);

            com.arquitectura.DTO.Comunicacion.DTORequest peticionMensaje =
                new com.arquitectura.DTO.Comunicacion.DTORequest("recibirMensajeDirecto", mensajeData);

            com.arquitectura.DTO.Comunicacion.DTOResponse respuestaPeer =
                chatFachada.p2p().retransmitirPeticion(userLocation.getPeerId(), peticionMensaje);

            if ("success".equals(respuestaPeer.getStatus())) {
                sendMessageDeliveredResponse(handler, userLocation);
            } else {
                sendMessageNotDeliveredResponse(handler, userLocation, "Peer destinatario no disponible");
            }

        } catch (Exception e) {
            System.err.println("✗ [UserLocationHandler] Error: " + e.getMessage());
            handleMessageRoutingError(e, handler);
        }
    }

    private UUID extractAndValidateUserId(JsonObject payload, IClientHandler handler, String action) {
        if (!payload.has("usuarioId")) {
            responseHelper.sendError(handler, action, "El ID del usuario es requerido",
                Map.of("campo", "usuarioId", "motivo", "Campo requerido"));
            return null;
        }

        try {
            return UUID.fromString(payload.get("usuarioId").getAsString());
        } catch (IllegalArgumentException e) {
            responseHelper.sendError(handler, action, "Formato de UUID inválido",
                Map.of("campo", "usuarioId", "motivo", "Formato UUID inválido"));
            return null;
        }
    }

    private Map<String, Object> buildUserLocationResponse(com.arquitectura.DTO.p2p.UserLocationResponseDto userLocation) {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("usuarioId", userLocation.getUsuarioId().toString());
        responseData.put("username", userLocation.getUsername());
        responseData.put("conectado", userLocation.isConectado());

        if (userLocation.getPeerId() != null) {
            responseData.put("peerId", userLocation.getPeerId().toString());
            responseData.put("peerIp", userLocation.getPeerIp());
            responseData.put("peerPuerto", userLocation.getPeerPuerto());
        } else {
            responseData.put("peerId", null);
            responseData.put("peerIp", null);
            responseData.put("peerPuerto", null);
        }

        return responseData;
    }

    private MessageRoutingData extractMessageRoutingData(JsonObject payload, IClientHandler handler) {
        if (!payload.has("remitenteId") || !payload.has("destinatarioId") || !payload.has("contenido")) {
            responseHelper.sendError(handler, "enrutarMensaje", "Faltan campos requeridos",
                Map.of("campo", "remitenteId/destinatarioId/contenido", "motivo", "Campos requeridos"));
            return null;
        }

        try {
            UUID remitenteId = UUID.fromString(payload.get("remitenteId").getAsString());
            UUID destinatarioId = UUID.fromString(payload.get("destinatarioId").getAsString());
            String contenido = payload.get("contenido").getAsString();
            String tipo = payload.has("tipo") ? payload.get("tipo").getAsString() : "texto";
            return new MessageRoutingData(remitenteId, destinatarioId, contenido, tipo);
        } catch (IllegalArgumentException e) {
            responseHelper.sendError(handler, "enrutarMensaje", "Formato de UUID inválido",
                Map.of("campo", "remitenteId/destinatarioId", "motivo", "Formato UUID inválido"));
            return null;
        }
    }

    private void sendMessageDeliveredResponse(IClientHandler handler,
                                             com.arquitectura.DTO.p2p.UserLocationResponseDto userLocation) {
        String fechaEntrega = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Map<String, Object> successData = new HashMap<>();
        successData.put("destinatarioId", userLocation.getUsuarioId().toString());
        successData.put("destinatarioUsername", userLocation.getUsername());
        successData.put("entregado", true);
        successData.put("fechaEntrega", fechaEntrega);
        successData.put("motivo", "Mensaje entregado exitosamente");

        responseHelper.sendSuccess(handler, "enrutarMensaje", "Mensaje enrutado exitosamente", successData);
    }

    private void sendMessageNotDeliveredResponse(IClientHandler handler,
                                                 com.arquitectura.DTO.p2p.UserLocationResponseDto userLocation,
                                                 String motivo) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("destinatarioId", userLocation.getUsuarioId().toString());
        errorData.put("destinatarioUsername", userLocation.getUsername());
        errorData.put("entregado", false);
        errorData.put("fechaEntrega", null);
        errorData.put("motivo", motivo);

        responseHelper.sendError(handler, "enrutarMensaje", "No se pudo entregar el mensaje", errorData);
    }

    private void handleUserNotFoundError(Exception e, IClientHandler handler, String action) {
        if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
            responseHelper.sendError(handler, action, "Usuario no encontrado", Map.of("usuarioId", "DESCONOCIDO"));
        } else {
            responseHelper.sendError(handler, action, "Error al buscar usuario", null);
        }
    }

    private void handleMessageRoutingError(Exception e, IClientHandler handler) {
        if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("destinatarioId", "DESCONOCIDO");
            errorData.put("destinatarioUsername", null);
            errorData.put("entregado", false);
            errorData.put("fechaEntrega", null);
            errorData.put("motivo", "Usuario no encontrado en el sistema");

            responseHelper.sendError(handler, "enrutarMensaje", "Usuario destinatario no encontrado", errorData);
        } else {
            responseHelper.sendError(handler, "enrutarMensaje", "Error al enrutar mensaje: " + e.getMessage(), null);
        }
    }

    private boolean validatePayload(Object payload, IClientHandler handler) {
        if (payload == null) {
            responseHelper.sendError(handler, "enrutarMensaje", "Falta payload", null);
            return false;
        }
        return true;
    }

    private static class MessageRoutingData {
        UUID remitenteId;
        UUID destinatarioId;
        String contenido;
        String tipo;

        MessageRoutingData(UUID remitenteId, UUID destinatarioId, String contenido, String tipo) {
            this.remitenteId = remitenteId;
            this.destinatarioId = destinatarioId;
            this.contenido = contenido;
            this.tipo = tipo;
        }
    }
}

