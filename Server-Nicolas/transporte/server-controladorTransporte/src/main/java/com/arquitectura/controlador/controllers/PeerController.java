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
        "añadirpeer",
        "listarPeersDisponibles",
        "reportarlatido",
        "retransmitirpeticion"
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
            case "añadirpeer":
                handleAñadirPeer(request, handler);
                break;
            case "listarPeersDisponibles":
                handleListarPeersDisponibles(request, handler);
                break;
            case "reportarlatido":
                handleReportarLatido(request, handler);
                break;
            case "retransmitirpeticion":
                handleRetransmitirPeticion(request, handler);
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
    
}
