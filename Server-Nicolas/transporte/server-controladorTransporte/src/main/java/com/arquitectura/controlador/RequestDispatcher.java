package com.arquitectura.controlador;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class RequestDispatcher {

    private final IChatFachada chatFachada;
    private final Gson gson;
    private static final Set<String> ACCIONES_PUBLICAS = Set.of(
            "authenticateuser"
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
                    Object dataObj = request.getData();
                    if (dataObj == null) {
                        sendJsonResponse(handler, "authenticateUser", false, "Falta data.", null);
                        return;
                    }

                    // Convertir el data a JSON y luego a LoginRequestDto
                    JsonObject dataJson = gson.toJsonTree(dataObj).getAsJsonObject();
                    String nombreUsuario = dataJson.has("nombreUsuario") ? dataJson.get("nombreUsuario").getAsString() : null;
                    String password = dataJson.has("password") ? dataJson.get("password").getAsString() : null;

                    if (nombreUsuario == null || password == null) {
                        sendJsonResponse(handler, "authenticateUser", false, "Email o contraseña inválidos",
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
                    responseData.put("imagenBase64", userDto.getImagenBase64());

                    sendJsonResponse(handler, "authenticateUser", true, "Autenticación exitosa", responseData);
                    break;

                case "logoutuser":
                    // Ya no necesitamos validar isAuthenticated() aquí porque se valida arriba
                    Object logoutDataObj = request.getData();
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

    // Método para crear data de error con campo y motivo
    private Map<String, String> createErrorData(String campo, String motivo) {
        Map<String, String> errorData = new HashMap<>();
        errorData.put("campo", campo);
        errorData.put("motivo", motivo);
        return errorData;
    }

    // Método actualizado para enviar respuestas JSON con 'success' booleano
    private void sendJsonResponse(IClientHandler handler, String action, boolean success, String message, Object data) {
        DTOResponse response = new DTOResponse(action, success, message, data);
        String jsonResponse = gson.toJson(response);
        handler.sendMessage(jsonResponse);
    }

    // Clase DTOResponse actualizada con 'success' booleano
    public static class DTOResponse {
        private final String action;
        private final boolean success;
        private final String message;
        private final Object data;

        public DTOResponse(String action, boolean success, String message, Object data) {
            this.action = action;
            this.success = success;
            this.message = message;
            this.data = data;
        }
    }
}
