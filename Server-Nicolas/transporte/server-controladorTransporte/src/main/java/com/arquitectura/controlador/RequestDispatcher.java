package com.arquitectura.controlador;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse; // <-- Usar el DTOResponse estándar
import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.DTO.archivos.DTODownloadInfo;
import com.arquitectura.DTO.archivos.DTOEndUpload;
import com.arquitectura.DTO.archivos.DTOStartUpload;
import com.arquitectura.DTO.archivos.DTOUploadChunk;
import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.fachada.IChatFachada;
import com.arquitectura.utils.chunkManager.FileUploadResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RequestDispatcher {

    private final IChatFachada chatFachada;
    private final Gson gson;
    private static final Set<String> ACCIONES_PUBLICAS = Set.of(
            "authenticateuser",
            "registeruser",                // Registro de usuarios (público)
            "uploadfileforregistration",  // Subida sin autenticación para registro
            "uploadfilechunk",             // Los chunks pueden ser públicos
            "endfileupload"                // Finalizar upload puede ser público
    );

    @Autowired
    public RequestDispatcher(IChatFachada chatFachada, Gson gson) {
        this.chatFachada = chatFachada;
        this.gson = gson;
    }

    public void dispatch(String requestJson, IClientHandler handler) {
        DTORequest request;
        String action = "unknown";
        try {
            request = gson.fromJson(requestJson, DTORequest.class);
            action = request.getAction() != null ? request.getAction().toLowerCase() : "unknown";

            // 2. Validar sesión
            if (!ACCIONES_PUBLICAS.contains(action)&& !handler.isAuthenticated()) {
                sendJsonResponse(handler, action, false, "Debes iniciar sesión para realizar esta acción.", null);
                return;
            }

            switch (action) {
                case "authenticateuser":
                    Object dataObj = request.getPayload();
                    if (dataObj == null) {
                        sendJsonResponse(handler, "authenticateUser", false, "Falta payload.", null);
                        return;
                    }

                    // Convertir el data a JSON y luego a LoginRequestDto
                    JsonObject dataJson = gson.toJsonTree(dataObj).getAsJsonObject();
                    String nombreUsuario = dataJson.has("nombreUsuario") ? dataJson.get("nombreUsuario").getAsString() : null;
                    String password = dataJson.has("password") ? dataJson.get("password").getAsString() : null;

                    if (nombreUsuario == null || password == null) {
                        sendJsonResponse(handler, "authenticateUser", false, "usuario o contraseña inválidos",
                            createErrorData("nombreUsuario", "El campo nombreUsuario es requerido"));
                        return;
                    }

                    LoginRequestDto serverLoginDto = new LoginRequestDto(nombreUsuario, password);
                    UserResponseDto userDto = chatFachada.autenticarUsuario(serverLoginDto, handler.getClientIpAddress());

                    // Autenticación exitosa
                    handler.setAuthenticatedUser(userDto);

                    // Construcción de la respuesta según el API del cliente
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("userId", userDto.getUserId().toString());
                    responseData.put("nombre", userDto.getUsername());
                    responseData.put("email", userDto.getEmail());
                    // Enviar el fileId (ruta relativa) para que el cliente lo descargue con chunks
                    // Si no tiene foto, enviar null
                    responseData.put("fileId", userDto.getPhotoAddress());

                    sendJsonResponse(handler, "authenticateUser", true, "Autenticación exitosa", responseData);
                    break;

                case "logoutuser":
                    // Ya no necesitamos validar isAuthenticated() aquí porque se valida arriba
                    Object logoutDataObj = request.getPayload();
                    if (logoutDataObj == null) {
                        sendJsonResponse(handler, "logoutUser", false, "Error al cerrar sesión: Falta el userId", null);
                        return;
                    }

                    // Extraer el userId del data
                    JsonObject logoutDataJson = gson.toJsonTree(logoutDataObj).getAsJsonObject();
                    String userIdStr = logoutDataJson.has("userId") ? logoutDataJson.get("userId").getAsString() : null;

                    if (userIdStr == null || userIdStr.isEmpty()) {
                        sendJsonResponse(handler, "logoutUser", false, "Error al cerrar sesión: userId requerido", null);
                        return;
                    }

                    try {
                        UUID userId = UUID.fromString(userIdStr);

                        // Verificar que el userId coincida con el usuario autenticado (seguridad)
                        if (!handler.getAuthenticatedUser().getUserId().equals(userId)) {
                            sendJsonResponse(handler, "logoutUser", false, "Usuario no autenticado o token inválido", null);
                            return;
                        }
                        // Realizar el logout
                        chatFachada.cambiarEstadoUsuario(userId, false);
                        handler.clearAuthenticatedUser();

                        sendJsonResponse(handler, "logoutUser", true, "Sesión cerrada exitosamente", null);
                    } catch (IllegalArgumentException e) {
                        sendJsonResponse(handler, "logoutUser", false, "Error al cerrar sesión: userId inválido", null);
                    }
                    break;

                case "registeruser":
                    // 1. Extraer payload
                    Object registerDataObj = request.getPayload();
                    if (registerDataObj == null) {
                        sendJsonResponse(handler, "registerUser", false, "Falta payload", null);
                        return;
                    }

                    // 2. Convertir a JSON y extraer campos
                    JsonObject registerJson = gson.toJsonTree(registerDataObj).getAsJsonObject();
                    String regUsername = registerJson.has("username") ? registerJson.get("username").getAsString() : null;
                    String regEmail = registerJson.has("email") ? registerJson.get("email").getAsString() : null;
                    String regPassword = registerJson.has("password") ? registerJson.get("password").getAsString() : null;
                    String regPhotoFileId = registerJson.has("photoFileId") ? registerJson.get("photoFileId").getAsString() : null;

                    // 3. Validar campos requeridos
                    if (regUsername == null || regUsername.trim().isEmpty()) {
                        sendJsonResponse(handler, "registerUser", false, "El nombre de usuario es requerido",
                            createErrorData("username", "Campo requerido"));
                        return;
                    }

                    if (regEmail == null || regEmail.trim().isEmpty()) {
                        sendJsonResponse(handler, "registerUser", false, "El email es requerido",
                            createErrorData("email", "Campo requerido"));
                        return;
                    }

                    if (regPassword == null || regPassword.trim().isEmpty()) {
                        sendJsonResponse(handler, "registerUser", false, "La contraseña es requerida",
                            createErrorData("password", "Campo requerido"));
                        return;
                    }

                    // 4. Validar formato de email (básico)
                    if (!regEmail.contains("@") || !regEmail.contains(".")) {
                        sendJsonResponse(handler, "registerUser", false, "Formato de email inválido",
                            createErrorData("email", "Formato inválido"));
                        return;
                    }

                    // 5. Validar longitud de contraseña
                    if (regPassword.length() < 6) {
                        sendJsonResponse(handler, "registerUser", false, "La contraseña debe tener al menos 6 caracteres",
                            createErrorData("password", "Mínimo 6 caracteres"));
                        return;
                    }

                    try {
                        // 6. Crear DTO
                        UserRegistrationRequestDto registrationDto = new UserRegistrationRequestDto(
                            regUsername,
                            regEmail,
                            regPassword,
                            regPhotoFileId
                        );

                        // 7. Llamar a la fachada
                        chatFachada.registrarUsuario(registrationDto, handler.getClientIpAddress());

                        // 8. Construir respuesta exitosa
                        Map<String, Object> registerResponseData = new HashMap<>();
                        registerResponseData.put("username", regUsername);
                        registerResponseData.put("email", regEmail);
                        registerResponseData.put("message", "Usuario registrado exitosamente. Ahora puedes iniciar sesión.");

                        sendJsonResponse(handler, "registerUser", true, "Registro exitoso", registerResponseData);

                    } catch (IllegalArgumentException e) {
                        // Error de validación (email duplicado, username duplicado, etc.)
                        String errorMessage = e.getMessage();
                        String campo = "general";
                        
                        if (errorMessage.contains("email")) {
                            campo = "email";
                        } else if (errorMessage.contains("username") || errorMessage.contains("usuario")) {
                            campo = "username";
                        }
                        
                        sendJsonResponse(handler, "registerUser", false, errorMessage,
                            createErrorData(campo, errorMessage));
                            
                    } catch (Exception e) {
                        // Error inesperado
                        System.err.println("Error al registrar usuario: " + e.getMessage());
                        sendJsonResponse(handler, "registerUser", false, "Error interno del servidor al registrar usuario", null);
                    }
                    break;

                case "listarcontactos":
                    Object listarContactosDataObj = request.getPayload();
                    if (listarContactosDataObj == null) {
                        sendJsonResponse(handler, "listarContactos", false, "Error al obtener contactos: Falta el usuarioId", null);
                        return;
                    }

                    JsonObject listarContactosJson = gson.toJsonTree(listarContactosDataObj).getAsJsonObject();
                    String usuarioIdStr = listarContactosJson.has("usuarioId") ? listarContactosJson.get("usuarioId").getAsString() : null;

                    if (usuarioIdStr == null || usuarioIdStr.isEmpty()) {
                        sendJsonResponse(handler, "listarContactos", false, "Error al obtener contactos: usuarioId requerido", null);
                        return;
                    }

                    try {
                        UUID usuarioId = UUID.fromString(usuarioIdStr);

                        // Verificar que el usuarioId coincida con el usuario autenticado (seguridad)
                        if (!handler.getAuthenticatedUser().getUserId().equals(usuarioId)) {
                            sendJsonResponse(handler, "listarContactos", false, "Usuario no autenticado o token inválido", null);
                            return;
                        }

                        // Obtener la lista de contactos (todos los usuarios excepto el actual)
                        List<UserResponseDto> contactos = chatFachada.listarContactos(usuarioId);

                        // Construir la respuesta según el formato del API
                        List<Map<String, Object>> contactosData = new ArrayList<>();
                        for (UserResponseDto contacto : contactos) {
                            Map<String, Object> contactoMap = new HashMap<>();
                            contactoMap.put("id", contacto.getUserId().toString());
                            contactoMap.put("nombre", contacto.getUsername());
                            contactoMap.put("email", contacto.getEmail());
                            contactoMap.put("imagenBase64", contacto.getImagenBase64());
                            contactoMap.put("conectado", contacto.getEstado());
                            contactosData.add(contactoMap);
                        }

                        sendJsonResponse(handler, "listarContactos", true, "Lista de contactos obtenida", contactosData);
                    } catch (IllegalArgumentException e) {
                        sendJsonResponse(handler, "listarContactos", false, "Error al obtener contactos: usuarioId inválido", null);
                    } catch (Exception e) {
                        sendJsonResponse(handler, "listarContactos", false, "Error al obtener contactos: " + e.getMessage(), null);
                    }
                    break;

                case "listarcanales":
                    Object listarCanalesDataObj = request.getPayload();
                    if (listarCanalesDataObj == null) {
                        sendJsonResponse(handler, "listarCanales", false, "Error al listar canales: Falta el usuarioId", null);
                        return;
                    }

                    JsonObject listarCanalesJson = gson.toJsonTree(listarCanalesDataObj).getAsJsonObject();
                    String canalesUsuarioIdStr = listarCanalesJson.has("usuarioId") ? listarCanalesJson.get("usuarioId").getAsString() : null;

                    if (canalesUsuarioIdStr == null || canalesUsuarioIdStr.isEmpty()) {
                        sendJsonResponse(handler, "listarCanales", false, "Error al listar canales: usuarioId requerido", null);
                        return;
                    }

                    try {
                        UUID canalesUsuarioId = UUID.fromString(canalesUsuarioIdStr);

                        // Verificar que el usuarioId coincida con el usuario autenticado (seguridad)
                        if (!handler.getAuthenticatedUser().getUserId().equals(canalesUsuarioId)) {
                            sendJsonResponse(handler, "listarCanales", false, "Usuario no autorizado para ver esta lista", null);
                            return;
                        }

                        // Obtener la lista de canales del usuario
                        List<ChannelResponseDto> canales = chatFachada.obtenerCanalesPorUsuario(canalesUsuarioId);

                        // Construir la respuesta según el formato del API
                        List<Map<String, Object>> canalesData = new ArrayList<>();
                        for (ChannelResponseDto canal : canales) {
                            Map<String, Object> canalMap = new HashMap<>();
                            canalMap.put("idCanal", canal.getChannelId().toString());
                            canalMap.put("idPeer", canal.getPeerId() != null ? canal.getPeerId().toString() : null);
                            canalMap.put("nombreCanal", canal.getChannelName());
                            canalMap.put("ownerId", canal.getOwner().getUserId().toString());
                            canalesData.add(canalMap);
                        }

                        sendJsonResponse(handler, "listarCanales", true, "Lista de canales obtenida", canalesData);
                    } catch (IllegalArgumentException e) {
                        sendJsonResponse(handler, "listarCanales", false, "Error al listar canales: usuarioId inválido", null);
                    } catch (Exception e) {
                        sendJsonResponse(handler, "listarCanales", false, "Error al listar canales: " + e.getMessage(), null);
                    }
                    break;

                case "enviarmensajecanal":
                case "enviarmensajetexto":
                    // 1. Extraer payload
                    Object mensajeDataObj = request.getPayload();
                    if (mensajeDataObj == null) {
                        sendJsonResponse(handler, "enviarMensajeCanal", false, "Falta payload", null);
                        return;
                    }

                    // 2. Convertir a JSON y extraer campos
                    JsonObject mensajeJson = gson.toJsonTree(mensajeDataObj).getAsJsonObject();
                    String canalIdStr = mensajeJson.has("canalId") ? mensajeJson.get("canalId").getAsString() : null;
                    String contenido = mensajeJson.has("contenido") ? mensajeJson.get("contenido").getAsString() : null;

                    // 3. Validar campos requeridos
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

                    // 4. Validar longitud del mensaje (opcional pero recomendado)
                    if (contenido.length() > 5000) {
                        sendJsonResponse(handler, "enviarMensajeCanal", false, "El mensaje es demasiado largo (máximo 5000 caracteres)",
                            createErrorData("contenido", "Máximo 5000 caracteres"));
                        return;
                    }

                    try {
                        // 5. Convertir canalId a UUID
                        UUID canalId = UUID.fromString(canalIdStr);

                        // 6. Obtener ID del usuario autenticado
                        UUID autorId = handler.getAuthenticatedUser().getUserId();

                        // 7. Crear DTO de request
                        SendMessageRequestDto sendMessageDto = new SendMessageRequestDto(
                            canalId,
                            "TEXT",
                            contenido
                        );

                        // 8. Llamar a la fachada
                        MessageResponseDto messageResponse = chatFachada.enviarMensajeTexto(sendMessageDto, autorId);

                        // 9. Construir respuesta exitosa
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
                        // Error de validación (canal no existe, no es miembro, etc.)
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
                        // Error inesperado
                        System.err.println("Error al enviar mensaje: " + e.getMessage());
                        e.printStackTrace();
                        sendJsonResponse(handler, "enviarMensajeCanal", false, "Error interno del servidor al enviar mensaje", null);
                    }
                    break;

                case "solicitarhistorialcanal":
                case "obtenermensajescanal":
                    // 1. Extraer payload
                    Object historialDataObj = request.getPayload();
                    if (historialDataObj == null) {
                        sendJsonResponse(handler, "solicitarHistorialCanal", false, "Falta payload", null);
                        return;
                    }

                    // 2. Convertir a JSON y extraer campos
                    JsonObject historialJson = gson.toJsonTree(historialDataObj).getAsJsonObject();
                    String histCanalIdStr = historialJson.has("canalId") ? historialJson.get("canalId").getAsString() : null;
                    String histUsuarioIdStr = historialJson.has("usuarioId") ? historialJson.get("usuarioId").getAsString() : null;

                    // 3. Validar campos requeridos
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

                    try {
                        // 4. Convertir a UUIDs
                        UUID histCanalId = UUID.fromString(histCanalIdStr);
                        UUID histUsuarioId = UUID.fromString(histUsuarioIdStr);

                        // 5. Validar que el usuario autenticado coincida con el solicitante (seguridad)
                        if (!handler.getAuthenticatedUser().getUserId().equals(histUsuarioId)) {
                            sendJsonResponse(handler, "solicitarHistorialCanal", false, "No autorizado para ver este historial",
                                createErrorData("permisos", "Usuario no autorizado"));
                            return;
                        }

                        // 6. Llamar a la fachada
                        List<MessageResponseDto> mensajes = chatFachada.obtenerMensajesDeCanal(histCanalId, histUsuarioId);

                        // 7. Enriquecer mensajes de audio con Base64
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
                            
                            // Para mensajes de audio, codificar a Base64
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
                                // Para mensajes de texto, usar el contenido directamente
                                mensajeMap.put("content", mensaje.getContent());
                            }
                            
                            mensajesEnriquecidos.add(mensajeMap);
                        }

                        // 8. Construir respuesta exitosa
                        Map<String, Object> historialResponseData = new HashMap<>();
                        historialResponseData.put("mensajes", mensajesEnriquecidos);
                        historialResponseData.put("totalMensajes", mensajes.size());

                        sendJsonResponse(handler, "solicitarHistorialCanal", true, "Historial obtenido", historialResponseData);

                    } catch (IllegalArgumentException e) {
                        // Error de validación (canal no existe, no es miembro, etc.)
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
                        // Error inesperado
                        System.err.println("Error al obtener historial: " + e.getMessage());
                        e.printStackTrace();
                        sendJsonResponse(handler, "solicitarHistorialCanal", false, "Error interno del servidor al obtener historial", null);
                    }
                    break;

                case "listarmiembros":
                case "obtenermiembroscanal":
                    // 1. Extraer payload
                    Object miembrosDataObj = request.getPayload();
                    if (miembrosDataObj == null) {
                        sendJsonResponse(handler, "listarMiembros", false, "Falta payload", null);
                        return;
                    }

                    // 2. Convertir a JSON y extraer campos
                    JsonObject miembrosJson = gson.toJsonTree(miembrosDataObj).getAsJsonObject();
                    String miembrosCanalIdStr = miembrosJson.has("canalId") ? miembrosJson.get("canalId").getAsString() : null;
                    String solicitanteIdStr = miembrosJson.has("solicitanteId") ? miembrosJson.get("solicitanteId").getAsString() : null;

                    // 3. Validar campos requeridos
                    if (miembrosCanalIdStr == null || miembrosCanalIdStr.trim().isEmpty()) {
                        sendJsonResponse(handler, "listarMiembros", false, "El ID del canal es requerido",
                            createErrorData("canalId", "Campo requerido"));
                        return;
                    }

                    if (solicitanteIdStr == null || solicitanteIdStr.trim().isEmpty()) {
                        sendJsonResponse(handler, "listarMiembros", false, "El ID del solicitante es requerido",
                            createErrorData("solicitanteId", "Campo requerido"));
                        return;
                    }

                    try {
                        // 4. Convertir a UUIDs
                        UUID miembrosCanalId = UUID.fromString(miembrosCanalIdStr);
                        UUID solicitanteId = UUID.fromString(solicitanteIdStr);

                        // 5. Validar que el usuario autenticado coincida con el solicitante (seguridad)
                        if (!handler.getAuthenticatedUser().getUserId().equals(solicitanteId)) {
                            sendJsonResponse(handler, "listarMiembros", false, "No autorizado para ver estos miembros",
                                createErrorData("permisos", "Usuario no autorizado"));
                            return;
                        }

                        // 6. Llamar a la fachada
                        List<UserResponseDto> miembros = chatFachada.obtenerMiembrosDeCanal(miembrosCanalId, solicitanteId);

                        // 7. Construir lista de miembros para la respuesta
                        List<Map<String, Object>> miembrosData = new ArrayList<>();
                        
                        for (UserResponseDto miembro : miembros) {
                            Map<String, Object> miembroMap = new HashMap<>();
                            miembroMap.put("userId", miembro.getUserId().toString());
                            miembroMap.put("username", miembro.getUsername());
                            miembroMap.put("email", miembro.getEmail());
                            miembroMap.put("photoAddress", miembro.getPhotoAddress());
                            miembroMap.put("conectado", miembro.getEstado() != null ? miembro.getEstado() : "false");
                            miembroMap.put("rol", miembro.getRol() != null ? miembro.getRol() : "MIEMBRO");
                            
                            miembrosData.add(miembroMap);
                        }

                        // 8. Construir respuesta exitosa
                        Map<String, Object> miembrosResponseData = new HashMap<>();
                        miembrosResponseData.put("miembros", miembrosData);
                        miembrosResponseData.put("totalMiembros", miembros.size());
                        miembrosResponseData.put("canalId", miembrosCanalIdStr);

                        sendJsonResponse(handler, "listarMiembros", true, "Miembros obtenidos", miembrosResponseData);

                    } catch (IllegalArgumentException e) {
                        // Error de validación (canal no existe, no es miembro, etc.)
                        String errorMessage = e.getMessage();
                        String campo = "general";
                        
                        if (errorMessage.contains("Canal")) {
                            campo = "canalId";
                        } else if (errorMessage.contains("miembro")) {
                            campo = "permisos";
                        }
                        
                        sendJsonResponse(handler, "listarMiembros", false, errorMessage,
                            createErrorData(campo, errorMessage));
                            
                    } catch (Exception e) {
                        // Error inesperado
                        System.err.println("Error al listar miembros: " + e.getMessage());
                        e.printStackTrace();
                        sendJsonResponse(handler, "listarMiembros", false, "Error interno del servidor al listar miembros", null);
                    }
                    break;

                case "startfileupload": // Subida autenticada (ej. audio)
                case "uploadfileforregistration": // Subida pública (ej. foto de registro)
                {
                    DTOStartUpload payload = gson.fromJson(gson.toJsonTree(request.getPayload()), DTOStartUpload.class);
                    // ✅ ¡Llamada correcta a la Fachada!
                    String uploadId = chatFachada.startUpload(payload);
                    sendJsonResponse(handler, action, true, "Upload iniciado", Map.of("uploadId", uploadId));
                    break;
                }

                case "uploadfilechunk":
                {
                    DTOUploadChunk payload = gson.fromJson(gson.toJsonTree(request.getPayload()), DTOUploadChunk.class);
                    // ✅ ¡Llamada correcta a la Fachada!
                    chatFachada.processChunk(payload);

                    // Respuesta PUSH (ack dinámico, como dice la documentación)
                    String ackAction = "uploadFileChunk_" + payload.getUploadId() + "_" + payload.getChunkNumber();
                    sendJsonResponse(handler, ackAction, true, "Chunk " + payload.getChunkNumber() + " recibido", null);
                    break;
                }

                case "endfileupload":
                {
                    DTOEndUpload payload = gson.fromJson(gson.toJsonTree(request.getPayload()), DTOEndUpload.class);

                    String subDirectory;
                    UUID autorId;

                    if (handler.isAuthenticated()) {
                        autorId = handler.getAuthenticatedUser().getUserId();
                        subDirectory = "audio_files"; // O "user_files" si es genérico
                    } else {
                        // Es una subida de registro (sin auth)
                        autorId = UUID.fromString("00000000-0000-0000-0000-000000000000"); // ID temporal/genérico
                        subDirectory = "user_photos"; // Guardar en fotos de perfil
                    }

                    // ✅ ¡Llamada correcta a la Fachada!
                    FileUploadResponse responseDataLocal = chatFachada.endUpload(payload, autorId, subDirectory);
                    sendJsonResponse(handler, action, true , "Archivo subido", responseDataLocal);
                    break;
                }

                // --- INICIO DE ACCIONES DE DESCARGA (Para el Login) ---
                case "startfiledownload":
                {
                    // El payload es { "fileId": "ruta/archivo.jpg" }
                    JsonObject payloadJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
                    String fileId = payloadJson.get("fileId").getAsString();

                    // ✅ ¡Llamada correcta a la Fachada!
                    DTODownloadInfo info = chatFachada.startDownload(fileId);
                    sendJsonResponse(handler, action, true, "Descarga iniciada", info);
                    break;
                }

                case "requestfilechunk":
                {
                    // El payload es { "downloadId": "...", "chunkNumber": N }
                    JsonObject payloadJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
                    String downloadId = payloadJson.get("downloadId").getAsString();
                    int chunkNumber = payloadJson.get("chunkNumber").getAsInt();

                    // ✅ ¡Llamada correcta a la Fachada!
                    byte[] chunkBytes = chatFachada.getChunk(downloadId, chunkNumber);
                    String chunkBase64 = Base64.getEncoder().encodeToString(chunkBytes);

                    Map<String, Object> chunkData = new HashMap<>();
                    chunkData.put("chunkNumber", chunkNumber);
                    chunkData.put("chunkDataBase64", chunkBase64); // La doc del cliente usa 'chunkDataBase64'

                    // Respuesta PUSH (chunk dinámico)
                    String pushAction = "downloadFileChunk_" + downloadId + "_" + chunkNumber;
                    sendJsonResponse(handler, pushAction, true, "Enviando chunk", chunkData);
                    break;
                }

                default:
                    sendJsonResponse(handler, action, false, "Comando desconocido: " + action, null);
                    break;
            }
        } catch (Exception e) {
            // Error interno del servidor
            sendJsonResponse(handler, action, false, "Error interno del servidor", null);
            e.printStackTrace();
        }
    }

    public MessageResponseDto enrichOutgoingMessage(MessageResponseDto originalDto) {
        if ("AUDIO".equals(originalDto.getMessageType())) {
            try {
                // Esta llamada (Controlador -> Fachada) es VÁLIDA.
                String base64Content = chatFachada.getFileAsBase64(originalDto.getContent());

                // Devolver un DTO *nuevo* con el contenido Base64
                return new MessageResponseDto(
                        originalDto.getMessageId(),
                        originalDto.getChannelId(),
                        originalDto.getAuthor(),
                        originalDto.getTimestamp(),
                        originalDto.getMessageType(),
                        base64Content
                );
            } catch (Exception e) {
                // Aquí deberíamos usar el logger
                System.err.println("Error al leer y codificar el archivo de audio para propagación: " + e.getMessage());
                // Devuelve el DTO original si falla la codificación
                return originalDto;
            }
        }
        // Si no es audio, devolver el original
        return originalDto;
    }

    // Método para crear data de error con campo y motivo
    private Map<String, String> createErrorData(String campo, String motivo) {
        Map<String, String> errorData = new HashMap<>();
        errorData.put("campo", campo);
        errorData.put("motivo", motivo);
        return errorData;
    }

    // Método actualizado para enviar respuestas JSON con 'success' booleano
    private void sendJsonResponse(IClientHandler handler, String action, boolean success, String message, Object data) {
        String status = success ? "success" : "error";
        DTOResponse response = new DTOResponse(action, status, message, data);
        String jsonResponse = gson.toJson(response);
        handler.sendMessage(jsonResponse);
    }
}
