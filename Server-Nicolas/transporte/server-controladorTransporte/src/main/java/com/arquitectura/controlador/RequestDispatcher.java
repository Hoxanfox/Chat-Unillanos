package com.arquitectura.controlador;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RequestDispatcher {

    private final IChatFachada chatFachada;
    private final Gson gson;
    private UserResponseDto authenticatedUser = null;

    @Autowired
    public RequestDispatcher(IChatFachada chatFachada, Gson gson) {
        this.chatFachada = chatFachada;
        this.gson = gson;
    }

    public void dispatch(String requestJson ,IClientHandler handler)  {
        DTORequest request;
        String action = "unknown";
        try {
            request = gson.fromJson(requestJson, DTORequest.class);
            action = request.getAction() != null ? request.getAction().toLowerCase() : "unknown";
            // 2. Validar sesión
            if (!action.equals("authenticateuser") && !handler.isAuthenticated()) {
                sendJsonResponse(handler, action, "error", "Debes iniciar sesión para realizar esta acción.", null);
                return;
            }
            switch (action) {
                case "authenticateuser":
                    Object payloadObj = request.getPayload();
                    if (payloadObj == null) {
                        sendJsonResponse(handler, "login", "error", "Falta payload.", null);
                        return;
                    }
                    // Convertir el payload a JSON y luego a LoginRequestDto
                    JsonObject payloadJson = gson.toJsonTree(payloadObj).getAsJsonObject();
                    String username = payloadJson.has("username") ? payloadJson.get("username").getAsString() : null;
                    String password = payloadJson.has("password") ? payloadJson.get("password").getAsString() : null;

                    if (username == null || password == null) {
                        // Enviar error: faltan campos en payload
                        sendJsonResponse(handler, "login", "error", "Payload debe contener 'username' y 'password'.", null);
                        return;
                    }
                    LoginRequestDto serverLoginDto = new LoginRequestDto(username, password);
                    UserResponseDto userDto = chatFachada.autenticarUsuario(serverLoginDto, handler.getClientIpAddress());

                    // Autenticación exitosa
                    handler.setAuthenticatedUser(userDto);

                    //construccion de la respuesta
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("id", userDto.getUserId().toString()); // ¡Correcto! Envía el UUID como String
                    responseData.put("nombre", userDto.getUsername());
                    responseData.put("email", userDto.getEmail());
                    responseData.put("photoId", userDto.getPhotoAddress());
                    responseData.put("estado", mapearEstadoParaCliente(userDto.getEstado()));
                    responseData.put("fechaRegistro", userDto.getFechaRegistro());
                    sendJsonResponse(handler, "login", "success", "Autenticación exitosa", responseData);
                    break;
                case "logout": // O "logoutUser"
                    if (handler.isAuthenticated()) {
                        UUID publicId = handler.getAuthenticatedUser().getUserId(); // Obtiene UUID
                        String loggedOutUsername = handler.getAuthenticatedUser().getUsername();

                        chatFachada.cambiarEstadoUsuario(publicId,false);
                        handler.clearAuthenticatedUser();

                        sendJsonResponse(handler, "logoutUser", "success", "Sesión de " + loggedOutUsername + " cerrada.", null);
                    } else {
                        sendJsonResponse(handler, "logoutUser", "error", "No hay sesión activa.", null);
                    }
                    break;

                default:
                    handler.sendMessage("ERROR;Comando desconocido: " + action);
                    break;

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    //clases para gson
    private void sendJsonResponse(IClientHandler handler, String action, String status, String message, Object data) {
        // Necesitarás una clase DTOResponse en el servidor similar a la del cliente
        DTOResponse response = new DTOResponse(action, status, message, data);
        String jsonResponse = gson.toJson(response);
        handler.sendMessage(jsonResponse);
    }
    public static class DTOResponse {
        private String action; private String status; private String message; private Object data;
        public DTOResponse(String a, String s, String m, Object d){this.action=a; this.status=s; this.message=m; this.data=d;}
    }
    //metodos para el mapeo de estados
    private String mapearEstadoParaCliente(String estadoServidor) {
        if (estadoServidor == null) return "inactivo";
        switch (estadoServidor.toUpperCase()) {
            case "ONLINE": return "activo";
            case "OFFLINE": return "inactivo";
            default: return "inactivo";
        }
    }
    private String mapearEstadoParaClienteLista(String estadoServidor) {
        if (estadoServidor == null) return "Offline";
        switch (estadoServidor.toUpperCase()) {
            case "ONLINE": return "Online";
            case "OFFLINE":
                return "Offline";
            default:
                return "Offline";
        }
    }




}


