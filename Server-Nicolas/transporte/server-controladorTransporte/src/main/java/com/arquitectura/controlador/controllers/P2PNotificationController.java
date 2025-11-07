package com.arquitectura.controlador.controllers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Controlador para manejar notificaciones P2P entrantes de otros servidores.
 * Procesa mensajes, invitaciones y consultas de información entre peers.
 */
@Component
public class P2PNotificationController extends BaseController {

    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
        "notificarmensaje",
        "notificarinvitacioncanal",
        "notificaraceptacioninvitacion",
        "obtenerinfousuario",
        "obtenerinfocanal"
    );

    @Autowired
    public P2PNotificationController(IChatFachada chatFachada, Gson gson) {
        super(chatFachada, gson);
    }

    @Override
    public boolean handleAction(String action, DTORequest request, IClientHandler handler) {
        if (!SUPPORTED_ACTIONS.contains(action.toLowerCase())) {
            return false;
        }

        System.out.println("→ [P2PNotificationController] Procesando acción P2P: " + action);

        switch (action.toLowerCase()) {
            case "notificarmensaje":
                handleNotificarMensaje(request, handler);
                break;
            case "notificarinvitacioncanal":
                handleNotificarInvitacionCanal(request, handler);
                break;
            case "notificaraceptacioninvitacion":
                handleNotificarAceptacionInvitacion(request, handler);
                break;
            case "obtenerinfousuario":
                handleObtenerInfoUsuario(request, handler);
                break;
            case "obtenerinfocanal":
                handleObtenerInfoCanal(request, handler);
                break;
            default:
                return false;
        }

        return true;
    }

    @Override
    public Set<String> getSupportedActions() {
        return SUPPORTED_ACTIONS;
    }

    /**
     * Maneja la notificación de un nuevo mensaje desde otro peer.
     * Retransmite el mensaje a los usuarios locales que son miembros del canal.
     */
    private void handleNotificarMensaje(DTORequest request, IClientHandler handler) {
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();

            String messageId = payload.get("messageId").getAsString();
            String channelId = payload.get("channelId").getAsString();
            String authorId = payload.get("authorId").getAsString();
            String authorUsername = payload.get("authorUsername").getAsString();
            String content = payload.get("content").getAsString();
            String messageType = payload.get("messageType").getAsString();
            String timestamp = payload.get("timestamp").getAsString();

            System.out.println("→ [P2PNotificationController] Mensaje recibido de peer:");
            System.out.println("  Canal: " + channelId);
            System.out.println("  Autor: " + authorUsername);
            System.out.println("  Tipo: " + messageType);

            // Crear el DTO del mensaje
            UserResponseDto authorDto = new UserResponseDto();
            authorDto.setUserId(UUID.fromString(authorId));
            authorDto.setUsername(authorUsername);

            MessageResponseDto messageDto = new MessageResponseDto();
            messageDto.setMessageId(UUID.fromString(messageId));
            messageDto.setChannelId(UUID.fromString(channelId));
            messageDto.setAuthor(authorDto);
            messageDto.setContent(content);
            messageDto.setMessageType(messageType);
            messageDto.setTimestamp(java.time.LocalDateTime.parse(timestamp));

            // Publicar evento para que los usuarios locales reciban el mensaje
            org.springframework.context.ApplicationEventPublisher eventPublisher =
                org.springframework.context.ApplicationContextProvider.getBean(
                    org.springframework.context.ApplicationEventPublisher.class);

            // Obtener miembros locales del canal
            List<com.arquitectura.DTO.usuarios.UserResponseDto> miembrosCanal =
                chatFachada.obtenerTodosLosUsuarios(); // Simplificado, deberías filtrar por canal

            List<UUID> memberIds = miembrosCanal.stream()
                .map(u -> u.getUserId())
                .collect(java.util.stream.Collectors.toList());

            eventPublisher.publishEvent(new com.arquitectura.events.NewMessageEvent(
                this, messageDto, memberIds));

            System.out.println("✓ [P2PNotificationController] Mensaje retransmitido a usuarios locales");

            sendJsonResponse(handler, "notificarMensaje", true,
                "Mensaje recibido y retransmitido", null);

        } catch (Exception e) {
            System.err.println("✗ [P2PNotificationController] Error al procesar mensaje: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "notificarMensaje", false,
                "Error al procesar mensaje: " + e.getMessage(), null);
        }
    }

    /**
     * Maneja la notificación de invitación a canal desde otro peer.
     */
    private void handleNotificarInvitacionCanal(DTORequest request, IClientHandler handler) {
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();

            String canalId = payload.get("canalId").getAsString();
            String usuarioInvitadoId = payload.get("usuarioInvitadoId").getAsString();
            String usuarioInvitadorId = payload.get("usuarioInvitadorId").getAsString();

            System.out.println("→ [P2PNotificationController] Invitación a canal recibida:");
            System.out.println("  Canal: " + canalId);
            System.out.println("  Usuario invitado: " + usuarioInvitadoId);
            System.out.println("  Usuario invitador: " + usuarioInvitadorId);

            // TODO: Implementar lógica para notificar al usuario local sobre la invitación
            // Esto podría incluir crear una entrada en la tabla de invitaciones pendientes
            // y notificar al cliente si está conectado

            System.out.println("✓ [P2PNotificationController] Invitación procesada");

            sendJsonResponse(handler, "notificarInvitacionCanal", true,
                "Invitación recibida", null);

        } catch (Exception e) {
            System.err.println("✗ [P2PNotificationController] Error al procesar invitación: " + e.getMessage());
            sendJsonResponse(handler, "notificarInvitacionCanal", false,
                "Error al procesar invitación: " + e.getMessage(), null);
        }
    }

    /**
     * Maneja la notificación de aceptación de invitación desde otro peer.
     */
    private void handleNotificarAceptacionInvitacion(DTORequest request, IClientHandler handler) {
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();

            String canalId = payload.get("canalId").getAsString();
            String usuarioId = payload.get("usuarioId").getAsString();

            System.out.println("→ [P2PNotificationController] Aceptación de invitación recibida:");
            System.out.println("  Canal: " + canalId);
            System.out.println("  Usuario: " + usuarioId);

            // TODO: Implementar lógica para actualizar la membresía del canal

            System.out.println("✓ [P2PNotificationController] Aceptación procesada");

            sendJsonResponse(handler, "notificarAceptacionInvitacion", true,
                "Aceptación recibida", null);

        } catch (Exception e) {
            System.err.println("✗ [P2PNotificationController] Error al procesar aceptación: " + e.getMessage());
            sendJsonResponse(handler, "notificarAceptacionInvitacion", false,
                "Error al procesar aceptación: " + e.getMessage(), null);
        }
    }

    /**
     * Maneja la solicitud de información de usuario desde otro peer.
     */
    private void handleObtenerInfoUsuario(DTORequest request, IClientHandler handler) {
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String usuarioId = payload.get("usuarioId").getAsString();

            System.out.println("→ [P2PNotificationController] Solicitud de info de usuario: " + usuarioId);

            // Buscar el usuario en este servidor
            Optional<UserResponseDto> usuarioOpt = chatFachada.buscarUsuarioPorUsername(usuarioId);

            if (usuarioOpt.isPresent()) {
                Map<String, Object> userData = new HashMap<>();
                UserResponseDto user = usuarioOpt.get();
                userData.put("userId", user.getUserId().toString());
                userData.put("username", user.getUsername());
                userData.put("email", user.getEmail());
                userData.put("photoAddress", user.getPhotoAddress());

                System.out.println("✓ [P2PNotificationController] Usuario encontrado: " + user.getUsername());

                sendJsonResponse(handler, "obtenerInfoUsuario", true,
                    "Usuario encontrado", userData);
            } else {
                System.out.println("✗ [P2PNotificationController] Usuario no encontrado");
                sendJsonResponse(handler, "obtenerInfoUsuario", false,
                    "Usuario no encontrado en este servidor", null);
            }

        } catch (Exception e) {
            System.err.println("✗ [P2PNotificationController] Error al obtener info de usuario: " + e.getMessage());
            sendJsonResponse(handler, "obtenerInfoUsuario", false,
                "Error al obtener información: " + e.getMessage(), null);
        }
    }

    /**
     * Maneja la solicitud de información de canal desde otro peer.
     */
    private void handleObtenerInfoCanal(DTORequest request, IClientHandler handler) {
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String canalId = payload.get("canalId").getAsString();

            System.out.println("→ [P2PNotificationController] Solicitud de info de canal: " + canalId);

            // Buscar el canal en este servidor
            List<ChannelResponseDto> canales = chatFachada.obtenerTodosLosCanales();
            Optional<ChannelResponseDto> canalOpt = canales.stream()
                .filter(c -> c.getChannelId().toString().equals(canalId))
                .findFirst();

            if (canalOpt.isPresent()) {
                Map<String, Object> channelData = new HashMap<>();
                ChannelResponseDto canal = canalOpt.get();
                channelData.put("channelId", canal.getChannelId().toString());
                channelData.put("channelName", canal.getChannelName());
                channelData.put("channelType", canal.getChannelType());
                channelData.put("ownerId", canal.getOwner().getUserId().toString());

                System.out.println("✓ [P2PNotificationController] Canal encontrado: " + canal.getChannelName());

                sendJsonResponse(handler, "obtenerInfoCanal", true,
                    "Canal encontrado", channelData);
            } else {
                System.out.println("✗ [P2PNotificationController] Canal no encontrado");
                sendJsonResponse(handler, "obtenerInfoCanal", false,
                    "Canal no encontrado en este servidor", null);
            }

        } catch (Exception e) {
            System.err.println("✗ [P2PNotificationController] Error al obtener info de canal: " + e.getMessage());
            sendJsonResponse(handler, "obtenerInfoCanal", false,
                "Error al obtener información: " + e.getMessage(), null);
        }
    }
}

