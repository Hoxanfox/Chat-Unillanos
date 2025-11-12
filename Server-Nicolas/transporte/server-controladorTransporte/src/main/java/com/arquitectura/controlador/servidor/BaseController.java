package com.arquitectura.controlador.servidor;

import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.controlador.IController;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase base abstracta para todos los controladores.
 * Proporciona métodos auxiliares comunes.
 */
public abstract class BaseController implements IController {
    
    protected final IChatFachada chatFachada;
    protected final Gson gson;
    
    public BaseController(IChatFachada chatFachada, Gson gson) {
        this.chatFachada = chatFachada;
        this.gson = gson;
    }
    
    /**
     * Envía una respuesta JSON al cliente
     */
    protected void sendJsonResponse(IClientHandler handler, String action, boolean success, String message, Object data) {
        String status = success ? "success" : "error";
        DTOResponse response = new DTOResponse(action, status, message, data);
        String jsonResponse = gson.toJson(response);
        System.out.println("→ [BaseController] Enviando respuesta JSON:");
        System.out.println("  Action: " + action);
        System.out.println("  Status: " + status);
        System.out.println("  Message: " + message);
        System.out.println("  JSON: " + jsonResponse);
        handler.sendMessage(jsonResponse);
        System.out.println("✓ [BaseController] Mensaje enviado al handler");
    }
    
    /**
     * Crea un mapa de datos de error estándar
     */
    protected Map<String, String> createErrorData(String campo, String motivo) {
        Map<String, String> errorData = new HashMap<>();
        errorData.put("campo", campo);
        errorData.put("motivo", motivo);
        return errorData;
    }
    
    /**
     * Valida que el payload no sea nulo
     */
    protected boolean validatePayload(Object payload, IClientHandler handler, String action) {
        if (payload == null) {
            sendJsonResponse(handler, action, false, "Falta payload", null);
            return false;
        }
        return true;
    }
}
