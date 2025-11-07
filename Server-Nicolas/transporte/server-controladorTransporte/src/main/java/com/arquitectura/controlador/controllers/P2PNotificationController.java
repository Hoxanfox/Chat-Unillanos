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
        "obtenerinfocanal",
        "iniciartransferenciaaudio",
        "recibirchunkaudio",
        "finalizartransferenciaaudio"
    );

    private final com.arquitectura.logicaPeers.AudioFileP2PService audioFileP2PService;

    @Autowired
    public P2PNotificationController(IChatFachada chatFachada, Gson gson,
                                     com.arquitectura.logicaPeers.AudioFileP2PService audioFileP2PService) {
        super(chatFachada, gson);
        this.audioFileP2PService = audioFileP2PService;
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
            case "iniciartransferenciaaudio":
                handleIniciarTransferenciaAudio(request, handler);
                break;
            case "recibirchunkaudio":
                handleRecibirChunkAudio(request, handler);
                break;
            case "finalizartransferenciaaudio":
                handleFinalizarTransferenciaAudio(request, handler);
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

            // ✅ CORREGIDO: Guardar el mensaje en la BD local
            try {
                chatFachada.guardarMensajeRemoto(
                    UUID.fromString(messageId),
                    UUID.fromString(channelId),
                    UUID.fromString(authorId),
                    content,
                    messageType,
                    java.time.LocalDateTime.parse(timestamp)
                );
                System.out.println("✓ Mensaje guardado en BD local");
            } catch (Exception e) {
                System.err.println("⚠ No se pudo guardar mensaje en BD: " + e.getMessage());
                // Continuar para notificar a usuarios conectados
            }

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

            // ✅ CORREGIDO: Obtener SOLO los miembros LOCALES del canal específico
            List<UUID> memberIds = chatFachada.obtenerMiembrosLocalesDelCanal(UUID.fromString(channelId));

            if (memberIds.isEmpty()) {
                System.out.println("⚠ No hay miembros locales en este canal");
            } else {
                System.out.println("→ Notificando a " + memberIds.size() + " miembros locales");
                
                eventPublisher.publishEvent(new com.arquitectura.events.NewMessageEvent(
                    this, messageDto, memberIds));
            }

            System.out.println("✓ [P2PNotificationController] Mensaje procesado correctamente");

            sendJsonResponse(handler, "notificarMensaje", true,
                "Mensaje recibido y procesado", null);

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
                UserResponseDto user = usuarioOpt.get();

                Map<String, Object> userData = new HashMap<>();
                userData.put("userId", user.getUserId().toString());
                userData.put("username", user.getUsername());
                userData.put("email", user.getEmail());
                userData.put("photoAddress", user.getPhotoAddress());

                // ✅ NUEVO: Incluir la foto en Base64 si existe
                if (user.getImagenBase64() != null && !user.getImagenBase64().isEmpty()) {
                    userData.put("imagenBase64", user.getImagenBase64());
                    System.out.println("  → Incluyendo foto de perfil en Base64");
                } else {
                    userData.put("imagenBase64", null);
                }

                if (user.getFechaRegistro() != null) {
                    userData.put("fechaRegistro", user.getFechaRegistro().toString());
                }
                if (user.getEstado() != null) {
                    userData.put("estado", user.getEstado());
                }

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

    /**
     * Maneja el inicio de una transferencia de audio desde otro peer.
     */
    private void handleIniciarTransferenciaAudio(DTORequest request, IClientHandler handler) {
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();

            String transferId = payload.get("transferId").getAsString();
            String fileName = payload.get("fileName").getAsString();
            long fileSize = payload.get("fileSize").getAsLong();
            int totalChunks = payload.get("totalChunks").getAsInt();
            String originalPath = payload.get("originalPath").getAsString();

            System.out.println("→ [P2PNotificationController] Iniciando recepción de audio:");
            System.out.println("  Transfer ID: " + transferId);
            System.out.println("  Archivo: " + fileName);
            System.out.println("  Tamaño: " + fileSize + " bytes");
            System.out.println("  Total chunks: " + totalChunks);

            boolean exito = audioFileP2PService.iniciarRecepcionAudio(
                transferId, fileName, fileSize, totalChunks, originalPath);

            if (exito) {
                System.out.println("✓ [P2PNotificationController] Recepción de audio iniciada");
                sendJsonResponse(handler, "iniciarTransferenciaAudio", true,
                    "Recepción iniciada", null);
            } else {
                System.err.println("✗ [P2PNotificationController] Error al iniciar recepción");
                sendJsonResponse(handler, "iniciarTransferenciaAudio", false,
                    "Error al iniciar recepción", null);
            }

        } catch (Exception e) {
            System.err.println("✗ [P2PNotificationController] Error al iniciar transferencia de audio: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "iniciarTransferenciaAudio", false,
                "Error al iniciar transferencia: " + e.getMessage(), null);
        }
    }

    /**
     * Maneja la recepción de un chunk de audio.
     */
    private void handleRecibirChunkAudio(DTORequest request, IClientHandler handler) {
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();

            String transferId = payload.get("transferId").getAsString();
            int chunkNumber = payload.get("chunkNumber").getAsInt();
            String chunkData = payload.get("chunkData").getAsString();

            boolean exito = audioFileP2PService.recibirChunkAudio(transferId, chunkNumber, chunkData);

            if (exito) {
                sendJsonResponse(handler, "recibirChunkAudio", true,
                    "Chunk recibido", null);
            } else {
                System.err.println("✗ [P2PNotificationController] Error al recibir chunk");
                sendJsonResponse(handler, "recibirChunkAudio", false,
                    "Error al recibir chunk", null);
            }

        } catch (Exception e) {
            System.err.println("✗ [P2PNotificationController] Error al recibir chunk de audio: " + e.getMessage());
            sendJsonResponse(handler, "recibirChunkAudio", false,
                "Error al recibir chunk: " + e.getMessage(), null);
        }
    }

    /**
     * Maneja la finalización de la transferencia de audio.
     */
    private void handleFinalizarTransferenciaAudio(DTORequest request, IClientHandler handler) {
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();

            String transferId = payload.get("transferId").getAsString();

            System.out.println("→ [P2PNotificationController] Finalizando recepción de audio: " + transferId);

            String rutaGuardada = audioFileP2PService.finalizarRecepcionAudio(transferId);

            if (rutaGuardada != null) {
                System.out.println("✓ [P2PNotificationController] Archivo guardado en: " + rutaGuardada);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("filePath", rutaGuardada);

                sendJsonResponse(handler, "finalizarTransferenciaAudio", true,
                    "Transferencia completada", responseData);
            } else {
                System.err.println("✗ [P2PNotificationController] Error al finalizar transferencia");
                sendJsonResponse(handler, "finalizarTransferenciaAudio", false,
                    "Error al ensamblar archivo", null);
            }

        } catch (Exception e) {
            System.err.println("✗ [P2PNotificationController] Error al finalizar transferencia de audio: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "finalizarTransferenciaAudio", false,
                "Error al finalizar transferencia: " + e.getMessage(), null);
        }
    }
}
