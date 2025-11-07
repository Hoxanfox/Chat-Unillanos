package com.arquitectura.controlador.controllers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Controlador para operaciones P2P (Peer-to-Peer):
 * - Agregar peers a la red
 * - Listar peers disponibles
 * - Reportar heartbeats
 * - Retransmitir peticiones entre peers
 * - Actualizar lista de peers
 */
@Component
public class PeerController extends BaseController {
    
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
        // "añadirpeer",  // DESHABILITADO: Los peers se registran automáticamente via bootstrap
        "listarPeersDisponibles",
        "reportarlatido",
        "retransmitirpeticion",
        "buscarusuario",
        "enrutarmensaje",
        "descubrirpeers",
        "sincronizarusuarios",
        "notificarcambioestado",
        "verificarconexion",
        "ping",
        "obtenerestadored",
        "sincronizarcanales"
    );
    
    @Autowired
    public PeerController(IChatFachada chatFachada, Gson gson) {
        super(chatFachada, gson);
    }
    
    @Override
    public boolean handleAction(String action, DTORequest request, IClientHandler handler) {
        String actionLower = action.toLowerCase();
        
        if (!SUPPORTED_ACTIONS.contains(actionLower)) {
            return false;
        }
        
        System.out.println("→ [PeerController] Manejando acción: " + action);
        
        switch (actionLower) {
            // case "añadirpeer":  // DESHABILITADO: Auto-registro via bootstrap
            //     handleAñadirPeer(request, handler);
            //     break;
            case "listarPeersDisponibles":
                handleListarPeersDisponibles(request, handler);
                break;
            case "reportarlatido":
                handleReportarLatido(request, handler);
                break;
            case "retransmitirpeticion":
                handleRetransmitirPeticion(request, handler);
                break;
            case "buscarusuario":
                handleBuscarUsuario(request, handler);
                break;
            case "enrutarmensaje":
                handleEnrutarMensaje(request, handler);
                break;
            case "descubrirpeers":
                handleDescubrirPeers(request, handler);
                break;
            case "sincronizarusuarios":
                handleSincronizarUsuarios(request, handler);
                break;
            case "notificarcambioestado":
                handleNotificarCambioEstado(request, handler);
                break;
            case "verificarconexion":
            case "ping":
                handleVerificarConexion(request, handler);
                break;
            case "obtenerestadored":
                handleObtenerEstadoRed(request, handler);
                break;
            case "sincronizarcanales":
                handleSincronizarCanales(request, handler);
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
     * Maneja la acción de añadir un nuevo peer a la red P2P.
     * 
     * Request data esperado:
     * {
     *   "usuarioId": "uuid-del-usuario",
     *   "ip": "192.168.1.10",
     *   "puerto": 9000
     * }
     */
    private void handleAñadirPeer(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando añadirPeer");
        
        if (!validatePayload(request.getPayload(), handler, "añadirPeer")) {
            return;
        }
        
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            
            // Validar campos requeridos
            if (!payload.has("ip") || !payload.has("puerto")) {
                sendJsonResponse(handler, "añadirPeer", false, 
                    "Datos del peer inválidos", 
                    Map.of("campo", "ip/puerto", "motivo", "Campos requeridos"));
                return;
            }
            
            String ip = payload.get("ip").getAsString();
            int puerto = payload.get("puerto").getAsInt();
            
            // Validar IP
            if (ip == null || ip.trim().isEmpty()) {
                sendJsonResponse(handler, "añadirPeer", false, 
                    "Datos del peer inválidos", 
                    Map.of("campo", "ip", "motivo", "Formato de IP inválido"));
                return;
            }
            
            // Validar puerto
            if (puerto <= 0 || puerto > 65535) {
                sendJsonResponse(handler, "añadirPeer", false, 
                    "Datos del peer inválidos", 
                    Map.of("campo", "puerto", "motivo", "Puerto inválido"));
                return;
            }
            
            // Agregar peer usando la fachada
            PeerResponseDto peerDto = chatFachada.agregarPeer(ip, puerto);
            
            // Obtener lista completa de peers después de agregar
            List<PeerResponseDto> todosLosPeers = chatFachada.listarPeersDisponibles();
            
            // Preparar respuesta con lista completa de peers
            List<Map<String, Object>> peersData = new ArrayList<>();
            for (PeerResponseDto peer : todosLosPeers) {
                Map<String, Object> peerMap = new HashMap<>();
                peerMap.put("peerId", peer.getPeerId().toString());
                peerMap.put("ip", peer.getIp());
                peerMap.put("puerto", peer.getPuerto());
                peerMap.put("conectado", peer.getConectado()); // Ya es String
                
                peersData.add(peerMap);
            }
            
            System.out.println("✓ [PeerController] Peer añadido exitosamente: " + peerDto.getPeerId());
            sendJsonResponse(handler, "añadirPeer", true, 
                "Peer añadido y lista de peers actualizada", peersData);
            
        } catch (IllegalArgumentException e) {
            System.err.println("✗ [PeerController] Error de validación: " + e.getMessage());
            
            // Verificar si es error de peer duplicado
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("ya existe")) {
                sendJsonResponse(handler, "añadirPeer", false, 
                    "El peer ya se encuentra en la lista", null);
            } else {
                sendJsonResponse(handler, "añadirPeer", false, 
                    "Error al añadir el peer: " + e.getMessage(), null);
            }
                
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al añadir peer: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "añadirPeer", false, 
                "Error interno del servidor al añadir el peer", null);
        }
    }
    
    /**
     * Maneja la acción de listar todos los peers disponibles en la red.
     * 
     * Request data esperado:
     * {
     *   "usuarioId": "uuid-del-usuario"
     * }
     */
    private void handleListarPeersDisponibles(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando listarPeersDisponibles");
        
        try {
            // Obtener lista de todos los peers
            List<PeerResponseDto> peers = chatFachada.listarPeersDisponibles();
            
            // Preparar respuesta como array directo
            List<Map<String, Object>> peersData = new ArrayList<>();
            for (PeerResponseDto peer : peers) {
                Map<String, Object> peerMap = new HashMap<>();
                peerMap.put("peerId", peer.getPeerId().toString());
                peerMap.put("ip", peer.getIp());
                peerMap.put("puerto", peer.getPuerto());
                peerMap.put("conectado", peer.getConectado()); // Ya es String
                
                peersData.add(peerMap);
            }
            
            System.out.println("✓ [PeerController] Lista de peers obtenida: " + peers.size() + " peers");
            sendJsonResponse(handler, "listarPeersDisponibles", true, 
                "Lista de peers y su estado obtenida", peersData);
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al listar peers: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "listarPeersDisponibles", false, 
                "Error al obtener la lista de peers", null);
        }
    }
    
    /**
     * Maneja la acción de reportar un heartbeat (latido) de un peer.
     * 
     * Request data esperado:
     * {
     *   "peerId": "uuid-del-peer",
     *   "ip": "192.168.1.5",
     *   "puerto": 9000
     * }
     */
    private void handleReportarLatido(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando reportarLatido");
        
        if (!validatePayload(request.getPayload(), handler, "reportarLatido")) {
            return;
        }
        
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            
            // Validar peerId
            if (!payload.has("peerId")) {
                sendJsonResponse(handler, "reportarLatido", false, 
                    "Peer no reconocido o no registrado", 
                    Map.of("peerId", "DESCONOCIDO"));
                return;
            }
            
            String peerIdStr = payload.get("peerId").getAsString();
            UUID peerId;
            
            try {
                peerId = UUID.fromString(peerIdStr);
            } catch (IllegalArgumentException e) {
                sendJsonResponse(handler, "reportarLatido", false, 
                    "Peer no reconocido o no registrado", 
                    Map.of("peerId", peerIdStr));
                return;
            }
            
            // Reportar latido con ip y puerto si están presentes
            if (payload.has("ip") && payload.has("puerto")) {
                String ip = payload.get("ip").getAsString();
                int puerto = payload.get("puerto").getAsInt();
                chatFachada.reportarLatido(peerId, ip, puerto);
            } else {
                chatFachada.reportarLatido(peerId);
            }
            
            // Obtener intervalo de heartbeat
            long intervaloHeartbeat = chatFachada.obtenerIntervaloHeartbeat();
            
            // Preparar respuesta
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("proximoLatidoMs", intervaloHeartbeat);
            
            System.out.println("✓ [PeerController] Latido reportado para peer: " + peerId);
            sendJsonResponse(handler, "reportarLatido", true, 
                "Latido recibido", responseData);
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al reportar latido: " + e.getMessage());
            e.printStackTrace();
            
            // Verificar si el peer no existe
            if (e.getMessage() != null && (e.getMessage().contains("no encontrado") || 
                                          e.getMessage().contains("no existe"))) {
                sendJsonResponse(handler, "reportarLatido", false, 
                    "Peer no reconocido o no registrado", 
                    Map.of("peerId", "DESCONOCIDO"));
            } else {
                sendJsonResponse(handler, "reportarLatido", false, 
                    "Error al reportar latido", null);
            }
        }
    }

    
    /**
     * Maneja la acción de retransmitir una petición a otro peer.
     * 
     * Request data esperado:
     * {
     *   "peerOrigen": {
     *     "peerId": "uuid-servidor-A",
     *     "ip": "192.168.1.10",
     *     "puerto": 9000
     *   },
     *   "peticionCliente": {
     *     "action": "enviarMensajeDirecto",
     *     "data": { ... }
     *   }
     * }
     */
    private void handleRetransmitirPeticion(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando retransmitirPeticion");
        
        if (!validatePayload(request.getPayload(), handler, "retransmitirPeticion")) {
            return;
        }
        
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            
            // Validar campos requeridos
            if (!payload.has("peerOrigen") || !payload.has("peticionCliente")) {
                sendJsonResponse(handler, "retransmitirPeticion", false, 
                    "Faltan campos requeridos: peerOrigen y peticionCliente", null);
                return;
            }
            
            // Extraer información del peer origen
            JsonObject peerOrigenJson = payload.get("peerOrigen").getAsJsonObject();
            String peerOrigenId = peerOrigenJson.has("peerId") ? peerOrigenJson.get("peerId").getAsString() : null;
            
            // Parsear la petición del cliente
            JsonObject peticionClienteJson = payload.get("peticionCliente").getAsJsonObject();
            DTORequest peticionCliente = gson.fromJson(peticionClienteJson, DTORequest.class);
            
            // Validar petición del cliente
            if (peticionCliente.getAction() == null || peticionCliente.getAction().trim().isEmpty()) {
                sendJsonResponse(handler, "retransmitirPeticion", false, 
                    "La petición del cliente debe tener una acción válida", null);
                return;
            }
            
            // Extraer peerDestinoId del payload de la petición del cliente
            JsonObject peticionPayload = gson.toJsonTree(peticionCliente.getPayload()).getAsJsonObject();
            String peerDestinoIdStr = peticionPayload.has("peerDestinoId") ? 
                peticionPayload.get("peerDestinoId").getAsString() : null;
            
            if (peerDestinoIdStr == null) {
                sendJsonResponse(handler, "retransmitirPeticion", false, 
                    "La petición del cliente debe incluir peerDestinoId", null);
                return;
            }
            
            UUID peerDestinoId = UUID.fromString(peerDestinoIdStr);
            
            System.out.println("→ [PeerController] Retransmitiendo acción '" + 
                peticionCliente.getAction() + "' al peer: " + peerDestinoId);
            
            // Retransmitir usando la fachada
            DTOResponse respuestaPeer = chatFachada.retransmitirPeticion(peerDestinoId, peticionCliente);
            
            // Preparar respuesta según el formato del documento
            Map<String, Object> responseData = new HashMap<>();
            
            // Crear objeto respuestaCliente con la respuesta del peer
            Map<String, Object> respuestaCliente = new HashMap<>();
            respuestaCliente.put("action", respuestaPeer.getAction());
            respuestaCliente.put("status", respuestaPeer.getStatus());
            respuestaCliente.put("message", respuestaPeer.getMessage());
            respuestaCliente.put("data", respuestaPeer.getData() != null ? respuestaPeer.getData() : null);
            
            responseData.put("respuestaCliente", respuestaCliente);
            
            System.out.println("✓ [PeerController] Petición retransmitida exitosamente");
            sendJsonResponse(handler, "retransmitirPeticion", true, 
                "Petición del cliente procesada exitosamente.", responseData);
            
        } catch (IllegalArgumentException e) {
            System.err.println("✗ [PeerController] Error de validación: " + e.getMessage());
            sendJsonResponse(handler, "retransmitirPeticion", false, 
                "Error de validación: " + e.getMessage(), null);
                
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al retransmitir petición: " + e.getMessage());
            e.printStackTrace();
            
            // Si hay error, devolver con status success pero con respuestaCliente de error
            Map<String, Object> responseData = new HashMap<>();
            Map<String, Object> respuestaCliente = new HashMap<>();
            respuestaCliente.put("status", "error");
            respuestaCliente.put("message", e.getMessage() != null ? e.getMessage() : "Error al procesar petición");
            respuestaCliente.put("data", null);
            
            responseData.put("respuestaCliente", respuestaCliente);
            
            sendJsonResponse(handler, "retransmitirPeticion", true, 
                "Petición del cliente procesada, pero resultó en un error.", responseData);
        }
    }
    
    /**
     * Maneja la acción de buscar en qué peer está conectado un usuario.
     * 
     * Request data esperado:
     * {
     *   "usuarioId": "uuid-del-usuario"
     * }
     */
    private void handleBuscarUsuario(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando buscarUsuario");
        
        if (!validatePayload(request.getPayload(), handler, "buscarUsuario")) {
            return;
        }
        
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            
            // Validar usuarioId
            if (!payload.has("usuarioId")) {
                sendJsonResponse(handler, "buscarUsuario", false, 
                    "El ID del usuario es requerido", 
                    Map.of("campo", "usuarioId", "motivo", "Campo requerido"));
                return;
            }
            
            String usuarioIdStr = payload.get("usuarioId").getAsString();
            UUID usuarioId;
            
            try {
                usuarioId = UUID.fromString(usuarioIdStr);
            } catch (IllegalArgumentException e) {
                sendJsonResponse(handler, "buscarUsuario", false, 
                    "Formato de UUID inválido", 
                    Map.of("campo", "usuarioId", "motivo", "Formato UUID inválido"));
                return;
            }
            
            System.out.println("→ [PeerController] Buscando usuario: " + usuarioId);
            
            // Buscar usuario usando la fachada
            com.arquitectura.DTO.p2p.UserLocationResponseDto userLocation = 
                chatFachada.buscarUsuario(usuarioId);
            
            // Preparar respuesta
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("usuarioId", userLocation.getUsuarioId().toString());
            responseData.put("username", userLocation.getUsername());
            responseData.put("conectado", userLocation.isConectado());
            
            if (userLocation.getPeerId() != null) {
                responseData.put("peerId", userLocation.getPeerId().toString());
                responseData.put("peerIp", userLocation.getPeerIp());
                responseData.put("peerPuerto", userLocation.getPeerPuerto());
            } else {
                responseData.put("peerId", null);
                responseData.put("peerIp", null);
                responseData.put("peerPuerto", null);
            }
            
            System.out.println("✓ [PeerController] Usuario encontrado");
            sendJsonResponse(handler, "buscarUsuario", true, 
                "Usuario encontrado exitosamente", responseData);
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al buscar usuario: " + e.getMessage());
            e.printStackTrace();
            
            // Verificar si es error de usuario no encontrado
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                sendJsonResponse(handler, "buscarUsuario", false, 
                    "Usuario no encontrado", 
                    Map.of("usuarioId", "DESCONOCIDO"));
            } else {
                sendJsonResponse(handler, "buscarUsuario", false, 
                    "Error al buscar usuario", null);
            }
        }
    }
    
    /**
     * Maneja la acción de enrutar un mensaje P2P desde un remitente hacia un destinatario.
     * 
     * Request data esperado:
     * {
     *   "remitenteId": "uuid-del-remitente",
     *   "destinatarioId": "uuid-del-destinatario",
     *   "contenido": "Texto del mensaje",
     *   "tipo": "texto"
     * }
     */
    private void handleEnrutarMensaje(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando enrutarMensaje");
        
        if (!validatePayload(request.getPayload(), handler, "enrutarMensaje")) {
            return;
        }
        
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            
            // Validar campos requeridos
            if (!payload.has("remitenteId") || !payload.has("destinatarioId") || !payload.has("contenido")) {
                sendJsonResponse(handler, "enrutarMensaje", false, 
                    "Faltan campos requeridos", 
                    Map.of("campo", "remitenteId/destinatarioId/contenido", "motivo", "Campos requeridos"));
                return;
            }
            
            String remitenteIdStr = payload.get("remitenteId").getAsString();
            String destinatarioIdStr = payload.get("destinatarioId").getAsString();
            String contenido = payload.get("contenido").getAsString();
            String tipo = payload.has("tipo") ? payload.get("tipo").getAsString() : "texto";
            
            UUID remitenteId;
            UUID destinatarioId;
            
            try {
                remitenteId = UUID.fromString(remitenteIdStr);
                destinatarioId = UUID.fromString(destinatarioIdStr);
            } catch (IllegalArgumentException e) {
                sendJsonResponse(handler, "enrutarMensaje", false, 
                    "Formato de UUID inválido", 
                    Map.of("campo", "remitenteId/destinatarioId", "motivo", "Formato UUID inválido"));
                return;
            }
            
            System.out.println("→ [PeerController] Enrutando mensaje de " + remitenteId + " a " + destinatarioId);
            
            // Buscar ubicación del destinatario
            com.arquitectura.DTO.p2p.UserLocationResponseDto userLocation = 
                chatFachada.buscarUsuario(destinatarioId);
            
            // Verificar si el usuario está conectado
            if (!userLocation.isConectado() || userLocation.getPeerId() == null) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("destinatarioId", destinatarioId.toString());
                errorData.put("destinatarioUsername", userLocation.getUsername());
                errorData.put("entregado", false);
                errorData.put("fechaEntrega", null);
                errorData.put("motivo", "Usuario no está conectado a ningún peer");
                
                sendJsonResponse(handler, "enrutarMensaje", false, 
                    "Usuario destinatario no está conectado", errorData);
                return;
            }
            
            // Preparar petición para retransmitir al peer destinatario
            Map<String, Object> mensajeData = new HashMap<>();
            mensajeData.put("remitenteId", remitenteId.toString());
            mensajeData.put("destinatarioId", destinatarioId.toString());
            mensajeData.put("contenido", contenido);
            mensajeData.put("tipo", tipo);
            
            DTORequest peticionMensaje = new DTORequest("recibirMensajeDirecto", mensajeData);
            
            // Retransmitir mensaje al peer donde está conectado el destinatario
            DTOResponse respuestaPeer = chatFachada.retransmitirPeticion(
                userLocation.getPeerId(), peticionMensaje);
            
            // Verificar si la entrega fue exitosa
            if ("success".equals(respuestaPeer.getStatus())) {
                // Obtener fecha actual en formato ISO 8601
                String fechaEntrega = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                
                Map<String, Object> successData = new HashMap<>();
                successData.put("destinatarioId", destinatarioId.toString());
                successData.put("destinatarioUsername", userLocation.getUsername());
                successData.put("entregado", true);
                successData.put("fechaEntrega", fechaEntrega);
                successData.put("motivo", "Mensaje entregado exitosamente");
                
                System.out.println("✓ [PeerController] Mensaje enrutado exitosamente");
                sendJsonResponse(handler, "enrutarMensaje", true, 
                    "Mensaje enrutado exitosamente", successData);
            } else {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("destinatarioId", destinatarioId.toString());
                errorData.put("destinatarioUsername", userLocation.getUsername());
                errorData.put("entregado", false);
                errorData.put("fechaEntrega", null);
                errorData.put("motivo", "Peer destinatario no disponible");
                
                sendJsonResponse(handler, "enrutarMensaje", false, 
                    "No se pudo entregar el mensaje", errorData);
            }
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al enrutar mensaje: " + e.getMessage());
            e.printStackTrace();
            
            // Verificar si es error de usuario no encontrado
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("destinatarioId", "DESCONOCIDO");
                errorData.put("destinatarioUsername", null);
                errorData.put("entregado", false);
                errorData.put("fechaEntrega", null);
                errorData.put("motivo", "Usuario no encontrado en el sistema");
                
                sendJsonResponse(handler, "enrutarMensaje", false, 
                    "Usuario destinatario no encontrado", errorData);
            } else {
                sendJsonResponse(handler, "enrutarMensaje", false, 
                    "Error al enrutar mensaje: " + e.getMessage(), null);
            }
        }
    }
    
    /**
     * Maneja la acción de descubrir peers disponibles en la red P2P.
     * Si el peer no existe, lo registra automáticamente.
     * 
     * Request data esperado:
     * {
     *   "peerId": "uuid-del-peer-solicitante" (opcional),
     *   "ip": "192.168.1.10",
     *   "puerto": 9000
     * }
     */
    private void handleDescubrirPeers(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando descubrirPeers");
        
        if (!validatePayload(request.getPayload(), handler, "descubrirPeers")) {
            return;
        }
        
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            
            // Validar campos requeridos
            if (!payload.has("ip") || !payload.has("puerto")) {
                sendJsonResponse(handler, "descubrirPeers", false, 
                    "Datos del peer inválidos", 
                    Map.of("campo", "ip/puerto", "motivo", "Campos requeridos"));
                return;
            }
            
            String ip = payload.get("ip").getAsString();
            int puerto = payload.get("puerto").getAsInt();
            
            // Validar IP
            if (ip == null || ip.trim().isEmpty()) {
                sendJsonResponse(handler, "descubrirPeers", false, 
                    "Datos del peer inválidos", 
                    Map.of("campo", "ip", "motivo", "Formato de IP inválido"));
                return;
            }
            
            // Validar puerto
            if (puerto <= 0 || puerto > 65535) {
                sendJsonResponse(handler, "descubrirPeers", false, 
                    "Puerto inválido", 
                    Map.of("campo", "puerto", "motivo", "El puerto debe estar entre 1 y 65535"));
                return;
            }
            
            UUID peerSolicitanteId = null;
            boolean esNuevo = false;
            
            // Verificar si el peer ya existe
            if (payload.has("peerId") && !payload.get("peerId").isJsonNull()) {
                try {
                    String peerIdStr = payload.get("peerId").getAsString();
                    peerSolicitanteId = UUID.fromString(peerIdStr);
                    
                    // Reportar latido para actualizar el peer existente
                    chatFachada.reportarLatido(peerSolicitanteId, ip, puerto);
                    System.out.println("→ [PeerController] Peer existente actualizado: " + peerSolicitanteId);
                    
                } catch (IllegalArgumentException e) {
                    System.out.println("→ [PeerController] PeerId inválido, se creará uno nuevo");
                    peerSolicitanteId = null;
                }
            }
            
            // Si no existe, crear un nuevo peer
            if (peerSolicitanteId == null) {
                PeerResponseDto nuevoPeer = chatFachada.agregarPeer(ip, puerto);
                peerSolicitanteId = nuevoPeer.getPeerId();
                esNuevo = true;
                System.out.println("→ [PeerController] Nuevo peer registrado: " + peerSolicitanteId);
            }
            
            // Obtener lista de peers activos (excluyendo al solicitante)
            List<PeerResponseDto> todosLosPeers = chatFachada.listarPeersActivos();
            
            // Filtrar para excluir al peer solicitante
            List<Map<String, Object>> peersDisponibles = new ArrayList<>();
            for (PeerResponseDto peer : todosLosPeers) {
                if (!peer.getPeerId().equals(peerSolicitanteId)) {
                    Map<String, Object> peerMap = new HashMap<>();
                    peerMap.put("peerId", peer.getPeerId().toString());
                    peerMap.put("ip", peer.getIp());
                    peerMap.put("puerto", peer.getPuerto());
                    peerMap.put("conectado", peer.getConectado());
                    
                    peersDisponibles.add(peerMap);
                }
            }
            
            // Preparar información del peer solicitante
            Map<String, Object> peerSolicitanteInfo = new HashMap<>();
            peerSolicitanteInfo.put("peerId", peerSolicitanteId.toString());
            peerSolicitanteInfo.put("registrado", true);
            if (esNuevo) {
                peerSolicitanteInfo.put("esNuevo", true);
            }
            
            // Preparar respuesta completa
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("peersDisponibles", peersDisponibles);
            responseData.put("totalPeers", peersDisponibles.size());
            responseData.put("peerSolicitante", peerSolicitanteInfo);
            
            String mensaje = esNuevo ? 
                "Peer registrado y peers descubiertos" : 
                "Peers descubiertos exitosamente";
            
            System.out.println("✓ [PeerController] Descubrimiento completado: " + 
                peersDisponibles.size() + " peers disponibles");
            sendJsonResponse(handler, "descubrirPeers", true, mensaje, responseData);
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al descubrir peers: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "descubrirPeers", false, 
                "Error al descubrir peers: " + e.getMessage(), null);
        }
    }
    
    /**
     * Maneja la acción de sincronizar la lista de usuarios conectados.
     * Retorna información de todos los usuarios y en qué peer están conectados.
     * 
     * Request data esperado:
     * {
     *   "peerId": "uuid-del-peer-solicitante" (opcional)
     * }
     */
    private void handleSincronizarUsuarios(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando sincronizarUsuarios");
        
        try {
            // El payload es opcional para esta operación
            UUID peerSolicitanteId = null;
            
            if (request.getPayload() != null) {
                JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
                
                // Verificar si se proporciona peerId (opcional)
                if (payload.has("peerId") && !payload.get("peerId").isJsonNull()) {
                    try {
                        String peerIdStr = payload.get("peerId").getAsString();
                        peerSolicitanteId = UUID.fromString(peerIdStr);
                        System.out.println("→ [PeerController] Sincronización solicitada por peer: " + peerSolicitanteId);
                    } catch (IllegalArgumentException e) {
                        System.out.println("→ [PeerController] PeerId inválido, continuando sin validación");
                    }
                }
            }
            
            // Obtener todos los usuarios del sistema
            List<com.arquitectura.DTO.usuarios.UserResponseDto> todosLosUsuarios = 
                chatFachada.obtenerTodosLosUsuarios();
            
            // Preparar lista de usuarios con su información de ubicación
            List<Map<String, Object>> usuariosData = new ArrayList<>();
            int usuariosConectados = 0;
            
            for (com.arquitectura.DTO.usuarios.UserResponseDto usuario : todosLosUsuarios) {
                Map<String, Object> usuarioMap = new HashMap<>();
                usuarioMap.put("usuarioId", usuario.getUserId().toString());
                usuarioMap.put("username", usuario.getUsername());
                
                // Verificar si el usuario está conectado (estado == "ONLINE")
                boolean conectado = "ONLINE".equalsIgnoreCase(usuario.getEstado());
                usuarioMap.put("conectado", conectado);
                
                // Si el usuario está conectado, buscar en qué peer está
                if (conectado) {
                    usuariosConectados++;
                    try {
                        com.arquitectura.DTO.p2p.UserLocationResponseDto ubicacion = 
                            chatFachada.buscarUsuario(usuario.getUserId());
                        
                        if (ubicacion.getPeerId() != null) {
                            usuarioMap.put("peerId", ubicacion.getPeerId().toString());
                            usuarioMap.put("peerIp", ubicacion.getPeerIp());
                            usuarioMap.put("peerPuerto", ubicacion.getPeerPuerto());
                        } else {
                            usuarioMap.put("peerId", null);
                            usuarioMap.put("peerIp", null);
                            usuarioMap.put("peerPuerto", null);
                        }
                    } catch (Exception e) {
                        // Si no se puede obtener la ubicación, marcar como null
                        usuarioMap.put("peerId", null);
                        usuarioMap.put("peerIp", null);
                        usuarioMap.put("peerPuerto", null);
                    }
                } else {
                    usuarioMap.put("peerId", null);
                    usuarioMap.put("peerIp", null);
                    usuarioMap.put("peerPuerto", null);
                }
                
                usuariosData.add(usuarioMap);
            }
            
            // Obtener timestamp de sincronización
            String fechaSincronizacion = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Preparar respuesta completa
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("usuarios", usuariosData);
            responseData.put("totalUsuarios", todosLosUsuarios.size());
            responseData.put("usuariosConectados", usuariosConectados);
            responseData.put("fechaSincronizacion", fechaSincronizacion);
            
            System.out.println("✓ [PeerController] Sincronización completada: " + 
                todosLosUsuarios.size() + " usuarios (" + usuariosConectados + " conectados)");
            sendJsonResponse(handler, "sincronizarUsuarios", true, 
                "Usuarios sincronizados exitosamente", responseData);
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al sincronizar usuarios: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "sincronizarUsuarios", false, 
                "Error al sincronizar usuarios: " + e.getMessage(), null);
        }
    }
    
    /**
     * Maneja la acción de notificar un cambio de estado de un usuario.
     * Actualiza el estado del usuario (ONLINE/OFFLINE) en el sistema.
     * 
     * Request data esperado:
     * {
     *   "usuarioId": "uuid-del-usuario",
     *   "nuevoEstado": "ONLINE",
     *   "peerId": "uuid-del-peer" (opcional, para ONLINE),
     *   "peerIp": "192.168.1.5" (opcional, para ONLINE),
     *   "peerPuerto": 9000 (opcional, para ONLINE)
     * }
     */
    private void handleNotificarCambioEstado(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando notificarCambioEstado");
        
        if (!validatePayload(request.getPayload(), handler, "notificarCambioEstado")) {
            return;
        }
        
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            
            // Validar campos requeridos
            if (!payload.has("usuarioId") || !payload.has("nuevoEstado")) {
                sendJsonResponse(handler, "notificarCambioEstado", false, 
                    "Faltan campos requeridos", 
                    Map.of("campo", "usuarioId/nuevoEstado", "motivo", "Campos requeridos"));
                return;
            }
            
            String usuarioIdStr = payload.get("usuarioId").getAsString();
            String nuevoEstado = payload.get("nuevoEstado").getAsString().toUpperCase();
            
            // Validar UUID
            UUID usuarioId;
            try {
                usuarioId = UUID.fromString(usuarioIdStr);
            } catch (IllegalArgumentException e) {
                sendJsonResponse(handler, "notificarCambioEstado", false, 
                    "Formato de UUID inválido", 
                    Map.of("campo", "usuarioId", "motivo", "Formato UUID inválido"));
                return;
            }
            
            // Validar estado
            if (!nuevoEstado.equals("ONLINE") && !nuevoEstado.equals("OFFLINE")) {
                sendJsonResponse(handler, "notificarCambioEstado", false, 
                    "Estado inválido", 
                    Map.of("campo", "nuevoEstado", "motivo", "El estado debe ser ONLINE u OFFLINE"));
                return;
            }
            
            System.out.println("→ [PeerController] Cambiando estado de usuario " + usuarioId + " a " + nuevoEstado);
            
            // Buscar usuario para obtener información actual
            com.arquitectura.DTO.usuarios.UserResponseDto usuario = 
                chatFachada.buscarUsuarioPorUsername(null).orElse(null);
            
            // Obtener el usuario por ID usando la lista de todos los usuarios
            List<com.arquitectura.DTO.usuarios.UserResponseDto> todosUsuarios = 
                chatFachada.obtenerTodosLosUsuarios();
            
            com.arquitectura.DTO.usuarios.UserResponseDto usuarioEncontrado = null;
            for (com.arquitectura.DTO.usuarios.UserResponseDto u : todosUsuarios) {
                if (u.getUserId().equals(usuarioId)) {
                    usuarioEncontrado = u;
                    break;
                }
            }
            
            if (usuarioEncontrado == null) {
                sendJsonResponse(handler, "notificarCambioEstado", false, 
                    "Usuario no encontrado", 
                    Map.of("usuarioId", usuarioIdStr, "motivo", "El usuario no existe en el sistema"));
                return;
            }
            
            // Obtener estado anterior
            String estadoAnterior = usuarioEncontrado.getEstado() != null ? 
                usuarioEncontrado.getEstado() : "OFFLINE";
            
            // Cambiar estado del usuario
            boolean nuevoEstadoBoolean = nuevoEstado.equals("ONLINE");
            chatFachada.cambiarEstadoUsuario(usuarioId, nuevoEstadoBoolean);
            
            // Obtener timestamp del cambio
            String fechaCambio = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Preparar respuesta
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("usuarioId", usuarioId.toString());
            responseData.put("username", usuarioEncontrado.getUsername());
            responseData.put("estadoAnterior", estadoAnterior);
            responseData.put("estadoNuevo", nuevoEstado);
            responseData.put("fechaCambio", fechaCambio);
            
            // Si el nuevo estado es ONLINE, incluir información del peer
            if (nuevoEstado.equals("ONLINE") && payload.has("peerId")) {
                try {
                    String peerIdStr = payload.get("peerId").getAsString();
                    UUID peerId = UUID.fromString(peerIdStr);
                    responseData.put("peerId", peerId.toString());
                    
                    if (payload.has("peerIp")) {
                        responseData.put("peerIp", payload.get("peerIp").getAsString());
                    }
                    if (payload.has("peerPuerto")) {
                        responseData.put("peerPuerto", payload.get("peerPuerto").getAsInt());
                    }
                } catch (Exception e) {
                    // Si hay error con el peer, continuar sin esa información
                    responseData.put("peerId", null);
                    responseData.put("peerIp", null);
                    responseData.put("peerPuerto", null);
                }
            } else {
                responseData.put("peerId", null);
                responseData.put("peerIp", null);
                responseData.put("peerPuerto", null);
            }
            
            System.out.println("✓ [PeerController] Estado cambiado exitosamente: " + 
                estadoAnterior + " → " + nuevoEstado);
            sendJsonResponse(handler, "notificarCambioEstado", true, 
                "Cambio de estado notificado exitosamente", responseData);
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al notificar cambio de estado: " + e.getMessage());
            e.printStackTrace();
            
            // Verificar si es error de usuario no encontrado
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                sendJsonResponse(handler, "notificarCambioEstado", false, 
                    "Usuario no encontrado", 
                    Map.of("usuarioId", "DESCONOCIDO", "motivo", "El usuario no existe en el sistema"));
            } else {
                sendJsonResponse(handler, "notificarCambioEstado", false, 
                    "Error al notificar cambio de estado: " + e.getMessage(), null);
            }
        }
    }
    
    /**
     * Maneja la acción de verificar conexión (Ping/Pong).
     * Responde inmediatamente para confirmar que el peer está activo.
     * 
     * Request data esperado:
     * {
     *   "peerId": "uuid-del-peer-solicitante" (opcional),
     *   "timestamp": "2024-11-07T10:30:00" (opcional),
     *   "solicitarEstadisticas": true (opcional)
     * }
     */
    private void handleVerificarConexion(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando verificarConexion (ping)");
        
        try {
            // Timestamp de recepción del ping
            String timestampPong = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Preparar respuesta básica
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("timestampPong", timestampPong);
            
            // Si hay payload, procesar información adicional
            if (request.getPayload() != null) {
                JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
                
                // Si se proporciona peerId del solicitante
                if (payload.has("peerId") && !payload.get("peerId").isJsonNull()) {
                    try {
                        String peerIdStr = payload.get("peerId").getAsString();
                        UUID peerId = UUID.fromString(peerIdStr);
                        System.out.println("→ [PeerController] Ping recibido de peer: " + peerId);
                    } catch (Exception e) {
                        // Ignorar si el peerId es inválido
                    }
                }
                
                // Si se proporciona timestamp del ping, calcular latencia
                if (payload.has("timestamp") && !payload.get("timestamp").isJsonNull()) {
                    try {
                        String timestampPing = payload.get("timestamp").getAsString();
                        responseData.put("timestampPing", timestampPing);
                        
                        // Calcular latencia aproximada (en milisegundos)
                        java.time.LocalDateTime pingTime = java.time.LocalDateTime.parse(
                            timestampPing, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        java.time.LocalDateTime pongTime = java.time.LocalDateTime.now();
                        
                        long latenciaMs = java.time.Duration.between(pingTime, pongTime).toMillis();
                        responseData.put("latenciaMs", latenciaMs);
                    } catch (Exception e) {
                        // Si hay error parseando el timestamp, continuar sin latencia
                    }
                }
                
                // Si se solicitan estadísticas adicionales
                if (payload.has("solicitarEstadisticas") && 
                    payload.get("solicitarEstadisticas").getAsBoolean()) {
                    
                    responseData.put("estadoServidor", "ONLINE");
                    responseData.put("version", "1.0.0");
                    
                    try {
                        // Obtener número de usuarios conectados
                        List<com.arquitectura.DTO.usuarios.UserResponseDto> usuariosConectados = 
                            chatFachada.obtenerUsuariosConectados();
                        responseData.put("usuariosConectados", usuariosConectados.size());
                    } catch (Exception e) {
                        // Si hay error obteniendo estadísticas, continuar sin ellas
                    }
                }
            }
            
            // Respuesta simple y rápida
            System.out.println("✓ [PeerController] Pong enviado");
            sendJsonResponse(handler, request.getAction(), true, "pong", responseData);
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al procesar ping: " + e.getMessage());
            e.printStackTrace();
            
            // Incluso en caso de error, intentar responder con pong básico
            Map<String, Object> basicResponse = new HashMap<>();
            basicResponse.put("timestamp", java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            sendJsonResponse(handler, request.getAction(), true, "pong", basicResponse);
        }
    }
    
    /**
     * Maneja la acción de obtener el estado completo de la red P2P (topología).
     * Proporciona información sobre peers, usuarios y estadísticas de la red.
     * 
     * Request data esperado:
     * {
     *   "peerId": "uuid-del-peer-solicitante" (opcional),
     *   "incluirDetalles": true (opcional)
     * }
     */
    private void handleObtenerEstadoRed(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando obtenerEstadoRed");
        
        try {
            boolean incluirDetalles = false;
            
            // Verificar si se solicitan detalles
            if (request.getPayload() != null) {
                JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
                
                if (payload.has("incluirDetalles") && !payload.get("incluirDetalles").isJsonNull()) {
                    incluirDetalles = payload.get("incluirDetalles").getAsBoolean();
                }
                
                if (payload.has("peerId") && !payload.get("peerId").isJsonNull()) {
                    String peerIdStr = payload.get("peerId").getAsString();
                    System.out.println("→ [PeerController] Estado solicitado por peer: " + peerIdStr);
                }
            }
            
            // Obtener todos los peers
            List<PeerResponseDto> todosLosPeers = chatFachada.listarPeersDisponibles();
            
            // Clasificar peers por estado
            int peersOnline = 0;
            int peersOffline = 0;
            List<Map<String, Object>> peersData = new ArrayList<>();
            
            for (PeerResponseDto peer : todosLosPeers) {
                boolean isOnline = "ONLINE".equalsIgnoreCase(peer.getConectado());
                
                if (isOnline) {
                    peersOnline++;
                } else {
                    peersOffline++;
                }
                
                if (incluirDetalles) {
                    Map<String, Object> peerMap = new HashMap<>();
                    peerMap.put("peerId", peer.getPeerId().toString());
                    peerMap.put("ip", peer.getIp());
                    peerMap.put("puerto", peer.getPuerto());
                    peerMap.put("estado", peer.getConectado());
                    
                    // Contar usuarios conectados a este peer
                    int usuariosEnPeer = 0;
                    try {
                        List<com.arquitectura.DTO.usuarios.UserResponseDto> todosUsuarios = 
                            chatFachada.obtenerTodosLosUsuarios();
                        
                        for (com.arquitectura.DTO.usuarios.UserResponseDto usuario : todosUsuarios) {
                            if ("ONLINE".equalsIgnoreCase(usuario.getEstado())) {
                                try {
                                    com.arquitectura.DTO.p2p.UserLocationResponseDto ubicacion = 
                                        chatFachada.buscarUsuario(usuario.getUserId());
                                    
                                    if (ubicacion.getPeerId() != null && 
                                        ubicacion.getPeerId().equals(peer.getPeerId())) {
                                        usuariosEnPeer++;
                                    }
                                } catch (Exception e) {
                                    // Ignorar errores al buscar ubicación de usuario
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Si hay error, continuar sin contar usuarios
                    }
                    
                    peerMap.put("usuariosConectados", usuariosEnPeer);
                    peersData.add(peerMap);
                }
            }
            
            // Preparar información de topología
            Map<String, Object> topologia = new HashMap<>();
            topologia.put("totalPeers", todosLosPeers.size());
            topologia.put("peersOnline", peersOnline);
            topologia.put("peersOffline", peersOffline);
            
            if (incluirDetalles) {
                topologia.put("peers", peersData);
            }
            
            // Obtener información de usuarios
            List<com.arquitectura.DTO.usuarios.UserResponseDto> todosUsuarios = 
                chatFachada.obtenerTodosLosUsuarios();
            
            int usuariosConectados = 0;
            Map<String, Integer> distribucionPorPeer = new HashMap<>();
            
            for (com.arquitectura.DTO.usuarios.UserResponseDto usuario : todosUsuarios) {
                if ("ONLINE".equalsIgnoreCase(usuario.getEstado())) {
                    usuariosConectados++;
                    
                    if (incluirDetalles) {
                        try {
                            com.arquitectura.DTO.p2p.UserLocationResponseDto ubicacion = 
                                chatFachada.buscarUsuario(usuario.getUserId());
                            
                            if (ubicacion.getPeerId() != null) {
                                String peerIdStr = ubicacion.getPeerId().toString();
                                distribucionPorPeer.put(peerIdStr, 
                                    distribucionPorPeer.getOrDefault(peerIdStr, 0) + 1);
                            }
                        } catch (Exception e) {
                            // Ignorar errores al buscar ubicación
                        }
                    }
                }
            }
            
            // Preparar información de usuarios
            Map<String, Object> usuarios = new HashMap<>();
            usuarios.put("totalUsuarios", todosUsuarios.size());
            usuarios.put("usuariosConectados", usuariosConectados);
            usuarios.put("usuariosOffline", todosUsuarios.size() - usuariosConectados);
            
            if (incluirDetalles && !distribucionPorPeer.isEmpty()) {
                List<Map<String, Object>> distribucion = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : distribucionPorPeer.entrySet()) {
                    Map<String, Object> dist = new HashMap<>();
                    dist.put("peerId", entry.getKey());
                    dist.put("cantidad", entry.getValue());
                    distribucion.add(dist);
                }
                usuarios.put("distribucion", distribucion);
            }
            
            // Timestamp de consulta
            String fechaConsulta = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Preparar respuesta completa
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("topologia", topologia);
            responseData.put("usuarios", usuarios);
            responseData.put("fechaConsulta", fechaConsulta);
            
            System.out.println("✓ [PeerController] Estado de red obtenido: " + 
                todosLosPeers.size() + " peers, " + usuariosConectados + " usuarios conectados");
            sendJsonResponse(handler, "obtenerEstadoRed", true, 
                "Estado de la red obtenido exitosamente", responseData);
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al obtener estado de red: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "obtenerEstadoRed", false, 
                "Error al obtener estado de la red", 
                Map.of("motivo", e.getMessage() != null ? e.getMessage() : "Error interno del servidor"));
        }
    }
    
    /**
     * Maneja la acción de sincronizar canales globales.
     * Proporciona información sobre todos los canales y opcionalmente sus miembros.
     * 
     * Request data esperado:
     * {
     *   "peerId": "uuid-del-peer-solicitante" (opcional),
     *   "incluirMiembros": true (opcional)
     * }
     */
    private void handleSincronizarCanales(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando sincronizarCanales");
        
        try {
            boolean incluirMiembros = false;
            
            // Verificar si se solicita incluir miembros
            if (request.getPayload() != null) {
                JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
                
                if (payload.has("incluirMiembros") && !payload.get("incluirMiembros").isJsonNull()) {
                    incluirMiembros = payload.get("incluirMiembros").getAsBoolean();
                }
                
                if (payload.has("peerId") && !payload.get("peerId").isJsonNull()) {
                    String peerIdStr = payload.get("peerId").getAsString();
                    System.out.println("→ [PeerController] Sincronización solicitada por peer: " + peerIdStr);
                }
            }
            
            // Obtener todos los canales
            List<com.arquitectura.DTO.canales.ChannelResponseDto> todosLosCanales = 
                chatFachada.obtenerTodosLosCanales();
            
            // Clasificar canales y preparar información
            int canalesPublicos = 0;
            int canalesPrivados = 0;
            List<Map<String, Object>> canalesData = new ArrayList<>();
            
            for (com.arquitectura.DTO.canales.ChannelResponseDto canal : todosLosCanales) {
                Map<String, Object> canalMap = new HashMap<>();
                canalMap.put("canalId", canal.getChannelId().toString());
                canalMap.put("nombre", canal.getChannelName());
                canalMap.put("tipo", canal.getChannelType() != null ? canal.getChannelType() : "PUBLICO");
                
                // Obtener información del propietario
                if (canal.getOwner() != null) {
                    canalMap.put("propietarioId", canal.getOwner().getUserId().toString());
                    canalMap.put("propietarioUsername", canal.getOwner().getUsername());
                } else {
                    canalMap.put("propietarioId", "DESCONOCIDO");
                    canalMap.put("propietarioUsername", "Desconocido");
                }
                
                // Clasificar por tipo
                String tipo = canal.getChannelType() != null ? canal.getChannelType() : "PUBLICO";
                if ("PUBLICO".equalsIgnoreCase(tipo) || "PUBLIC".equalsIgnoreCase(tipo)) {
                    canalesPublicos++;
                } else {
                    canalesPrivados++;
                }
                
                // Si se solicitan miembros, obtenerlos
                if (incluirMiembros) {
                    try {
                        // Obtener miembros del canal
                        // Necesitamos un usuario solicitante válido, usamos el propietario
                        UUID ownerId = canal.getOwner() != null ? canal.getOwner().getUserId() : null;
                        if (ownerId != null) {
                            List<com.arquitectura.DTO.usuarios.UserResponseDto> miembros = 
                                chatFachada.obtenerMiembrosDeCanal(canal.getChannelId(), ownerId);
                        
                        List<Map<String, Object>> miembrosData = new ArrayList<>();
                        for (com.arquitectura.DTO.usuarios.UserResponseDto miembro : miembros) {
                            Map<String, Object> miembroMap = new HashMap<>();
                            miembroMap.put("usuarioId", miembro.getUserId().toString());
                            miembroMap.put("username", miembro.getUsername());
                            miembroMap.put("rol", miembro.getRol() != null ? miembro.getRol() : "MIEMBRO");
                            miembrosData.add(miembroMap);
                        }
                        
                            canalMap.put("miembros", miembrosData);
                            canalMap.put("totalMiembros", miembros.size());
                        } else {
                            canalMap.put("miembros", new ArrayList<>());
                            canalMap.put("totalMiembros", 0);
                        }
                    } catch (Exception e) {
                        // Si hay error obteniendo miembros, solo incluir el conteo
                        canalMap.put("miembros", new ArrayList<>());
                        canalMap.put("totalMiembros", 0);
                    }
                } else {
                    // Solo incluir conteo de miembros
                    try {
                        UUID ownerId = canal.getOwner() != null ? canal.getOwner().getUserId() : null;
                        if (ownerId != null) {
                            List<com.arquitectura.DTO.usuarios.UserResponseDto> miembros = 
                                chatFachada.obtenerMiembrosDeCanal(canal.getChannelId(), ownerId);
                            canalMap.put("totalMiembros", miembros.size());
                        } else {
                            canalMap.put("totalMiembros", 0);
                        }
                    } catch (Exception e) {
                        canalMap.put("totalMiembros", 0);
                    }
                }
                
                canalesData.add(canalMap);
            }
            
            // Timestamp de sincronización
            String fechaSincronizacion = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Preparar respuesta completa
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("canales", canalesData);
            responseData.put("totalCanales", todosLosCanales.size());
            responseData.put("canalesPublicos", canalesPublicos);
            responseData.put("canalesPrivados", canalesPrivados);
            responseData.put("fechaSincronizacion", fechaSincronizacion);
            
            System.out.println("✓ [PeerController] Canales sincronizados: " + 
                todosLosCanales.size() + " canales (" + canalesPublicos + " públicos, " + 
                canalesPrivados + " privados)");
            sendJsonResponse(handler, "sincronizarCanales", true, 
                "Canales sincronizados exitosamente", responseData);
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al sincronizar canales: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "sincronizarCanales", false, 
                "Error al sincronizar canales", 
                Map.of("motivo", e.getMessage() != null ? e.getMessage() : "Error interno del servidor"));
        }
    }
    
}
