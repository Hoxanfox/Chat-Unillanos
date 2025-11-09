package com.arquitectura.controlador;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.Mensajes.MessageResponseDto;

import com.arquitectura.controlador.controllers.*;

import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * RequestDispatcher refactorizado que delega las acciones a controladores especializados.
 * Cada controlador maneja un dominio específico (usuarios, canales, mensajes, archivos).
 */
@Component
public class RequestDispatcher {

    private final IChatFachada chatFachada;
    private final Gson gson;

    private final List<IController> controllers;
    
    private static final Set<String> ACCIONES_PUBLICAS = Set.of(
            "authenticateuser",
            "registeruser",
            "uploadfileforregistration",
            "uploadfilechunk",
            "endfileupload"

    );
    private IContactListBroadcaster contactListBroadcaster;

    @Autowired
    public RequestDispatcher(
            IChatFachada chatFachada, 
            Gson gson,
            UserController userController,
            ChannelController channelController,
            MessageController messageController,
            FileController fileController,
            PeerController peerController) {
        this.chatFachada = chatFachada;
        this.gson = gson;
        
        // Registrar controladores en orden de prioridad
        this.controllers = Arrays.asList(
            userController,
            channelController,
            messageController,
            fileController,
            peerController
        );
    }

    /**
     * Permite inyectar el broadcaster para evitar dependencias circulares
     */
    @Autowired
    public void setContactListBroadcaster(IContactListBroadcaster broadcaster) {
        this.contactListBroadcaster = broadcaster;
    }

    public void dispatch(String requestJson, IClientHandler handler) {
        DTORequest request;
        String action = "unknown";
        try {
            request = gson.fromJson(requestJson, DTORequest.class);
            action = request.getAction() != null ? request.getAction().toLowerCase() : "unknown";

            // Validar sesión
            if (!ACCIONES_PUBLICAS.contains(action) && !handler.isAuthenticated()) {
                sendJsonResponse(handler, action, false, "Debes iniciar sesión para realizar esta acción.", null);
                return;
            }

            // Delegar a los controladores
            boolean handled = false;
            for (IController controller : controllers) {
                if (controller.handleAction(action, request, handler)) {
                    handled = true;
                    break;
                }
            }

            if (!handled) {
                sendJsonResponse(handler, action, false, "Comando desconocido: " + action, null);
            }

        } catch (Exception e) {
            System.err.println("Error en dispatch: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, action, false, "Error interno del servidor", null);
        }
    }

    /**
     * Enriquece mensajes salientes convirtiendo archivos de audio a Base64
     */
    public MessageResponseDto enrichOutgoingMessage(MessageResponseDto originalDto) {
        if ("AUDIO".equals(originalDto.getMessageType())) {
            try {
                String base64Content = chatFachada.archivos().getFileAsBase64(originalDto.getContent());

                return new MessageResponseDto(
                        originalDto.getMessageId(),
                        originalDto.getChannelId(),
                        originalDto.getAuthor(),
                        originalDto.getTimestamp(),
                        originalDto.getMessageType(),
                        base64Content
                );
            } catch (Exception e) {
                System.err.println("Error al leer y codificar el archivo de audio para propagación: " + e.getMessage());
                return originalDto;
            }
        }
        return originalDto;
    }

    /**
     * Método auxiliar para enviar respuestas JSON
     */
    private void sendJsonResponse(IClientHandler handler, String action, boolean success, String message, Object data) {
        String status = success ? "success" : "error";
        DTOResponse response = new DTOResponse(action, status, message, data);
        String jsonResponse = gson.toJson(response);
        handler.sendMessage(jsonResponse);
    }
}
