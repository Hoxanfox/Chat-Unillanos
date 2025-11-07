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
        "retransmitirpeticion",
        "actualizarlistapeers"
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
            case "actualizarlistapeers":
                handleActualizarListaPeers(request, handler);
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
     * Request payload esperado:
     * {
     *   "ip": "192.168.1.10",
     *   "puerto": 22100,
     *   "nombreServidor": "Servidor-A" (opcional)
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
                    "Faltan campos requeridos: ip y puerto", 
                    createErrorData("ip/puerto", "Campos requeridos"));
                return;
            }
            
            String ip = payload.get("ip").getAsString();
            int puerto = payload.get("puerto").getAsInt();
            String nombreServidor = payload.has("nombreServidor") ? 
                payload.get("nombreServidor").getAsString() : null;
            
            // Validar IP
            if (ip == null || ip.trim().isEmpty()) {
                sendJsonResponse(handler, "añadirPeer", false, 
                    "La IP no puede estar vacía", 
                    createErrorData("ip", "Campo requerido"));
                return;
            }
            
            // Validar puerto
            if (puerto <= 0 || puerto > 65535) {
                sendJsonResponse(handler, "añadirPeer", false, 
                    "Puerto inválido. Debe estar entre 1 y 65535", 
                    createErrorData("puerto", "Valor inválido"));
                return;
            }
            
            // Agregar peer usando la fachada
            PeerResponseDto peerDto;
            if (nombreServidor != null && !nombreServidor.trim().isEmpty()) {
                peerDto = chatFachada.agregarPeer(ip, puerto, nombreServidor);
            } else {
                peerDto = chatFachada.agregarPeer(ip, puerto);
            }
            
            // Preparar respuesta
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("peerId", peerDto.getPeerId().toString());
            responseData.put("ip", peerDto.getIp());
            responseData.put("puerto", peerDto.getPuerto());
            responseData.put("conectado", peerDto.getConectado());
            responseData.put("ultimoLatido", peerDto.getUltimoLatido() != null ? 
                peerDto.getUltimoLatido().toString() : null);
            
            if (peerDto.getNombreServidor() != null) {
                responseData.put("nombreServidor", peerDto.getNombreServidor());
            }
            
            System.out.println("✓ [PeerController] Peer añadido exitosamente: " + peerDto.getPeerId());
            sendJsonResponse(handler, "añadirPeer", true, 
                "Peer añadido exitosamente", responseData);
            
        } catch (IllegalArgumentException e) {
            System.err.println("✗ [PeerController] Error de validación: " + e.getMessage());
            sendJsonResponse(handler, "añadirPeer", false, 
                e.getMessage(), 
                createErrorData("validacion", e.getMessage()));
                
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al añadir peer: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "añadirPeer", false, 
                "Error interno al añadir peer", null);
        }
    }
    
    /**
     * Maneja la acción de listar todos los peers disponibles en la red.
     * 
     * Request payload esperado:
     * {
     *   "soloActivos": true/false (opcional, default: false)
     * }
     */
    private void handleListarPeersDisponibles(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando listarPeersDisponibles");
        
        try {
            boolean soloActivos = false;
            
            // Verificar si se solicita solo peers activos
            if (request.getPayload() != null) {
                JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
                if (payload.has("soloActivos")) {
                    soloActivos = payload.get("soloActivos").getAsBoolean();
                }
            }
            
            // Obtener lista de peers
            List<PeerResponseDto> peers;
            if (soloActivos) {
                peers = chatFachada.listarPeersActivos();
            } else {
                peers = chatFachada.listarPeersDisponibles();
            }
            
            // Preparar respuesta
            List<Map<String, Object>> peersData = new ArrayList<>();
            for (PeerResponseDto peer : peers) {
                Map<String, Object> peerMap = new HashMap<>();
                peerMap.put("peerId", peer.getPeerId().toString());
                peerMap.put("ip", peer.getIp());
                peerMap.put("puerto", peer.getPuerto());
                peerMap.put("conectado", peer.getConectado());
                peerMap.put("ultimoLatido", peer.getUltimoLatido() != null ? 
                    peer.getUltimoLatido().toString() : null);
                
                if (peer.getNombreServidor() != null) {
                    peerMap.put("nombreServidor", peer.getNombreServidor());
                }
                
                peersData.add(peerMap);
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("peers", peersData);
            responseData.put("total", peers.size());
            responseData.put("soloActivos", soloActivos);
            
            System.out.println("✓ [PeerController] Lista de peers obtenida: " + peers.size() + " peers");
            sendJsonResponse(handler, "listarPeersDisponibles", true, 
                "Lista de peers obtenida exitosamente", responseData);
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al listar peers: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "listarPeersDisponibles", false, 
                "Error interno al listar peers", null);
        }
    }
    
    /**
     * Maneja la acción de reportar un heartbeat (latido) de un peer.
     * 
     * Request payload esperado:
     * {
     *   "peerId": "uuid-del-peer",
     *   "ip": "192.168.1.10" (opcional),
     *   "puerto": 22100 (opcional)
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
                    "Campo requerido: peerId", 
                    createErrorData("peerId", "Campo requerido"));
                return;
            }
            
            String peerIdStr = payload.get("peerId").getAsString();
            UUID peerId = UUID.fromString(peerIdStr);
            
            // Reportar latido
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
            responseData.put("peerId", peerId.toString());
            responseData.put("proximoLatidoMs", intervaloHeartbeat);
            responseData.put("timestamp", java.time.LocalDateTime.now().toString());
            
            System.out.println("✓ [PeerController] Latido reportado para peer: " + peerId);
            sendJsonResponse(handler, "reportarLatido", true, 
                "Latido recibido exitosamente", responseData);
            
        } catch (IllegalArgumentException e) {
            System.err.println("✗ [PeerController] PeerId inválido: " + e.getMessage());
            sendJsonResponse(handler, "reportarLatido", false, 
                "PeerId inválido", 
                createErrorData("peerId", "Formato UUID inválido"));
                
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al reportar latido: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "reportarLatido", false, 
                "Error interno al reportar latido", null);
        }
    }

    
    /**
     * Maneja la acción de retransmitir una petición a otro peer.
     * 
     * Request payload esperado:
     * {
     *   "peerDestinoId": "uuid-del-peer-destino",
     *   "peticionOriginal": {
     *     "action": "accion",
     *     "payload": { ... }
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
            
            // Validar campos requeridos para el nuevo formato
            if (payload.has("originalRequest")) {
                // Nuevo formato: La petición original viene directamente
                handleRetransmitirPeticionNuevoFormato(payload, handler);
            } else if (payload.has("peerDestinoId") && payload.has("peticionOriginal")) {
                // Formato antiguo: Se especifica el peer destino
                handleRetransmitirPeticionFormatoAntiguo(payload, handler);
            } else {
                sendJsonResponse(handler, "retransmitirpeticion", false, 
                    "Formato de petición inválido", 
                    createErrorData("payload", "Se requiere originalRequest o peerDestinoId+peticionOriginal"));
            }
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al retransmitir petición: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "retransmitirpeticion", false, 
                "Error interno al retransmitir petición", null);
        }
    }
    
    /**
     * Maneja retransmisión en el nuevo formato (federación P2P directa).
     * Este método procesa peticiones que vienen de otros peers.
     */
    private void handleRetransmitirPeticionNuevoFormato(JsonObject payload, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando retransmisión formato nuevo (Federación P2P)");
        
        try {
            // Extraer el requestId si existe
            String requestId = payload.has("requestId") ? payload.get("requestId").getAsString() : null;
            
            // Extraer la petición original
            JsonObject originalRequestJson = payload.get("originalRequest").getAsJsonObject();
            DTORequest originalRequest = gson.fromJson(originalRequestJson, DTORequest.class);
            
            String action = originalRequest.getAction();
            System.out.println("→ [PeerController] Procesando acción retransmitida: " + action);
            
            // Procesar la acción específica
            switch (action.toLowerCase()) {
                case "crearcandirectoacanaldirecto":
                    handleCrearCanalDirectoFederado(originalRequest, handler, requestId);
                    break;
                    
                default:
                    System.err.println("✗ [PeerController] Acción no soportada para federación: " + action);
                    sendJsonResponse(handler, "retransmitirpeticion", false, 
                        "Acción no soportada: " + action, 
                        createErrorData("action", "No implementada para federación"));
            }
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error en retransmisión nuevo formato: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "retransmitirpeticion", false, 
                "Error procesando petición federada: " + e.getMessage(), null);
        }
    }
    
    /**
     * Maneja la creación de un canal directo en modo federado.
     * Este servidor tiene autoridad sobre uno de los usuarios.
     */
    private void handleCrearCanalDirectoFederado(DTORequest originalRequest, IClientHandler handler, String requestId) {
        System.out.println("→ [PeerController] Procesando crearCanalDirecto federado");
        
        try {
            JsonObject requestPayload = gson.toJsonTree(originalRequest.getPayload()).getAsJsonObject();
            
            // Extraer los IDs de usuarios
            String user1IdStr = requestPayload.get("user1Id").getAsString();
            String user2IdStr = requestPayload.get("user2Id").getAsString();
            
            UUID user1Id = UUID.fromString(user1IdStr);
            UUID user2Id = UUID.fromString(user2IdStr);
            
            System.out.println("→ [PeerController] Creando canal directo federado entre " + user1Id + " y " + user2Id);
            
            // Usar la fachada para crear el canal
            // La lógica en ChannelServiceImpl detectará que es local y lo creará
            var channelDto = chatFachada.crearCanalDirecto(user1Id, user2Id);
            
            // Preparar respuesta con los datos del canal
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("channelId", channelDto.getChannelId().toString());
            responseData.put("channelName", channelDto.getChannelName());
            responseData.put("channelType", channelDto.getChannelType());
            responseData.put("ownerId", channelDto.getOwner().getUserId().toString());
            responseData.put("peerId", channelDto.getPeerId() != null ? channelDto.getPeerId().toString() : null);
            
            if (requestId != null) {
                responseData.put("requestId", requestId);
            }
            
            System.out.println("✓ [PeerController] Canal directo federado creado exitosamente: " + channelDto.getChannelId());
            sendJsonResponse(handler, "crearCanalDirecto", true, 
                "Canal directo creado exitosamente", responseData);
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al crear canal directo federado: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "crearCanalDirecto", false, 
                "Error al crear canal: " + e.getMessage(), 
                createErrorData("federation", e.getMessage()));
        }
    }
    
    /**
     * Maneja retransmisión en el formato antiguo (forward a otro peer).
     */
    private void handleRetransmitirPeticionFormatoAntiguo(JsonObject payload, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando retransmisión formato antiguo (Forward)");
        
        try {
            String peerDestinoIdStr = payload.get("peerDestinoId").getAsString();
            UUID peerDestinoId = UUID.fromString(peerDestinoIdStr);
            
            // Parsear la petición original
            JsonObject peticionOriginalJson = payload.get("peticionOriginal").getAsJsonObject();
            DTORequest peticionOriginal = gson.fromJson(peticionOriginalJson, DTORequest.class);
            
            // Validar petición original
            if (peticionOriginal.getAction() == null || peticionOriginal.getAction().trim().isEmpty()) {
                sendJsonResponse(handler, "retransmitirPeticion", false, 
                    "La petición original debe tener una acción válida", 
                    createErrorData("peticionOriginal.action", "Campo requerido"));
                return;
            }
            
            System.out.println("→ [PeerController] Retransmitiendo acción '" + 
                peticionOriginal.getAction() + "' al peer: " + peerDestinoId);
            
            // Retransmitir usando la fachada
            DTOResponse respuestaPeer = chatFachada.retransmitirPeticion(peerDestinoId, peticionOriginal);
            
            // Preparar respuesta
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("peerDestinoId", peerDestinoId.toString());
            responseData.put("accionRetransmitida", peticionOriginal.getAction());
            responseData.put("respuestaPeer", Map.of(
                "action", respuestaPeer.getAction(),
                "status", respuestaPeer.getStatus(),
                "message", respuestaPeer.getMessage(),
                "data", respuestaPeer.getData() != null ? respuestaPeer.getData() : new HashMap<>()
            ));
            
            System.out.println("✓ [PeerController] Petición retransmitida exitosamente");
            sendJsonResponse(handler, "retransmitirPeticion", true, 
                "Petición retransmitida exitosamente", responseData);
            
        } catch (IllegalArgumentException e) {
            System.err.println("✗ [PeerController] Error de validación: " + e.getMessage());
            sendJsonResponse(handler, "retransmitirPeticion", false, 
                "Error de validación: " + e.getMessage(), 
                createErrorData("validacion", e.getMessage()));
                
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al retransmitir petición: " + e.getMessage());
            e.printStackTrace();
            
            // Determinar si es un error de comunicación con el peer
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("no está activo") || 
                                        errorMessage.contains("no encontrado") ||
                                        errorMessage.contains("comunicación"))) {
                sendJsonResponse(handler, "retransmitirPeticion", false, 
                    errorMessage, 
                    createErrorData("peer", errorMessage));
            } else {
                sendJsonResponse(handler, "retransmitirPeticion", false, 
                    "Error interno al retransmitir petición", null);
            }
        }
    }
    
    /**
     * Maneja la acción de actualizar la lista de peers.
     * Permite sincronizar la lista de peers conocidos con otro servidor.
     * 
     * Request payload esperado:
     * {
     *   "peers": [
     *     {
     *       "peerId": "uuid",
     *       "ip": "192.168.1.10",
     *       "puerto": 22100,
     *       "nombreServidor": "Servidor-A" (opcional)
     *     },
     *     ...
     *   ]
     * }
     */
    private void handleActualizarListaPeers(DTORequest request, IClientHandler handler) {
        System.out.println("→ [PeerController] Procesando actualizarListaPeers");
        
        if (!validatePayload(request.getPayload(), handler, "actualizarListaPeers")) {
            return;
        }
        
        try {
            JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            
            // Validar que exista el array de peers
            if (!payload.has("peers") || !payload.get("peers").isJsonArray()) {
                sendJsonResponse(handler, "actualizarListaPeers", false, 
                    "Se requiere un array de peers", 
                    createErrorData("peers", "Array requerido"));
                return;
            }
            
            var peersArray = payload.get("peers").getAsJsonArray();
            
            int peersAgregados = 0;
            int peersActualizados = 0;
            int peersError = 0;
            List<String> errores = new ArrayList<>();
            
            // Procesar cada peer
            for (var peerElement : peersArray) {
                try {
                    JsonObject peerJson = peerElement.getAsJsonObject();
                    
                    // Validar campos requeridos
                    if (!peerJson.has("ip") || !peerJson.has("puerto")) {
                        peersError++;
                        errores.add("Peer sin IP o puerto");
                        continue;
                    }
                    
                    String ip = peerJson.get("ip").getAsString();
                    int puerto = peerJson.get("puerto").getAsInt();
                    String nombreServidor = peerJson.has("nombreServidor") ? 
                        peerJson.get("nombreServidor").getAsString() : null;
                    
                    // Intentar agregar el peer
                    PeerResponseDto peerDto;
                    if (nombreServidor != null && !nombreServidor.trim().isEmpty()) {
                        peerDto = chatFachada.agregarPeer(ip, puerto, nombreServidor);
                    } else {
                        peerDto = chatFachada.agregarPeer(ip, puerto);
                    }
                    
                    // Determinar si fue agregado o actualizado
                    // (esto depende de la implementación del servicio)
                    peersAgregados++;
                    
                } catch (Exception e) {
                    peersError++;
                    errores.add("Error con peer: " + e.getMessage());
                    System.err.println("✗ [PeerController] Error al procesar peer: " + e.getMessage());
                }
            }
            
            // Preparar respuesta
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("totalRecibidos", peersArray.size());
            responseData.put("peersAgregados", peersAgregados);
            responseData.put("peersActualizados", peersActualizados);
            responseData.put("peersError", peersError);
            
            if (!errores.isEmpty()) {
                responseData.put("errores", errores);
            }
            
            // Obtener lista actualizada de peers
            List<PeerResponseDto> peersActuales = chatFachada.listarPeersDisponibles();
            responseData.put("totalPeersActuales", peersActuales.size());
            
            String mensaje = String.format(
                "Lista actualizada: %d agregados, %d errores de %d recibidos",
                peersAgregados, peersError, peersArray.size()
            );
            
            System.out.println("✓ [PeerController] " + mensaje);
            sendJsonResponse(handler, "actualizarListaPeers", true, mensaje, responseData);
            
        } catch (Exception e) {
            System.err.println("✗ [PeerController] Error al actualizar lista de peers: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "actualizarListaPeers", false, 
                "Error interno al actualizar lista de peers", null);
        }
    }
}
