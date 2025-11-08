package com.arquitectura.controlador.controllers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.canales.InviteMemberRequestDto;
import com.arquitectura.DTO.canales.RespondToInviteRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Controlador para operaciones relacionadas con canales:
 * - Listar canales
 * - Crear canales directos
 * - Invitar miembros
 * - Responder invitaciones
 * - Obtener invitaciones
 * - Listar miembros
 */
@Component
public class ChannelController extends BaseController {
    
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
        "listarcanales",
        "crearcanal",
        "crearcanaldirecto",
        "iniciarchat",
        "obtenerchatprivado",
        "invitarmiembro",
        "invitarusuario",
        "responderinvitacion",
        "aceptarinvitacion",
        "rechazarinvitacion",
        "obtenerinvitaciones",
        "listarinvitaciones",
        "invitacionespendientes",
        "listarmiembros",
        "obtenermiembroscanal"
    );
    
    @Autowired
    public ChannelController(IChatFachada chatFachada, Gson gson) {
        super(chatFachada, gson);
    }
    
    @Override
    public boolean handleAction(String action, DTORequest request, IClientHandler handler) {
        if (!SUPPORTED_ACTIONS.contains(action.toLowerCase())) {
            return false;
        }
        
        switch (action.toLowerCase()) {
            case "listarcanales":
                handleListChannels(request, handler);
                break;
            case "crearcanal":
                handleCreateChannel(request, handler);
                break;
            case "crearcanaldirecto":
            case "iniciarchat":
            case "obtenerchatprivado":
                handleCreateDirectChannel(request, handler);
                break;
            case "invitarmiembro":
            case "invitarusuario":
                handleInviteMember(request, handler);
                break;
            case "responderinvitacion":
            case "aceptarinvitacion":
            case "rechazarinvitacion":
                handleRespondInvitation(request, handler);
                break;
            case "obtenerinvitaciones":
            case "listarinvitaciones":
            case "invitacionespendientes":
                handleGetInvitations(request, handler);
                break;
            case "listarmiembros":
            case "obtenermiembroscanal":
                handleListMembers(request, handler);
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
    
    private void handleListChannels(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "listarCanales")) {
            return;
        }

        try {
            JsonObject listarCanalesJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String canalesUsuarioIdStr = listarCanalesJson.has("usuarioId") ? listarCanalesJson.get("usuarioId").getAsString() : null;

            if (canalesUsuarioIdStr == null || canalesUsuarioIdStr.isEmpty()) {
                sendJsonResponse(handler, "listarCanales", false, "Error al listar canales: usuarioId requerido", null);
                return;
            }

            UUID canalesUsuarioId = UUID.fromString(canalesUsuarioIdStr);

            if (!handler.getAuthenticatedUser().getUserId().equals(canalesUsuarioId)) {
                sendJsonResponse(handler, "listarCanales", false, "Usuario no autorizado para ver esta lista", null);
                return;
            }

            List<ChannelResponseDto> canales = chatFachada.obtenerCanalesPorUsuario(canalesUsuarioId);

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
            System.err.println("Error al listar canales: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "listarCanales", false, "Error al listar canales: " + e.getMessage(), null);
        }
    }
    
    private void handleCreateDirectChannel(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "crearCanalDirecto")) {
            return;
        }

        try {
            JsonObject directJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String user1IdStr = directJson.has("user1Id") ? directJson.get("user1Id").getAsString() : null;
            String user2IdStr = directJson.has("user2Id") ? directJson.get("user2Id").getAsString() : null;

            System.out.println("→ [ChannelController] Solicitud crearCanalDirecto recibida");
            System.out.println("  user1Id: " + user1IdStr);
            System.out.println("  user2Id: " + user2IdStr);

            if (user1IdStr == null || user1IdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "crearCanalDirecto", false, "El ID del primer usuario es requerido",
                    createErrorData("user1Id", "Campo requerido"));
                return;
            }

            if (user2IdStr == null || user2IdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "crearCanalDirecto", false, "El ID del segundo usuario es requerido",
                    createErrorData("user2Id", "Campo requerido"));
                return;
            }

            UUID user1Id = UUID.fromString(user1IdStr);
            UUID user2Id = UUID.fromString(user2IdStr);
            UUID authenticatedUserId = handler.getAuthenticatedUser().getUserId();
            
            if (!authenticatedUserId.equals(user1Id) && !authenticatedUserId.equals(user2Id)) {
                sendJsonResponse(handler, "crearCanalDirecto", false, "No autorizado para crear este canal",
                    createErrorData("permisos", "Usuario no autorizado"));
                return;
            }

            System.out.println("→ [ChannelController] Llamando a chatFachada.crearCanalDirecto...");
            ChannelResponseDto canalDirecto = chatFachada.crearCanalDirecto(user1Id, user2Id);
            System.out.println("✓ [ChannelController] Canal obtenido: " + canalDirecto.getChannelId());
            
            UUID otherUserId = authenticatedUserId.equals(user1Id) ? user2Id : user1Id;
            List<UserResponseDto> otherUsers = chatFachada.getUsersByIds(Set.of(otherUserId));

            // Formato compatible con el cliente (similar a crearCanal)
            Map<String, Object> directResponseData = new HashMap<>();
            directResponseData.put("id", canalDirecto.getChannelId().toString());
            directResponseData.put("nombre", canalDirecto.getChannelName());
            directResponseData.put("creadorId", canalDirecto.getOwner().getUserId().toString());
            directResponseData.put("tipo", canalDirecto.getChannelType());
            
            // Información adicional para compatibilidad
            directResponseData.put("channelId", canalDirecto.getChannelId().toString());
            directResponseData.put("channelName", canalDirecto.getChannelName());
            directResponseData.put("channelType", canalDirecto.getChannelType());
            directResponseData.put("owner", Map.of(
                "userId", canalDirecto.getOwner().getUserId().toString(),
                "username", canalDirecto.getOwner().getUsername()
            ));
            if (canalDirecto.getPeerId() != null) {
                directResponseData.put("peerId", canalDirecto.getPeerId().toString());
            }

            if (!otherUsers.isEmpty()) {
                UserResponseDto otherUser = otherUsers.get(0);
                directResponseData.put("otherUser", Map.of(
                    "userId", otherUser.getUserId().toString(),
                    "username", otherUser.getUsername(),
                    "email", otherUser.getEmail(),
                    "photoAddress", otherUser.getPhotoAddress() != null ? otherUser.getPhotoAddress() : "",
                    "conectado", otherUser.getEstado() != null ? otherUser.getEstado() : "false"
                ));
            }

            System.out.println("✓ [ChannelController] Enviando respuesta exitosa al cliente");
            System.out.println("  Response data: " + gson.toJson(directResponseData));
            sendJsonResponse(handler, "crearCanalDirecto", true, "Canal directo creado/obtenido exitosamente", directResponseData);
            System.out.println("✓ [ChannelController] Respuesta enviada");

        } catch (Exception e) {
            System.err.println("✗ [ChannelController] Error al crear canal directo: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "crearCanalDirecto", false, "Error interno del servidor al crear canal directo", null);
        }
    }
    
    private void handleInviteMember(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "invitarMiembro")) {
            return;
        }

        try {
            JsonObject inviteJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String inviteChannelIdStr = inviteJson.has("channelId") ? inviteJson.get("channelId").getAsString() : null;
            String inviteUserIdStr = inviteJson.has("userIdToInvite") ? inviteJson.get("userIdToInvite").getAsString() : null;

            if (inviteChannelIdStr == null || inviteChannelIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "invitarMiembro", false, "El ID del canal es requerido",
                    createErrorData("channelId", "Campo requerido"));
                return;
            }

            if (inviteUserIdStr == null || inviteUserIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "invitarMiembro", false, "El ID del usuario a invitar es requerido",
                    createErrorData("userIdToInvite", "Campo requerido"));
                return;
            }

            UUID inviteChannelId = UUID.fromString(inviteChannelIdStr);
            UUID inviteUserId = UUID.fromString(inviteUserIdStr);
            UUID ownerId = handler.getAuthenticatedUser().getUserId();

            InviteMemberRequestDto inviteDto = new InviteMemberRequestDto(inviteChannelId, inviteUserId);
            chatFachada.invitarMiembro(inviteDto, ownerId);

            List<UserResponseDto> invitedUsers = chatFachada.getUsersByIds(Set.of(inviteUserId));
            
            Map<String, Object> inviteResponseData = new HashMap<>();
            inviteResponseData.put("channelId", inviteChannelIdStr);
            inviteResponseData.put("invitedUserId", inviteUserIdStr);
            if (!invitedUsers.isEmpty()) {
                inviteResponseData.put("invitedUsername", invitedUsers.get(0).getUsername());
            }

            sendJsonResponse(handler, "invitarMiembro", true, "Invitación enviada exitosamente", inviteResponseData);

        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            String campo = "general";
            
            if (errorMessage.contains("Canal")) {
                campo = "channelId";
            } else if (errorMessage.contains("propietario") || errorMessage.contains("owner")) {
                campo = "permisos";
            } else if (errorMessage.contains("Usuario") || errorMessage.contains("usuario")) {
                campo = "userIdToInvite";
            } else if (errorMessage.contains("miembro") || errorMessage.contains("invitación")) {
                campo = "membresía";
            }
            
            sendJsonResponse(handler, "invitarMiembro", false, errorMessage,
                createErrorData(campo, errorMessage));
                
        } catch (Exception e) {
            System.err.println("Error al invitar miembro: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "invitarMiembro", false, "Error interno del servidor al invitar miembro", null);
        }
    }
    
    private void handleRespondInvitation(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "responderInvitacion")) {
            return;
        }

        try {
            JsonObject respondJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String respondChannelIdStr = respondJson.has("channelId") ? respondJson.get("channelId").getAsString() : null;
            Boolean accepted = respondJson.has("accepted") ? respondJson.get("accepted").getAsBoolean() : null;

            if (respondChannelIdStr == null || respondChannelIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "responderInvitacion", false, "El ID del canal es requerido",
                    createErrorData("channelId", "Campo requerido"));
                return;
            }

            if (accepted == null) {
                sendJsonResponse(handler, "responderInvitacion", false, "Debes indicar si aceptas o rechazas la invitación",
                    createErrorData("accepted", "Campo requerido"));
                return;
            }

            UUID respondChannelId = UUID.fromString(respondChannelIdStr);
            UUID userId = handler.getAuthenticatedUser().getUserId();

            RespondToInviteRequestDto respondDto = new RespondToInviteRequestDto(respondChannelId, accepted);
            chatFachada.responderInvitacion(respondDto, userId);

            Map<String, Object> respondResponseData = new HashMap<>();
            respondResponseData.put("channelId", respondChannelIdStr);
            respondResponseData.put("accepted", accepted);

            String message = accepted ? 
                "Invitación aceptada. Ahora eres miembro del canal" : 
                "Invitación rechazada";

            sendJsonResponse(handler, "responderInvitacion", true, message, respondResponseData);

        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            String campo = "general";
            
            if (errorMessage.contains("Canal")) {
                campo = "channelId";
            } else if (errorMessage.contains("invitación")) {
                campo = "invitación";
            }
            
            sendJsonResponse(handler, "responderInvitacion", false, errorMessage,
                createErrorData(campo, errorMessage));
                
        } catch (Exception e) {
            System.err.println("Error al responder invitación: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "responderInvitacion", false, "Error interno del servidor al responder invitación", null);
        }
    }
    
    private void handleGetInvitations(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "obtenerInvitaciones")) {
            return;
        }

        try {
            JsonObject invitacionesJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String invitUsuarioIdStr = invitacionesJson.has("usuarioId") ? invitacionesJson.get("usuarioId").getAsString() : null;

            if (invitUsuarioIdStr == null || invitUsuarioIdStr.trim().isEmpty()) {
                sendJsonResponse(handler, "obtenerInvitaciones", false, "El ID del usuario es requerido",
                    createErrorData("usuarioId", "Campo requerido"));
                return;
            }

            UUID invitUsuarioId = UUID.fromString(invitUsuarioIdStr);

            if (!handler.getAuthenticatedUser().getUserId().equals(invitUsuarioId)) {
                sendJsonResponse(handler, "obtenerInvitaciones", false, "No autorizado para ver estas invitaciones",
                    createErrorData("permisos", "Usuario no autorizado"));
                return;
            }

            List<ChannelResponseDto> invitaciones = chatFachada.getPendingInvitationsForUser(invitUsuarioId);
            
            List<Map<String, Object>> invitacionesData = new ArrayList<>();
            for (ChannelResponseDto canal : invitaciones) {
                Map<String, Object> canalMap = new HashMap<>();
                canalMap.put("channelId", canal.getChannelId().toString());
                canalMap.put("channelName", canal.getChannelName());
                canalMap.put("channelType", canal.getChannelType());
                
                if (canal.getOwner() != null) {
                    canalMap.put("owner", Map.of(
                        "userId", canal.getOwner().getUserId().toString(),
                        "username", canal.getOwner().getUsername()
                    ));
                }
                
                if (canal.getPeerId() != null) {
                    canalMap.put("peerId", canal.getPeerId().toString());
                }
                
                invitacionesData.add(canalMap);
            }

            Map<String, Object> invitacionesResponseData = new HashMap<>();
            invitacionesResponseData.put("invitaciones", invitacionesData);
            invitacionesResponseData.put("totalInvitaciones", invitaciones.size());

            sendJsonResponse(handler, "obtenerInvitaciones", true, "Invitaciones obtenidas", invitacionesResponseData);

        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            sendJsonResponse(handler, "obtenerInvitaciones", false, errorMessage,
                createErrorData("general", errorMessage));
                
        } catch (Exception e) {
            System.err.println("Error al obtener invitaciones: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "obtenerInvitaciones", false, "Error interno del servidor al obtener invitaciones", null);
        }
    }
    
    private void handleListMembers(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "listarMiembros")) {
            return;
        }

        try {
            JsonObject miembrosJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String miembrosCanalIdStr = miembrosJson.has("canalId") ? miembrosJson.get("canalId").getAsString() : null;
            String solicitanteIdStr = miembrosJson.has("solicitanteId") ? miembrosJson.get("solicitanteId").getAsString() : null;

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

            UUID miembrosCanalId = UUID.fromString(miembrosCanalIdStr);
            UUID solicitanteId = UUID.fromString(solicitanteIdStr);

            if (!handler.getAuthenticatedUser().getUserId().equals(solicitanteId)) {
                sendJsonResponse(handler, "listarMiembros", false, "No autorizado para ver estos miembros",
                    createErrorData("permisos", "Usuario no autorizado"));
                return;
            }

            List<UserResponseDto> miembros = chatFachada.obtenerMiembrosDeCanal(miembrosCanalId, solicitanteId);
            
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
            
            Map<String, Object> miembrosResponseData = new HashMap<>();
            miembrosResponseData.put("miembros", miembrosData);
            miembrosResponseData.put("totalMiembros", miembros.size());
            miembrosResponseData.put("canalId", miembrosCanalIdStr);

            sendJsonResponse(handler, "listarMiembros", true, "Miembros obtenidos", miembrosResponseData);

        } catch (IllegalArgumentException e) {
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
            System.err.println("Error al listar miembros: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "listarMiembros", false, "Error interno del servidor al listar miembros", null);
        }
    }
    
    private void handleCreateChannel(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "crearCanal")) {
            return;
        }

        try {
            JsonObject canalJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String nombre = canalJson.has("nombre") ? canalJson.get("nombre").getAsString() : null;
            String tipo = canalJson.has("tipo") ? canalJson.get("tipo").getAsString() : "GRUPO";

            if (nombre == null || nombre.trim().isEmpty()) {
                sendJsonResponse(handler, "crearCanal", false, "El nombre del canal es requerido",
                    createErrorData("nombre", "Campo requerido"));
                return;
            }

            UUID ownerId = handler.getAuthenticatedUser().getUserId();
            com.arquitectura.DTO.canales.CreateChannelRequestDto createDto = 
                new com.arquitectura.DTO.canales.CreateChannelRequestDto(nombre, tipo);
            ChannelResponseDto nuevoCanal = chatFachada.crearCanal(createDto, ownerId);

            // Formato compatible con el cliente
            Map<String, Object> canalData = new HashMap<>();
            canalData.put("id", nuevoCanal.getChannelId().toString());
            canalData.put("nombre", nuevoCanal.getChannelName());
            canalData.put("creadorId", nuevoCanal.getOwner().getUserId().toString());
            canalData.put("tipo", nuevoCanal.getChannelType());
            
            // Información adicional para compatibilidad
            canalData.put("channelId", nuevoCanal.getChannelId().toString());
            canalData.put("channelName", nuevoCanal.getChannelName());
            canalData.put("channelType", nuevoCanal.getChannelType());
            canalData.put("owner", Map.of(
                "userId", nuevoCanal.getOwner().getUserId().toString(),
                "username", nuevoCanal.getOwner().getUsername()
            ));
            if (nuevoCanal.getPeerId() != null) {
                canalData.put("peerId", nuevoCanal.getPeerId().toString());
            }

            sendJsonResponse(handler, "crearCanal", true, "Canal creado exitosamente", canalData);

        } catch (Exception e) {
            System.err.println("Error al crear canal: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "crearCanal", false, "Error interno del servidor al crear canal", null);
        }
    }
}
