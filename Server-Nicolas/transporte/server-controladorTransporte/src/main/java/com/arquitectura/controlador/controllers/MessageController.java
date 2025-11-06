package com.arquitectura.controlador.controllers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Controlador para operaciones relacionadas con mensajes:
 * - Enviar mensajes de texto
 * - Enviar mensajes de audio
 * - Obtener historial de canal
 * - Obtener transcripciones de audio
 */
@Component
public class MessageController extends BaseController {
    
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
        "enviarmensajecanal",
        "enviarmensajetexto",
        "enviarmensajeaudio",
        "enviaraudio",
        "solicitarhistorialcanal",
        "obtenermensajescanal",
        "obtenertranscripciones",
        "vertranscripciones"
    );
    
    @Autowired
    public MessageController(IChatFachada chatFachada, Gson gson) {
        super(chatFachada, gson);
    }
    
    @Override
    public boolean handleAction(String action, DTORequest request, IClientHandler handler) {
        if (!SUPPORTED_ACTIONS.contains(action.toLowerCase())) {
            return false;
        }
        
        switch (action.toLowerCase()) {
            case "enviarmensajecanal":
            case "enviarmensajetexto":
                handleSendTextMessage(request, handler);
                break;
            case "enviarmensajeaudio":
            case "enviaraudio":
                handleSendAudioMessage(request, handler);
                break;
            case "solicitarhistorialcanal":
            case "obtenermensajescanal":
                handleGetHistory(request, handler);
                break;
            case "obtenertranscripciones":
            case "vertranscripciones":
                handleGetTranscriptions(request, handler);
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
    
    private void handleSendTextMessage(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "enviarMensajeCanal")) {
            return;
        }

        try {
            JsonObject mensajeJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String canalIdStr = mensajeJson.has("canalId") ? mensajeJson.get("canalId").getAsString() : null;
            String contenido = mensajeJson.has("contenido") ? mensajeJson.get("contenido").getAsString() : null;

            if (canalIdStr == null || canalIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "enviarMensajeCanal", false, "El ID del canal es requerido",
                    createErrorData("canalId", "Campo requerido"));
                return;
            }

            if (contenido == null || contenido.trim().isEmpty()) {
                sendJsonResponse(handler, "enviarMensajeCanal", false, "El contenido del mensaje es requerido",
                    createErrorData("contenido", "Campo requerido"));
                return;
            }

            if (contenido.length() > 5000) {
                sendJsonResponse(handler, "enviarMensajeCanal", false, "El mensaje es demasiado largo (máximo 5000 caracteres)",
                    createErrorData("contenido", "Máximo 5000 caracteres"));
                return;
            }

            UUID canalId = UUID.fromString(canalIdStr);
            UUID autorId = handler.getAuthenticatedUser().getUserId();

            SendMessageRequestDto sendMessageDto = new SendMessageRequestDto(
                canalId,
                "TEXT",
                contenido
            );

            MessageResponseDto messageResponse = chatFachada.enviarMensajeTexto(sendMessageDto, autorId);

            Map<String, Object> mensajeResponseData = new HashMap<>();
            mensajeResponseData.put("messageId", messageResponse.getMessageId().toString());
            mensajeResponseData.put("channelId", messageResponse.getChannelId().toString());
            mensajeResponseData.put("author", Map.of(
                "userId", messageResponse.getAuthor().getUserId().toString(),
                "username", messageResponse.getAuthor().getUsername()
            ));
            mensajeResponseData.put("timestamp", messageResponse.getTimestamp().toString());
            mensajeResponseData.put("messageType", messageResponse.getMessageType());
            mensajeResponseData.put("content", messageResponse.getContent());

            sendJsonResponse(handler, "enviarMensajeCanal", true, "Mensaje enviado", mensajeResponseData);

        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            String campo = "general";
            
            if (errorMessage.contains("Canal") || errorMessage.contains("canal")) {
                campo = "canalId";
            } else if (errorMessage.contains("miembro")) {
                campo = "permisos";
            }
            
            sendJsonResponse(handler, "enviarMensajeCanal", false, errorMessage,
                createErrorData(campo, errorMessage));
                
        } catch (Exception e) {
            System.err.println("Error al enviar mensaje: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "enviarMensajeCanal", false, "Error interno del servidor al enviar mensaje", null);
        }
    }
    
    private void handleGetHistory(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "solicitarHistorialCanal")) {
            return;
        }

        try {
            JsonObject historialJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String histCanalIdStr = historialJson.has("canalId") ? historialJson.get("canalId").getAsString() : null;
            String histUsuarioIdStr = historialJson.has("usuarioId") ? historialJson.get("usuarioId").getAsString() : null;

            if (histCanalIdStr == null || histCanalIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "solicitarHistorialCanal", false, "El ID del canal es requerido",
                    createErrorData("canalId", "Campo requerido"));
                return;
            }

            if (histUsuarioIdStr == null || histUsuarioIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "solicitarHistorialCanal", false, "El ID del usuario es requerido",
                    createErrorData("usuarioId", "Campo requerido"));
                return;
            }

            UUID histCanalId = UUID.fromString(histCanalIdStr);
            UUID histUsuarioId = UUID.fromString(histUsuarioIdStr);

            if (!handler.getAuthenticatedUser().getUserId().equals(histUsuarioId)) {
                sendJsonResponse(handler, "solicitarHistorialCanal", false, "No autorizado para ver este historial",
                    createErrorData("permisos", "Usuario no autorizado"));
                return;
            }

            List<MessageResponseDto> mensajes = chatFachada.obtenerMensajesDeCanal(histCanalId, histUsuarioId);

            List<Map<String, Object>> mensajesEnriquecidos = new ArrayList<>();
            
            for (MessageResponseDto mensaje : mensajes) {
                Map<String, Object> mensajeMap = new HashMap<>();
                mensajeMap.put("messageId", mensaje.getMessageId().toString());
                mensajeMap.put("channelId", mensaje.getChannelId().toString());
                mensajeMap.put("author", Map.of(
                    "userId", mensaje.getAuthor().getUserId().toString(),
                    "username", mensaje.getAuthor().getUsername()
                ));
                mensajeMap.put("timestamp", mensaje.getTimestamp().toString());
                mensajeMap.put("messageType", mensaje.getMessageType());
                
                if ("AUDIO".equals(mensaje.getMessageType())) {
                    try {
                        String base64Content = chatFachada.getFileAsBase64(mensaje.getContent());
                        mensajeMap.put("content", base64Content);
                    } catch (Exception e) {
                        System.err.println("Error al codificar audio a Base64: " + e.getMessage());
                        mensajeMap.put("content", null);
                        mensajeMap.put("error", "Audio no disponible");
                    }
                } else {
                    mensajeMap.put("content", mensaje.getContent());
                }
                
                mensajesEnriquecidos.add(mensajeMap);
            }

            Map<String, Object> historialResponseData = new HashMap<>();
            historialResponseData.put("mensajes", mensajesEnriquecidos);
            historialResponseData.put("totalMensajes", mensajes.size());

            sendJsonResponse(handler, "solicitarHistorialCanal", true, "Historial obtenido", historialResponseData);

        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            String campo = "general";
            
            if (errorMessage.contains("Canal") || errorMessage.contains("canal")) {
                campo = "canalId";
            } else if (errorMessage.contains("miembro")) {
                campo = "permisos";
            }
            
            sendJsonResponse(handler, "solicitarHistorialCanal", false, errorMessage,
                createErrorData(campo, errorMessage));
                
        } catch (Exception e) {
            System.err.println("Error al obtener historial: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "solicitarHistorialCanal", false, "Error interno del servidor al obtener historial", null);
        }
    }
    
    private void handleSendAudioMessage(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "enviarMensajeAudio")) {
            return;
        }

        try {
            JsonObject audioJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String audioCanalIdStr = audioJson.has("canalId") ? audioJson.get("canalId").getAsString() : null;
            String audioBase64 = audioJson.has("audioBase64") ? audioJson.get("audioBase64").getAsString() : null;
            Double duration = audioJson.has("duration") ? audioJson.get("duration").getAsDouble() : null;
            String format = audioJson.has("format") ? audioJson.get("format").getAsString() : "webm";

            if (audioCanalIdStr == null || audioCanalIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "enviarMensajeAudio", false, "El ID del canal es requerido",
                    createErrorData("canalId", "Campo requerido"));
                return;
            }

            if (audioBase64 == null || audioBase64.trim().isEmpty()) {
                sendJsonResponse(handler, "enviarMensajeAudio", false, "El audio es requerido",
                    createErrorData("audioBase64", "Campo requerido"));
                return;
            }

            UUID audioCanalId = UUID.fromString(audioCanalIdStr);
            UUID autorId = handler.getAuthenticatedUser().getUserId();

            // Guardar el archivo de audio usando el sistema de chunks
            String fileName = "audio_" + autorId + "_" + System.currentTimeMillis() + "." + format;
            String audioFilePath = chatFachada.guardarArchivoDeAudio(fileName, audioBase64, autorId);

            // Crear DTO de request con la ruta del archivo
            SendMessageRequestDto sendAudioDto = new SendMessageRequestDto(
                audioCanalId,
                "AUDIO",
                audioFilePath
            );

            // Llamar a la fachada para enviar el mensaje de audio
            MessageResponseDto audioResponse = chatFachada.enviarMensajeAudio(sendAudioDto, autorId);

            // Construir respuesta exitosa
            Map<String, Object> audioResponseData = new HashMap<>();
            audioResponseData.put("messageId", audioResponse.getMessageId().toString());
            audioResponseData.put("channelId", audioResponse.getChannelId().toString());
            audioResponseData.put("author", Map.of(
                "userId", audioResponse.getAuthor().getUserId().toString(),
                "username", audioResponse.getAuthor().getUsername()
            ));
            audioResponseData.put("timestamp", audioResponse.getTimestamp().toString());
            audioResponseData.put("messageType", audioResponse.getMessageType());
            audioResponseData.put("content", audioResponse.getContent());
            if (duration != null) {
                audioResponseData.put("duration", duration);
            }

            sendJsonResponse(handler, "enviarMensajeAudio", true, "Audio enviado", audioResponseData);

        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            String campo = "general";
            
            if (errorMessage.contains("Canal") || errorMessage.contains("canal")) {
                campo = "canalId";
            } else if (errorMessage.contains("miembro")) {
                campo = "permisos";
            } else if (errorMessage.contains("audio")) {
                campo = "audioBase64";
            }
            
            sendJsonResponse(handler, "enviarMensajeAudio", false, errorMessage,
                createErrorData(campo, errorMessage));
                
        } catch (Exception e) {
            System.err.println("Error al enviar audio: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "enviarMensajeAudio", false, "Error interno del servidor al enviar audio", null);
        }
    }
    
    private void handleGetTranscriptions(DTORequest request, IClientHandler handler) {
        try {
            // El payload es opcional - puede filtrar por messageId o traer todas
            UUID messageIdFilter = null;
            
            if (request.getPayload() != null) {
                JsonObject transcripcionJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
                String messageIdStr = transcripcionJson.has("messageId") ? transcripcionJson.get("messageId").getAsString() : null;
                if (messageIdStr != null && !messageIdStr.trim().isEmpty()) {
                    messageIdFilter = UUID.fromString(messageIdStr);
                }
            }

            // Obtener todas las transcripciones
            List<com.arquitectura.DTO.Mensajes.TranscriptionResponseDto> transcripciones = chatFachada.obtenerTranscripciones();

            // Filtrar por messageId si se proporcionó
            if (messageIdFilter != null) {
                final UUID finalMessageIdFilter = messageIdFilter;
                transcripciones = transcripciones.stream()
                    .filter(t -> t.getChannelId() != null) // Asegurar que tenga channelId
                    .collect(java.util.stream.Collectors.toList());
            }

            // Construir respuesta
            List<Map<String, Object>> transcripcionesData = new ArrayList<>();
            
            for (com.arquitectura.DTO.Mensajes.TranscriptionResponseDto transcripcion : transcripciones) {
                Map<String, Object> transcripcionMap = new HashMap<>();
                transcripcionMap.put("messageId", transcripcion.getMessageId().toString());
                transcripcionMap.put("text", transcripcion.getTranscribedText());
                transcripcionMap.put("timestamp", transcripcion.getProcessedDate().toString());
                
                if (transcripcion.getAuthor() != null) {
                    transcripcionMap.put("author", Map.of(
                        "userId", transcripcion.getAuthor().getUserId().toString(),
                        "username", transcripcion.getAuthor().getUsername()
                    ));
                }
                
                if (transcripcion.getChannelId() != null) {
                    transcripcionMap.put("channelId", transcripcion.getChannelId().toString());
                }
                
                transcripcionesData.add(transcripcionMap);
            }

            Map<String, Object> transcripcionesResponseData = new HashMap<>();
            transcripcionesResponseData.put("transcripciones", transcripcionesData);
            transcripcionesResponseData.put("totalTranscripciones", transcripciones.size());

            sendJsonResponse(handler, "obtenerTranscripciones", true, "Transcripciones obtenidas", transcripcionesResponseData);

        } catch (Exception e) {
            System.err.println("Error al obtener transcripciones: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "obtenerTranscripciones", false, "Error interno del servidor al obtener transcripciones", null);
        }
    }
}
