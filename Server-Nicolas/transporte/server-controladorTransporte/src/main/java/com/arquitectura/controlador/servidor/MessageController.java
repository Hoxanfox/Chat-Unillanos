package com.arquitectura.controlador.servidor;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.controlador.routing.P2PRoutingHelper;
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
 *
 * ENRUTAMIENTO AUTOMÁTICO P2P:
 * Detecta automáticamente cuando el destinatario está en otro peer y enruta transparentemente.
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
        "vertranscripciones",
        "enviarmensajedirecto",
        "enviarmensajedirectoaudio",
        "solicitarhistorialprivado"
    );
    
    private final P2PRoutingHelper routingHelper;

    @Autowired
    public MessageController(IChatFachada chatFachada, Gson gson, P2PRoutingHelper routingHelper) {
        super(chatFachada, gson);
        this.routingHelper = routingHelper;
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
            case "enviarmensajedirecto":
                handleSendDirectMessage(request, handler);
                break;
            case "enviarmensajedirectoaudio":
                handleSendDirectAudioMessage(request, handler);
                break;
            case "solicitarhistorialprivado":
                handleGetPrivateHistory(request, handler);
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

            MessageResponseDto messageResponse = chatFachada.mensajes().enviarMensajeTexto(sendMessageDto, autorId);

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

            List<MessageResponseDto> mensajes = chatFachada.mensajes().obtenerMensajesDeCanal(histCanalId, histUsuarioId);

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
                        String base64Content = chatFachada.archivos().getFileAsBase64(mensaje.getContent());
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
            String audioFilePath = chatFachada.mensajes().guardarArchivoDeAudio(fileName, audioBase64, autorId);

            // Crear DTO de request con la ruta del archivo
            SendMessageRequestDto sendAudioDto = new SendMessageRequestDto(
                audioCanalId,
                "AUDIO",
                audioFilePath
            );

            // Llamar a la fachada para enviar el mensaje de audio
            MessageResponseDto audioResponse = chatFachada.mensajes().enviarMensajeAudio(sendAudioDto, autorId);

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
            List<com.arquitectura.DTO.Mensajes.TranscriptionResponseDto> transcripciones = chatFachada.mensajes().obtenerTranscripciones();

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

    /**
     * Maneja el envío de mensajes directos entre usuarios.
     * Crea o recupera un canal directo y envía el mensaje.
     *
     * ENRUTAMIENTO AUTOMÁTICO P2P:
     * Detecta automáticamente si el destinatario está en otro peer y enruta transparentemente.
     */
    private void handleSendDirectMessage(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "enviarMensajeDirecto")) {
            return;
        }

        try {
            JsonObject mensajeJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();

            // Extraer campos del request
            String peerDestinoIdStr = mensajeJson.has("peerDestinoId") ? mensajeJson.get("peerDestinoId").getAsString() : null;
            String peerRemitenteIdStr = mensajeJson.has("peerRemitenteId") ? mensajeJson.get("peerRemitenteId").getAsString() : null;
            String remitenteIdStr = mensajeJson.has("remitenteId") ? mensajeJson.get("remitenteId").getAsString() : null;
            String destinatarioIdStr = mensajeJson.has("destinatarioId") ? mensajeJson.get("destinatarioId").getAsString() : null;
            String tipo = mensajeJson.has("tipo") ? mensajeJson.get("tipo").getAsString() : "texto";
            String contenido = mensajeJson.has("contenido") ? mensajeJson.get("contenido").getAsString() : null;
            String fechaEnvioStr = mensajeJson.has("fechaEnvio") ? mensajeJson.get("fechaEnvio").getAsString() : null;

            // Validaciones de campos requeridos
            if (remitenteIdStr == null || remitenteIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "enviarMensajeDirecto", false, "Datos de mensaje inválidos",
                    createErrorData("remitenteId", "El ID del remitente es requerido"));
                return;
            }

            if (destinatarioIdStr == null || destinatarioIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "enviarMensajeDirecto", false, "Datos de mensaje inválidos",
                    createErrorData("destinatarioId", "El ID del destinatario es requerido"));
                return;
            }

            if (contenido == null || contenido.trim().isEmpty()) {
                sendJsonResponse(handler, "enviarMensajeDirecto", false, "Datos de mensaje inválidos",
                    createErrorData("contenido", "El contenido no puede estar vacío"));
                return;
            }

            if (contenido.length() > 5000) {
                sendJsonResponse(handler, "enviarMensajeDirecto", false, "Datos de mensaje inválidos",
                    createErrorData("contenido", "El mensaje es demasiado largo (máximo 5000 caracteres)"));
                return;
            }

            // Convertir IDs a UUID
            UUID remitenteId = UUID.fromString(remitenteIdStr);
            UUID destinatarioId = UUID.fromString(destinatarioIdStr);

            // Verificar autenticación: el remitente debe ser el usuario autenticado
            if (!handler.getAuthenticatedUser().getUserId().equals(remitenteId)) {
                sendJsonResponse(handler, "enviarMensajeDirecto", false, "Error al enviar mensaje: Usuario no autorizado",
                    createErrorData("remitenteId", "No tienes permiso para enviar como este usuario"));
                return;
            }

            // ===== ENRUTAMIENTO AUTOMÁTICO P2P =====
            // Verificar si la petición ya fue enrutada (viene de otro peer)
            boolean yaEnrutado = mensajeJson.has("_yaEnrutado") && mensajeJson.get("_yaEnrutado").getAsBoolean();

            if (!yaEnrutado) {
                // Detectar si el destinatario está en otro peer
                System.out.println("🔍 [MessageController] Verificando ubicación del destinatario...");

                // Convertir payload a Map
                @SuppressWarnings("unchecked")
                Map<String, Object> payloadMap = (Map<String, Object>) request.getPayload();

                Optional<DTOResponse> respuestaEnrutada = routingHelper.enrutarSiEsNecesario(
                    destinatarioId,
                    "enviarMensajeDirecto",
                    payloadMap,
                    handler
                );

                // Si la petición fue enrutada a otro peer, devolver la respuesta
                if (respuestaEnrutada.isPresent()) {
                    DTOResponse respuesta = respuestaEnrutada.get();
                    System.out.println("✅ [MessageController] Mensaje enrutado a peer remoto exitosamente");

                    // Extraer la respuesta del peer remoto
                    if ("success".equals(respuesta.getStatus())) {
                        sendJsonResponse(handler, "enviarMensajeDirecto", true,
                            respuesta.getMessage() != null ? respuesta.getMessage() : "Mensaje enviado a peer remoto",
                            respuesta.getData());
                    } else {
                        sendJsonResponse(handler, "enviarMensajeDirecto", false,
                            respuesta.getMessage() != null ? respuesta.getMessage() : "Error al enviar al peer remoto",
                            respuesta.getData());
                    }
                    return;
                }
            } else {
                System.out.println("📍 [MessageController] Petición ya enrutada desde otro peer, procesando localmente...");
            }

            // ===== PROCESAMIENTO LOCAL =====
            // El destinatario está en este peer, procesar localmente
            System.out.println("📍 [MessageController] Destinatario es local, procesando mensaje...");

            // Obtener o crear el canal directo entre remitente y destinatario
            com.arquitectura.DTO.canales.ChannelResponseDto canalDirecto;
            try {
                canalDirecto = chatFachada.canales().crearCanalDirecto(remitenteId, destinatarioId);
            } catch (Exception e) {
                System.err.println("Error al crear/obtener canal directo: " + e.getMessage());

                // Verificar si el error es porque el destinatario no existe
                if (e.getMessage() != null && (e.getMessage().contains("Usuario") || e.getMessage().contains("no existe"))) {
                    sendJsonResponse(handler, "enviarMensajeDirecto", false, "Destinatario no encontrado o desconectado", null);
                    return;
                }

                throw e;
            }

            // Preparar el DTO de envío de mensaje
            String messageType = tipo.equalsIgnoreCase("audio") ? "AUDIO" : "TEXT";
            SendMessageRequestDto sendMessageDto = new SendMessageRequestDto(
                canalDirecto.getChannelId(),
                messageType,
                contenido
            );

            // Enviar el mensaje según el tipo
            MessageResponseDto messageResponse;
            if (messageType.equals("AUDIO")) {
                messageResponse = chatFachada.mensajes().enviarMensajeAudio(sendMessageDto, remitenteId);
            } else {
                messageResponse = chatFachada.mensajes().enviarMensajeTexto(sendMessageDto, remitenteId);
            }

            // Construir respuesta exitosa en el formato solicitado
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("mensajeId", messageResponse.getMessageId().toString());
            responseData.put("fechaEnvio", messageResponse.getTimestamp().toString());

            sendJsonResponse(handler, "enviarMensajeDirecto", true, "Mensaje enviado", responseData);

        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            String campo = "general";

            if (errorMessage != null) {
                if (errorMessage.contains("remitente")) {
                    campo = "remitenteId";
                } else if (errorMessage.contains("destinatario")) {
                    campo = "destinatarioId";
                } else if (errorMessage.contains("contenido")) {
                    campo = "contenido";
                }
            }

            sendJsonResponse(handler, "enviarMensajeDirecto", false, "Datos de mensaje inválidos",
                createErrorData(campo, errorMessage != null ? errorMessage : "Error de validación"));

        } catch (Exception e) {
            System.err.println("Error al enviar mensaje directo: " + e.getMessage());
            e.printStackTrace();

            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("no existe") || errorMsg.contains("not found"))) {
                sendJsonResponse(handler, "enviarMensajeDirecto", false, "Destinatario no encontrado o desconectado", null);
            } else {
                sendJsonResponse(handler, "enviarMensajeDirecto", false, "Error al enviar mensaje: " +
                    (errorMsg != null ? errorMsg : "Error desconocido"), null);
            }
        }
    }

    /**
     * Maneja el envío de mensajes de audio directos entre usuarios.
     * Crea o recupera un canal directo y envía el mensaje de audio.
     */
    private void handleSendDirectAudioMessage(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "enviarMensajeDirectoAudio")) {
            return;
        }

        try {
            JsonObject mensajeJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();

            // Extraer campos del request
            String peerDestinoIdStr = mensajeJson.has("peerDestinoId") ? mensajeJson.get("peerDestinoId").getAsString() : null;
            String peerRemitenteIdStr = mensajeJson.has("peerRemitenteId") ? mensajeJson.get("peerRemitenteId").getAsString() : null;
            String remitenteIdStr = mensajeJson.has("remitenteId") ? mensajeJson.get("remitenteId").getAsString() : null;
            String destinatarioIdStr = mensajeJson.has("destinatarioId") ? mensajeJson.get("destinatarioId").getAsString() : null;
            String contenido = mensajeJson.has("contenido") ? mensajeJson.get("contenido").getAsString() : null;
            String fechaEnvioStr = mensajeJson.has("fechaEnvio") ? mensajeJson.get("fechaEnvio").getAsString() : null;
            String tipo = mensajeJson.has("tipo") ? mensajeJson.get("tipo").getAsString() : "audio";

            // Validaciones de campos requeridos
            if (remitenteIdStr == null || remitenteIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "enviarMensajeDirectoAudio", false, "Datos de mensaje inválidos",
                    createErrorData("remitenteId", "El ID del remitente es requerido"));
                return;
            }

            if (destinatarioIdStr == null || destinatarioIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "enviarMensajeDirectoAudio", false, "Datos de mensaje inválidos",
                    createErrorData("destinatarioId", "El ID del destinatario es requerido"));
                return;
            }

            if (contenido == null || contenido.trim().isEmpty()) {
                sendJsonResponse(handler, "enviarMensajeDirectoAudio", false, "Datos de mensaje inválidos",
                    createErrorData("contenido", "El enlace del archivo de audio es requerido"));
                return;
            }

            // Convertir IDs a UUID
            UUID remitenteId;
            UUID destinatarioId;

            try {
                remitenteId = UUID.fromString(remitenteIdStr);
                destinatarioId = UUID.fromString(destinatarioIdStr);
            } catch (IllegalArgumentException e) {
                sendJsonResponse(handler, "enviarMensajeDirectoAudio", false, "Datos de mensaje inválidos",
                    createErrorData("general", "Formato de UUID inválido"));
                return;
            }

            // Verificar autenticación: el remitente debe ser el usuario autenticado
            if (!handler.getAuthenticatedUser().getUserId().equals(remitenteId)) {
                sendJsonResponse(handler, "enviarMensajeDirectoAudio", false, "Error al enviar mensaje de audio: Usuario no autorizado",
                    createErrorData("remitenteId", "No tienes permiso para enviar como este usuario"));
                return;
            }

            // Obtener o crear el canal directo entre remitente y destinatario
            com.arquitectura.DTO.canales.ChannelResponseDto canalDirecto;
            try {
                canalDirecto = chatFachada.canales().crearCanalDirecto(remitenteId, destinatarioId);
            } catch (Exception e) {
                System.err.println("Error al crear/obtener canal directo: " + e.getMessage());

                // Verificar si el error es porque el destinatario no existe
                if (e.getMessage() != null && (e.getMessage().contains("Usuario") || e.getMessage().contains("no existe"))) {
                    sendJsonResponse(handler, "enviarMensajeDirectoAudio", false, "Destinatario no encontrado o desconectado", null);
                    return;
                }

                throw e;
            }

            // El contenido es un enlace/ruta del archivo de audio
            // Se usa directamente como ruta del archivo
            String audioFilePath = contenido;

            // Crear DTO de request con la ruta del archivo
            SendMessageRequestDto sendAudioDto = new SendMessageRequestDto(
                canalDirecto.getChannelId(),
                "AUDIO",
                audioFilePath
            );

            // Llamar a la fachada para enviar el mensaje de audio
            MessageResponseDto audioResponse = chatFachada.mensajes().enviarMensajeAudio(sendAudioDto, remitenteId);

            // Construir respuesta exitosa en el formato solicitado
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("mensajeId", audioResponse.getMessageId().toString());
            responseData.put("fechaEnvio", audioResponse.getTimestamp().toString());

            sendJsonResponse(handler, "enviarMensajeDirectoAudio", true, "Mensaje de audio enviado", responseData);

        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            String campo = "general";

            if (errorMessage != null) {
                if (errorMessage.contains("remitente")) {
                    campo = "remitenteId";
                } else if (errorMessage.contains("destinatario")) {
                    campo = "destinatarioId";
                } else if (errorMessage.contains("audio") || errorMessage.contains("contenido")) {
                    campo = "contenido";
                }
            }

            sendJsonResponse(handler, "enviarMensajeDirectoAudio", false, "Datos de mensaje inválidos",
                createErrorData(campo, errorMessage != null ? errorMessage : "Error de validación"));

        } catch (Exception e) {
            System.err.println("Error al enviar mensaje de audio directo: " + e.getMessage());
            e.printStackTrace();

            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("no existe") || errorMsg.contains("not found"))) {
                sendJsonResponse(handler, "enviarMensajeDirectoAudio", false, "Destinatario no encontrado o desconectado", null);
            } else {
                sendJsonResponse(handler, "enviarMensajeDirectoAudio", false, "Error al enviar mensaje de audio: " +
                    (errorMsg != null ? errorMsg : "Error desconocido"), null);
            }
        }
    }

    /**
     * Maneja la solicitud de historial privado entre dos usuarios.
     * Obtiene todos los mensajes del canal directo compartido.
     */
    private void handleGetPrivateHistory(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "solicitarHistorialPrivado")) {
            return;
        }

        try {
            JsonObject historialJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();

            // Extraer campos del request
            String remitenteIdStr = historialJson.has("remitenteId") ? historialJson.get("remitenteId").getAsString() : null;
            String peerRemitenteIdStr = historialJson.has("peerRemitenteId") ? historialJson.get("peerRemitenteId").getAsString() : null;
            String destinatarioIdStr = historialJson.has("destinatarioId") ? historialJson.get("destinatarioId").getAsString() : null;
            String peerDestinatarioIdStr = historialJson.has("peerDestinatarioId") ? historialJson.get("peerDestinatarioId").getAsString() : null;

            // Validaciones de campos requeridos
            if (remitenteIdStr == null || remitenteIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "solicitarHistorialPrivado", false, "Error al obtener el historial: remitenteId requerido", null);
                return;
            }

            if (destinatarioIdStr == null || destinatarioIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "solicitarHistorialPrivado", false, "Error al obtener el historial: destinatarioId requerido", null);
                return;
            }

            // Convertir IDs a UUID
            UUID remitenteId;
            UUID destinatarioId;

            try {
                remitenteId = UUID.fromString(remitenteIdStr);
                destinatarioId = UUID.fromString(destinatarioIdStr);
            } catch (IllegalArgumentException e) {
                sendJsonResponse(handler, "solicitarHistorialPrivado", false, "Error al obtener el historial: Formato de UUID inválido", null);
                return;
            }

            // Verificar autenticación: el remitente debe ser el usuario autenticado
            if (!handler.getAuthenticatedUser().getUserId().equals(remitenteId)) {
                sendJsonResponse(handler, "solicitarHistorialPrivado", false, "Error al obtener el historial: Usuario no autorizado", null);
                return;
            }

            // Obtener el historial privado de la fachada
            List<MessageResponseDto> mensajes = chatFachada.mensajes().obtenerHistorialPrivado(remitenteId, destinatarioId);

            // Construir la lista de mensajes en el formato solicitado
            List<Map<String, Object>> mensajesData = new ArrayList<>();

            for (MessageResponseDto mensaje : mensajes) {
                Map<String, Object> mensajeMap = new HashMap<>();

                mensajeMap.put("mensajeId", mensaje.getMessageId().toString());

                // Determinar remitente y destinatario del mensaje
                UUID autorMensajeId = mensaje.getAuthor().getUserId();
                if (autorMensajeId.equals(remitenteId)) {
                    mensajeMap.put("remitenteId", remitenteId.toString());
                    mensajeMap.put("destinatarioId", destinatarioId.toString());
                } else {
                    mensajeMap.put("remitenteId", destinatarioId.toString());
                    mensajeMap.put("destinatarioId", remitenteId.toString());
                }

                // Agregar peer IDs si están disponibles (pueden ser null)
                mensajeMap.put("peerRemitenteId", mensaje.getAuthor().getPeerId() != null ? mensaje.getAuthor().getPeerId().toString() : null);
                mensajeMap.put("peerDestinoId", null); // TODO: Obtener peer del destinatario si es necesario

                // Tipo de mensaje
                String tipo = "TEXT".equals(mensaje.getMessageType()) ? "texto" : "audio";
                mensajeMap.put("tipo", tipo);

                // Contenido del mensaje
                mensajeMap.put("contenido", mensaje.getContent());

                // Fecha de envío
                mensajeMap.put("fechaEnvio", mensaje.getTimestamp().toString());

                mensajesData.add(mensajeMap);
            }

            // Enviar respuesta exitosa con el array de mensajes directamente en data
            sendJsonResponse(handler, "solicitarHistorialPrivado", true, "Historial privado obtenido exitosamente", mensajesData);

        } catch (IllegalArgumentException e) {
            sendJsonResponse(handler, "solicitarHistorialPrivado", false, "Error al obtener el historial: " + e.getMessage(), null);
        } catch (Exception e) {
            System.err.println("Error al obtener historial privado: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "solicitarHistorialPrivado", false, "Error al obtener el historial: " +
                (e.getMessage() != null ? e.getMessage() : "Error desconocido"), null);
        }
    }
}
