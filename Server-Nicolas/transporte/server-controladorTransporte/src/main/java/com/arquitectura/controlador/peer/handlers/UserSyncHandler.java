package com.arquitectura.controlador.peer.handlers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Handler para sincronización de usuarios
 */
@Component
public class UserSyncHandler {

    private final IChatFachada chatFachada;
    private final Gson gson;
    private final PeerResponseHelper responseHelper;

    public UserSyncHandler(IChatFachada chatFachada, Gson gson, PeerResponseHelper responseHelper) {
        this.chatFachada = chatFachada;
        this.gson = gson;
        this.responseHelper = responseHelper;
    }

    public void handleSincronizarUsuarios(DTORequest request, IClientHandler handler) {
        System.out.println("→ [UserSyncHandler] Procesando sincronizarUsuarios");

        try {
            List<UserResponseDto> usuariosLocales = chatFachada.usuarios().obtenerTodosLosUsuarios();
            UUID peerActualId = chatFachada.p2p().obtenerPeerActualId();
            com.arquitectura.DTO.p2p.PeerResponseDto peerActual = chatFachada.p2p().obtenerPeer(peerActualId);

            UserSyncResult result = buildUserSyncData(usuariosLocales, peerActual);
            Map<String, Object> responseData = buildSyncResponse(result);

            System.out.println("✓ [UserSyncHandler] Sincronización completada: " +
                result.totalUsuarios + " usuarios locales (" + result.usuariosConectados + " conectados)");

            responseHelper.sendSuccess(handler, "sincronizarUsuarios", "Usuarios sincronizados exitosamente", responseData);

        } catch (Exception e) {
            System.err.println("✗ [UserSyncHandler] Error: " + e.getMessage());
            responseHelper.sendError(handler, "sincronizarUsuarios", "Error al sincronizar usuarios: " + e.getMessage(), null);
        }
    }

    public void handleNotificarCambioEstado(DTORequest request, IClientHandler handler) {
        System.out.println("→ [UserSyncHandler] Procesando notificarCambioEstado");

        if (!validatePayload(request.getPayload(), handler)) return;

        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            StateChangeData data = extractStateChangeData(payload, handler);
            if (data == null) return;

            UserResponseDto usuario = findUser(data.usuarioId, handler);
            if (usuario == null) return;

            String estadoAnterior = usuario.getEstado() != null ? usuario.getEstado() : "OFFLINE";
            chatFachada.usuarios().cambiarEstadoUsuario(data.usuarioId, data.nuevoEstado.equals("ONLINE"));

            Map<String, Object> responseData = buildStateChangeResponse(usuario, estadoAnterior, data, payload);

            System.out.println("✓ [UserSyncHandler] Estado cambiado: " + estadoAnterior + " → " + data.nuevoEstado);
            responseHelper.sendSuccess(handler, "notificarCambioEstado", "Cambio de estado notificado exitosamente", responseData);

        } catch (Exception e) {
            System.err.println("✗ [UserSyncHandler] Error: " + e.getMessage());
            handleStateChangeError(e, handler);
        }
    }

    public void handleNotificacionCambioUsuario(DTORequest request, IClientHandler handler) {
        System.out.println("→ [UserSyncHandler] Procesando notificacionCambioUsuario (PUSH)");

        try {
            responseHelper.sendSuccess(handler, "notificacionCambioUsuario", "Notificación recibida", null);
        } catch (Exception e) {
            System.err.println("✗ [UserSyncHandler] Error: " + e.getMessage());
            responseHelper.sendError(handler, "notificacionCambioUsuario", "Error al procesar notificación", null);
        }
    }

    private UserSyncResult buildUserSyncData(List<UserResponseDto> usuarios,
                                             com.arquitectura.DTO.p2p.PeerResponseDto peerActual) {
        UserSyncResult result = new UserSyncResult();
        Map<String, Map<String, Object>> mapaUsuarios = new HashMap<>();

        for (UserResponseDto usuario : usuarios) {
            Map<String, Object> usuarioMap = new HashMap<>();
            usuarioMap.put("usuarioId", usuario.getUserId().toString());
            usuarioMap.put("username", usuario.getUsername());

            boolean conectado = "ONLINE".equalsIgnoreCase(usuario.getEstado());
            usuarioMap.put("conectado", conectado);

            if (conectado) {
                result.usuariosConectados++;
                usuarioMap.put("peerId", peerActual.getPeerId().toString());
                usuarioMap.put("peerIp", peerActual.getIp());
                usuarioMap.put("peerPuerto", peerActual.getPuerto());
            } else {
                usuarioMap.put("peerId", null);
                usuarioMap.put("peerIp", null);
                usuarioMap.put("peerPuerto", null);
            }

            mapaUsuarios.put(usuario.getUserId().toString(), usuarioMap);
        }

        result.usuariosData = new ArrayList<>(mapaUsuarios.values());
        result.totalUsuarios = result.usuariosData.size();

        return result;
    }

    private Map<String, Object> buildSyncResponse(UserSyncResult result) {
        String fechaSincronizacion = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("usuarios", result.usuariosData);
        responseData.put("totalUsuarios", result.totalUsuarios);
        responseData.put("usuariosConectados", result.usuariosConectados);
        responseData.put("fechaSincronizacion", fechaSincronizacion);

        return responseData;
    }

    private StateChangeData extractStateChangeData(JsonObject payload, IClientHandler handler) {
        if (!payload.has("usuarioId") || !payload.has("nuevoEstado")) {
            responseHelper.sendError(handler, "notificarCambioEstado", "Faltan campos requeridos",
                Map.of("campo", "usuarioId/nuevoEstado", "motivo", "Campos requeridos"));
            return null;
        }

        String nuevoEstado = payload.get("nuevoEstado").getAsString().toUpperCase();

        if (!nuevoEstado.equals("ONLINE") && !nuevoEstado.equals("OFFLINE")) {
            responseHelper.sendError(handler, "notificarCambioEstado", "Estado inválido",
                Map.of("campo", "nuevoEstado", "motivo", "El estado debe ser ONLINE u OFFLINE"));
            return null;
        }

        try {
            UUID usuarioId = UUID.fromString(payload.get("usuarioId").getAsString());
            return new StateChangeData(usuarioId, nuevoEstado);
        } catch (IllegalArgumentException e) {
            responseHelper.sendError(handler, "notificarCambioEstado", "Formato de UUID inválido",
                Map.of("campo", "usuarioId", "motivo", "Formato UUID inválido"));
            return null;
        }
    }

    private UserResponseDto findUser(UUID usuarioId, IClientHandler handler) {
        List<UserResponseDto> todosUsuarios = chatFachada.usuarios().obtenerTodosLosUsuarios();

        for (UserResponseDto u : todosUsuarios) {
            if (u.getUserId().equals(usuarioId)) {
                return u;
            }
        }

        responseHelper.sendError(handler, "notificarCambioEstado", "Usuario no encontrado",
            Map.of("usuarioId", usuarioId.toString(), "motivo", "El usuario no existe"));
        return null;
    }

    private Map<String, Object> buildStateChangeResponse(UserResponseDto usuario, String estadoAnterior,
                                                          StateChangeData data, JsonObject payload) {
        String fechaCambio = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("usuarioId", usuario.getUserId().toString());
        responseData.put("username", usuario.getUsername());
        responseData.put("estadoAnterior", estadoAnterior);
        responseData.put("estadoNuevo", data.nuevoEstado);
        responseData.put("fechaCambio", fechaCambio);

        if (data.nuevoEstado.equals("ONLINE") && payload.has("peerId")) {
            try {
                UUID peerId = UUID.fromString(payload.get("peerId").getAsString());
                responseData.put("peerId", peerId.toString());
                if (payload.has("peerIp")) responseData.put("peerIp", payload.get("peerIp").getAsString());
                if (payload.has("peerPuerto")) responseData.put("peerPuerto", payload.get("peerPuerto").getAsInt());
            } catch (Exception e) {
                responseData.put("peerId", null);
                responseData.put("peerIp", null);
                responseData.put("peerPuerto", null);
            }
        } else {
            responseData.put("peerId", null);
            responseData.put("peerIp", null);
            responseData.put("peerPuerto", null);
        }

        return responseData;
    }

    private void handleStateChangeError(Exception e, IClientHandler handler) {
        if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
            responseHelper.sendError(handler, "notificarCambioEstado", "Usuario no encontrado", Map.of("usuarioId", "DESCONOCIDO"));
        } else {
            responseHelper.sendError(handler, "notificarCambioEstado", "Error al notificar cambio de estado", null);
        }
    }

    private boolean validatePayload(Object payload, IClientHandler handler) {
        if (payload == null) {
            responseHelper.sendError(handler, "notificarCambioEstado", "Falta payload", null);
            return false;
        }
        return true;
    }

    private static class UserSyncResult {
        List<Map<String, Object>> usuariosData;
        int totalUsuarios;
        int usuariosConectados = 0;
    }

    private static class StateChangeData {
        UUID usuarioId;
        String nuevoEstado;

        StateChangeData(UUID usuarioId, String nuevoEstado) {
            this.usuarioId = usuarioId;
            this.nuevoEstado = nuevoEstado;
        }
    }
}

