package com.arquitectura.controlador.peer.handlers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler especializado para operaciones de enrutamiento P2P
 */
@Component
public class PeerRoutingHandler {

    private final IChatFachada chatFachada;
    private final Gson gson;
    private final PeerResponseHelper responseHelper;

    public PeerRoutingHandler(IChatFachada chatFachada, Gson gson, PeerResponseHelper responseHelper) {
        this.chatFachada = chatFachada;
        this.gson = gson;
        this.responseHelper = responseHelper;
    }

    /**
     * Maneja la acción de retransmitir una petición a otro peer
     * FASE 1: CARTERO PURO - Diseño Limpio
     */
    public void handleRetransmitirPeticion(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerRoutingHandler] Procesando retransmitirPeticion (Fase 1: Cartero Puro)");

        if (!validatePayload(request.getPayload(), handler)) {
            return;
        }

        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();

            // Validar campos requeridos
            if (!payload.has("peerDestinoId") || !payload.has("peticionCliente")) {
                responseHelper.sendError(handler, "retransmitirPeticion",
                    "Faltan campos requeridos: peerDestinoId y peticionCliente", null);
                return;
            }

            // Extraer datos de enrutamiento
            RetransmissionData data = extractRetransmissionData(payload);

            System.out.println("🚚 [PeerRoutingHandler] Cartero: Entregando paquete");
            System.out.println("   ├─ Origen: " + data.peerOrigenInfo);
            System.out.println("   ├─ Destino: " + data.peerDestinoId);
            System.out.println("   └─ Acción: " + data.peticionCliente.getAction());

            // Enviar al peer remoto
            DTOResponse respuestaPeer = chatFachada.p2p().retransmitirPeticion(
                data.peerDestinoId, data.peticionCliente);

            // Preparar respuesta
            Map<String, Object> responseData = buildRetransmissionResponse(respuestaPeer);

            System.out.println("✓ [PeerRoutingHandler] Cartero: Paquete entregado y respuesta recibida");
            responseHelper.sendSuccess(handler, "retransmitirPeticion",
                "Petición del cliente procesada exitosamente.", responseData);

        } catch (IllegalArgumentException e) {
            System.err.println("✗ [PeerRoutingHandler] Error de validación: " + e.getMessage());
            responseHelper.sendError(handler, "retransmitirPeticion",
                "Error de validación: " + e.getMessage(), null);

        } catch (Exception e) {
            System.err.println("✗ [PeerRoutingHandler] Error al retransmitir petición: " + e.getMessage());
            e.printStackTrace();

            // Devolver error dentro de respuestaCliente
            Map<String, Object> errorResponse = buildErrorRetransmissionResponse(e);
            responseHelper.sendSuccess(handler, "retransmitirPeticion",
                "Petición del cliente procesada, pero resultó en un error.", errorResponse);
        }
    }

    private RetransmissionData extractRetransmissionData(JsonObject payload) {
        RetransmissionData data = new RetransmissionData();

        // Extraer destino
        String peerDestinoIdStr = payload.get("peerDestinoId").getAsString();
        data.peerDestinoId = UUID.fromString(peerDestinoIdStr);

        // Extraer información del origen (opcional)
        data.peerOrigenInfo = "DESCONOCIDO";
        if (payload.has("peerOrigen")) {
            JsonObject peerOrigenJson = payload.get("peerOrigen").getAsJsonObject();
            data.peerOrigenInfo = peerOrigenJson.has("nombreServidor") ?
                peerOrigenJson.get("nombreServidor").getAsString() :
                peerOrigenJson.get("peerId").getAsString();
        }

        // Extraer petición del cliente
        JsonObject peticionClienteJson = payload.get("peticionCliente").getAsJsonObject();
        data.peticionCliente = gson.fromJson(peticionClienteJson, DTORequest.class);

        // Validar petición
        if (data.peticionCliente.getAction() == null || data.peticionCliente.getAction().trim().isEmpty()) {
            throw new IllegalArgumentException("La petición del cliente debe tener una acción válida");
        }

        return data;
    }

    private Map<String, Object> buildRetransmissionResponse(DTOResponse respuestaPeer) {
        Map<String, Object> respuestaCliente = new HashMap<>();
        respuestaCliente.put("action", respuestaPeer.getAction());
        respuestaCliente.put("status", respuestaPeer.getStatus());
        respuestaCliente.put("message", respuestaPeer.getMessage());
        respuestaCliente.put("data", respuestaPeer.getData() != null ? respuestaPeer.getData() : null);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("respuestaCliente", respuestaCliente);

        return responseData;
    }

    private Map<String, Object> buildErrorRetransmissionResponse(Exception e) {
        Map<String, Object> respuestaCliente = new HashMap<>();
        respuestaCliente.put("status", "error");
        respuestaCliente.put("message", e.getMessage() != null ? e.getMessage() : "Error al procesar petición");
        respuestaCliente.put("data", null);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("respuestaCliente", respuestaCliente);

        return responseData;
    }

    private boolean validatePayload(Object payload, IClientHandler handler) {
        if (payload == null) {
            responseHelper.sendError(handler, "retransmitirPeticion", "Falta payload", null);
            return false;
        }
        return true;
    }

    private static class RetransmissionData {
        UUID peerDestinoId;
        String peerOrigenInfo;
        DTORequest peticionCliente;
    }
}
