package com.arquitectura.controlador.peer.handlers;

import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.controlador.IClientHandler;
import com.google.gson.Gson;
import org.springframework.stereotype.Component;

/**
 * Helper para enviar respuestas consistentes desde los handlers P2P
 */
@Component
public class PeerResponseHelper {

    private final Gson gson;

    public PeerResponseHelper(Gson gson) {
        this.gson = gson;
    }

    /**
     * Envía una respuesta de éxito
     */
    public void sendSuccess(IClientHandler handler, String action, String message, Object data) {
        sendResponse(handler, action, "success", message, data);
    }

    /**
     * Envía una respuesta de error
     */
    public void sendError(IClientHandler handler, String action, String message, Object data) {
        sendResponse(handler, action, "error", message, data);
    }

    /**
     * Envía una respuesta JSON al cliente
     */
    private void sendResponse(IClientHandler handler, String action, String status, String message, Object data) {
        DTOResponse response = new DTOResponse(action, status, message, data);
        String jsonResponse = gson.toJson(response);
        handler.sendMessage(jsonResponse);
    }
}

