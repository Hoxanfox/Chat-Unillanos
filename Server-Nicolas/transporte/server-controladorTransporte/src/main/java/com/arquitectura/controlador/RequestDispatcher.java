package com.arquitectura.controlador;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse; // <-- Usar el DTOResponse estándar
import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.archivos.DTODownloadInfo;
import com.arquitectura.DTO.archivos.DTOEndUpload;
import com.arquitectura.DTO.archivos.DTOStartUpload;
import com.arquitectura.DTO.archivos.DTOUploadChunk;
import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.peers.PeerResponseDto;
import com.arquitectura.DTO.usuarios.LoginRequestDto;
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
            "uploadfileforregistration",  // Subida sin autenticación para registro
            "uploadfilechunk",             // Los chunks pueden ser públicos
            "endfileupload",               // Finalizar upload puede ser público
            "reportarlatido",              // Los peers pueden reportar latido sin autenticación
            "añadirpeer",                  // Añadir un nuevo peer a la red
            "retransmitirpeticion",        // Retransmitir peticiones entre peers (P2P)
            "actualizarlistapeers"         // Recibir actualizaciones push de la lista de peers
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
                // ============================================
                // SECCIÓN 1: RUTAS DE AUTENTICACIÓN (CLIENTES)
                // ============================================
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

                // ============================================
                // SECCIÓN 2: RUTAS DE CONTACTOS Y USUARIOS (CLIENTES)
                // ============================================
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

                // ============================================
                // SECCIÓN 3: RUTAS DE CANALES (CLIENTES)
                // ============================================
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

                // ============================================
                // SECCIÓN 4: RUTAS DE TRANSFERENCIA DE ARCHIVOS (CLIENTES)
                // ============================================
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

                // ============================================
                // SECCIÓN 5: RUTAS DE PEERS (P2P - SERVER TO SERVER)
                // ============================================
                case "reportarlatido":
                {
                    Object heartbeatDataObj = request.getPayload();
                    if (heartbeatDataObj == null) {
                        sendJsonResponse(handler, "reportarLatido", false, "Datos del peer inválidos",
                            createErrorData("peerId", "El campo peerId es requerido"));
                        return;
                    }

                    JsonObject heartbeatJson = gson.toJsonTree(heartbeatDataObj).getAsJsonObject();
                    String heartbeatPeerIdStr = heartbeatJson.has("peerId") ? heartbeatJson.get("peerId").getAsString() : null;
                    String heartbeatIp = heartbeatJson.has("ip") ? heartbeatJson.get("ip").getAsString() : null;
                    Integer heartbeatPuerto = heartbeatJson.has("puerto") ? heartbeatJson.get("puerto").getAsInt() : null;

                    if (heartbeatPeerIdStr == null || heartbeatIp == null || heartbeatPuerto == null) {
                        sendJsonResponse(handler, "reportarLatido", false, "Datos del peer inválidos",
                            createErrorData("datos", "peerId, ip y puerto son requeridos"));
                        return;
                    }

                    try {
                        UUID heartbeatPeerId = UUID.fromString(heartbeatPeerIdStr);

                        com.arquitectura.DTO.peers.HeartbeatRequestDto heartbeatDto =
                            new com.arquitectura.DTO.peers.HeartbeatRequestDto(heartbeatPeerId, heartbeatIp, heartbeatPuerto);

                        com.arquitectura.DTO.peers.HeartbeatResponseDto heartbeatResponse = chatFachada.reportarLatido(heartbeatDto);

                        Map<String, Object> heartbeatResponseData = new HashMap<>();
                        heartbeatResponseData.put("proximoLatidoMs", heartbeatResponse.getProximoLatidoMs());

                        sendJsonResponse(handler, "reportarLatido", true, "Latido recibido", heartbeatResponseData);
                    } catch (IllegalArgumentException e) {
                        sendJsonResponse(handler, "reportarLatido", false, "Datos del peer inválidos",
                            createErrorData("formato", e.getMessage()));
                    } catch (Exception e) {
                        // Peer no reconocido o no registrado
                        Map<String, Object> errorData = new HashMap<>();
                        errorData.put("peerId", heartbeatPeerIdStr);
                        sendJsonResponse(handler, "reportarLatido", false, "Peer no reconocido o no registrado", errorData);
                    }
                    break;
                }

                case "añadirpeer":
                {
                    Object addPeerDataObj = request.getPayload();
                    if (addPeerDataObj == null) {
                        sendJsonResponse(handler, "añadirPeer", false, "Datos del peer inválidos",
                            createErrorData("datos", "ip y puerto son requeridos"));
                        return;
                    }

                    JsonObject addPeerJson = gson.toJsonTree(addPeerDataObj).getAsJsonObject();
                    String addPeerIp = addPeerJson.has("ip") ? addPeerJson.get("ip").getAsString() : null;
                    Integer addPeerPuerto = addPeerJson.has("puerto") ? addPeerJson.get("puerto").getAsInt() : null;

                    if (addPeerIp == null || addPeerPuerto == null) {
                        sendJsonResponse(handler, "añadirPeer", false, "Datos del peer inválidos",
                            createErrorData("datos", "ip y puerto son requeridos"));
                        return;
                    }

                    try {
                        com.arquitectura.DTO.peers.AddPeerRequestDto addPeerDto =
                            new com.arquitectura.DTO.peers.AddPeerRequestDto(addPeerIp, addPeerPuerto);

                        PeerResponseDto newPeer = chatFachada.añadirPeer(addPeerDto);

                        Map<String, Object> peerData = new HashMap<>();
                        peerData.put("peerId", newPeer.getPeerId().toString());
                        peerData.put("ip", newPeer.getIp());
                        peerData.put("puerto", newPeer.getPuerto());
                        peerData.put("conectado", newPeer.getConectado());

                        sendJsonResponse(handler, "añadirPeer", true, "Peer añadido exitosamente", peerData);
                    } catch (IllegalArgumentException e) {
                        // Error de validación (IP o puerto inválido)
                        sendJsonResponse(handler, "añadirPeer", false, "Datos del peer inválidos",
                            createErrorData("ip", "Formato de IP inválido"));
                    } catch (Exception e) {
                        // El peer ya existe
                        Map<String, Object> errorData = new HashMap<>();
                        errorData.put("ip", addPeerIp);
                        errorData.put("puerto", addPeerPuerto);
                        sendJsonResponse(handler, "añadirPeer", false, "El peer ya se encuentra en la lista", errorData);
                    }
                    break;
                }

                case "listarpeersdisponibles":
                {
                    Object listarPeersDataObj = request.getPayload();
                    if (listarPeersDataObj == null) {
                        sendJsonResponse(handler, "listarPeersDisponibles", false, "Error al obtener la lista de peers", null);
                        return;
                    }

                    JsonObject listarPeersJson = gson.toJsonTree(listarPeersDataObj).getAsJsonObject();
                    String peerIdStr = listarPeersJson.has("peerId") ? listarPeersJson.get("peerId").getAsString() : null;

                    if (peerIdStr == null || peerIdStr.isEmpty()) {
                        sendJsonResponse(handler, "listarPeersDisponibles", false, "Error al obtener la lista de peers", null);
                        return;
                    }

                    try {
                        UUID peerId = UUID.fromString(peerIdStr);

                        // Obtener la lista de peers disponibles (excluyendo el peer actual)
                        List<PeerResponseDto> peers = chatFachada.listarPeersDisponibles(peerId);

                        // Construir la respuesta según el formato del API
                        List<Map<String, Object>> peersData = new ArrayList<>();
                        for (PeerResponseDto peer : peers) {
                            Map<String, Object> peerMap = new HashMap<>();
                            peerMap.put("peerId", peer.getPeerId().toString());
                            peerMap.put("ip", peer.getIp());
                            peerMap.put("puerto", peer.getPuerto());
                            peerMap.put("conectado", peer.getConectado());
                            peersData.add(peerMap);
                        }

                        sendJsonResponse(handler, "listarPeersDisponibles", true, "Lista de peers y su estado obtenida", peersData);
                    } catch (IllegalArgumentException e) {
                        sendJsonResponse(handler, "listarPeersDisponibles", false, "Error al obtener la lista de peers", null);
                    } catch (Exception e) {
                        sendJsonResponse(handler, "listarPeersDisponibles", false, "Error al obtener la lista de peers", null);
                    }
                    break;
                }

                case "verificarestadopeer":
                {
                    Object verifyPeerDataObj = request.getPayload();
                    if (verifyPeerDataObj == null) {
                        sendJsonResponse(handler, "verificarEstadoPeer", false, "Datos del peer inválidos",
                            createErrorData("peerId", "El campo peerId es requerido"));
                        return;
                    }

                    JsonObject verifyPeerJson = gson.toJsonTree(verifyPeerDataObj).getAsJsonObject();
                    String verifyPeerIdStr = verifyPeerJson.has("peerId") ? verifyPeerJson.get("peerId").getAsString() : null;

                    if (verifyPeerIdStr == null) {
                        sendJsonResponse(handler, "verificarEstadoPeer", false, "Datos del peer inválidos",
                            createErrorData("peerId", "El campo peerId es requerido"));
                        return;
                    }

                    try {
                        UUID verifyPeerId = UUID.fromString(verifyPeerIdStr);
                        PeerResponseDto peerStatus = chatFachada.verificarEstadoPeer(verifyPeerId);

                        Map<String, Object> statusData = new HashMap<>();
                        statusData.put("peerId", peerStatus.getPeerId().toString());
                        statusData.put("ip", peerStatus.getIp());
                        statusData.put("puerto", peerStatus.getPuerto());
                        statusData.put("conectado", peerStatus.getConectado());

                        sendJsonResponse(handler, "verificarEstadoPeer", true, "Estado del peer obtenido", statusData);
                    } catch (IllegalArgumentException e) {
                        sendJsonResponse(handler, "verificarEstadoPeer", false, "Datos del peer inválidos",
                            createErrorData("peerId", "Formato de peerId inválido"));
                    } catch (Exception e) {
                        sendJsonResponse(handler, "verificarEstadoPeer", false, "Peer no encontrado",
                            Map.of("peerId", verifyPeerIdStr));
                    }
                    break;
                }

                case "retransmitirpeticion":
                {
                    Object retransmitDataObj = request.getPayload();
                    if (retransmitDataObj == null) {
                        sendJsonResponse(handler, "retransmitirPeticion", false, "Datos de retransmisión inválidos", null);
                        return;
                    }

                    try {
                        // Parsear el DTO de retransmisión
                        com.arquitectura.DTO.peers.RetransmitRequestDto retransmitDto =
                            gson.fromJson(gson.toJsonTree(retransmitDataObj), com.arquitectura.DTO.peers.RetransmitRequestDto.class);

                        // Validar el peer de origen en la fachada
                        chatFachada.retransmitirPeticion(retransmitDto);

                        // Procesar la petición del cliente (está anidada en peticionCliente)
                        Object clientRequest = retransmitDto.getPeticionCliente();
                        if (clientRequest == null) {
                            sendJsonResponse(handler, "retransmitirPeticion", false,
                                "Petición del cliente procesada, pero resultó en un error.",
                                Map.of("respuestaCliente", Map.of(
                                    "status", "error",
                                    "message", "Petición del cliente vacía o inválida",
                                    "data", null
                                )));
                            return;
                        }

                        // Convertir la petición del cliente a DTORequest
                        DTORequest clientDtoRequest = gson.fromJson(gson.toJsonTree(clientRequest), DTORequest.class);
                        String clientAction = clientDtoRequest.getAction() != null ?
                            clientDtoRequest.getAction().toLowerCase() : "unknown";

                        // Crear un handler temporal para capturar la respuesta
                        RetransmitClientHandler tempHandler = new RetransmitClientHandler();

                        // Procesar la petición del cliente usando el dispatcher recursivamente
                        String clientRequestJson = gson.toJson(clientDtoRequest);
                        dispatch(clientRequestJson, tempHandler);

                        // Obtener la respuesta del cliente procesada
                        String clientResponseJson = tempHandler.getResponse();
                        Object clientResponse = gson.fromJson(clientResponseJson, Object.class);

                        // Construir la respuesta de retransmisión
                        Map<String, Object> retransmitResponseData = new HashMap<>();
                        retransmitResponseData.put("respuestaCliente", clientResponse);

                        sendJsonResponse(handler, "retransmitirPeticion", true,
                            "Petición del cliente procesada exitosamente.", retransmitResponseData);

                    } catch (IllegalArgumentException e) {
                        // Error de validación
                        sendJsonResponse(handler, "retransmitirPeticion", false,
                            "Datos de retransmisión inválidos",
                            createErrorData("formato", e.getMessage()));
                    } catch (Exception e) {
                        // Error al procesar (peer no conectado, etc.)
                        sendJsonResponse(handler, "retransmitirPeticion", false,
                            e.getMessage(), null);
                    }
                    break;
                }

                case "actualizarlistapeers":
                {
                    Object updatePeerListDataObj = request.getPayload();
                    if (updatePeerListDataObj == null) {
                        sendJsonResponse(handler, "actualizarListaPeers", false, "Datos del peer inválidos",
                            createErrorData("listaPeers", "La lista de peers es requerida"));
                        return;
                    }

                    try {
                        // Parsear el DTO con la lista de peers
                        com.arquitectura.DTO.peers.UpdatePeerListRequestDto updatePeerListDto =
                            gson.fromJson(gson.toJsonTree(updatePeerListDataObj),
                                com.arquitectura.DTO.peers.UpdatePeerListRequestDto.class);

                        // Actualizar la lista de peers en la base de datos
                        com.arquitectura.DTO.peers.UpdatePeerListResponseDto responseDto =
                            chatFachada.actualizarListaPeers(updatePeerListDto);

                        // Construir la respuesta con la lista actualizada
                        List<Map<String, Object>> listaPeersData = new ArrayList<>();
                        for (PeerResponseDto peer : responseDto.getListaPeers()) {
                            Map<String, Object> peerMap = new HashMap<>();
                            peerMap.put("peerId", peer.getPeerId().toString());
                            peerMap.put("ip", peer.getIp());
                            peerMap.put("puerto", peer.getPuerto());
                            peerMap.put("conectado", peer.getConectado());
                            listaPeersData.add(peerMap);
                        }

                        Map<String, Object> data = new HashMap<>();
                        data.put("listaPeers", listaPeersData);

                        sendJsonResponse(handler, "actualizarListaPeers", true,
                            "Peer añadido y lista de peers actualizada", data);
                    } catch (IllegalArgumentException e) {
                        // Error de validación
                        sendJsonResponse(handler, "actualizarListaPeers", false, "Datos del peer inválidos",
                            createErrorData("validacion", e.getMessage()));
                    } catch (Exception e) {
                        // Error general
                        sendJsonResponse(handler, "actualizarListaPeers", false,
                            "Error interno del servidor al actualizar la lista de peers", null);
                    }
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
