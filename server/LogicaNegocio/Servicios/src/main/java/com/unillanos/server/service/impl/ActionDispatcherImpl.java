package com.unillanos.server.service.impl;

import com.google.gson.Gson;
import com.unillanos.server.dto.*;
import com.unillanos.server.dto.response.*;
import com.unillanos.server.exception.ValidationException;
import com.unillanos.server.service.interfaces.IActionDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación del despachador de acciones.
 * Enruta las peticiones según el campo 'action' del DTORequest.
 */
@Service
public class ActionDispatcherImpl implements IActionDispatcher {
    
    private static final Logger logger = LoggerFactory.getLogger(ActionDispatcherImpl.class);
    
    private final GlobalExceptionHandler exceptionHandler;
    private final LoggerService loggerService;
    private final AutenticacionService autenticacionService;
    private final CanalService canalService;
    private final MensajeriaService mensajeriaService;
    private final ArchivoService archivoService;
    private final ChunkingService chunkingService;
    private final NotificationManager notificationManager;
    private final AdminUsersService adminUsersService;
    private final ConnectionManager connectionManager;
    private final NotificationService notificationService;
    private final ContactService contactService;
    private final Gson gson;

    public ActionDispatcherImpl(GlobalExceptionHandler exceptionHandler, 
                                LoggerService loggerService,
                                AutenticacionService autenticacionService,
                                CanalService canalService,
                                MensajeriaService mensajeriaService,
                                ArchivoService archivoService,
                                ChunkingService chunkingService,
                                NotificationManager notificationManager,
                                AdminUsersService adminUsersService,
                                ConnectionManager connectionManager,
                                NotificationService notificationService,
                                ContactService contactService) {
        this.exceptionHandler = exceptionHandler;
        this.loggerService = loggerService;
        this.autenticacionService = autenticacionService;
        this.canalService = canalService;
        this.mensajeriaService = mensajeriaService;
        this.archivoService = archivoService;
        this.chunkingService = chunkingService;
        this.notificationManager = notificationManager;
        this.adminUsersService = adminUsersService;
        this.connectionManager = connectionManager;
        this.notificationService = notificationService;
        this.contactService = contactService;
        this.gson = new Gson();
    }

    @Override
    public DTOResponse dispatch(DTORequest request, ChannelHandlerContext ctx) {
        String ipAddress = extractIpAddress(ctx);
        String action = request.getAction();
        
        try {
            logger.debug("Despachando acción: {} desde IP: {}", action, ipAddress);
            
            // Validar que la acción no sea nula o vacía
            if (action == null || action.isEmpty()) {
                throw new ValidationException("La acción no puede estar vacía", "action");
            }

            // Registrar la acción recibida
            loggerService.logInfo(action, "Acción recibida desde IP: " + ipAddress);

            // Enrutamiento de acciones
            return switch (action) {
                case "ping" -> handlePing();
                
                // --- ACCIONES DE USUARIOS ---
                case "registro" -> handleRegistro(request, ipAddress);
                case "login" -> handleLogin(request, ctx, ipAddress);
                // Adaptaciones para coincidir con el cliente
                case "registerUser" -> handleRegistro(request, ipAddress);
                case "authenticateUser" -> handleLogin(request, ctx, ipAddress);
                case "logout" -> handleLogout(request, ipAddress);
                case "actualizarPerfil" -> handleActualizarPerfil(request);
                case "cambiarEstado" -> handleCambiarEstado(request);
                
                // --- ACCIONES DE CANALES ---
                case "crearCanal" -> handleCrearCanal(request);
                case "unirseCanal" -> handleUnirseCanal(request);
                case "salirCanal" -> handleSalirCanal(request);
                case "listarCanales" -> handleListarCanales(request);
                case "listarMiembros" -> handleListarMiembros(request);
                case "gestionarMiembro" -> handleGestionarMiembro(request);
                
                // --- ACCIONES DE MENSAJERÍA ---
                case "enviarMensajeDirecto" -> handleEnviarMensajeDirecto(request);
                case "enviarMensajeCanal" -> handleEnviarMensajeCanal(request);
                // Adaptaciones para coincidir con el cliente
                case "enviarMensajePrivado" -> handleEnviarMensajeDirecto(request);
                case "obtenerHistorial" -> handleObtenerHistorial(request);
                case "solicitarHistorialPrivado" -> handleSolicitarHistorialPrivado(request);
                case "solicitarHistorialCanal" -> handleSolicitarHistorialCanal(request);
                case "marcar_mensaje_leido" -> handleMarcarMensajeLeido(request);
                
                // --- ACCIONES DE CONTACTOS ---
                case "solicitarListaContactos" -> handleSolicitarListaContactos(request);
                
                // --- ACCIONES DE ARCHIVOS ---
                case "subirArchivo" -> handleSubirArchivo(request);
                case "descargarArchivo" -> handleDescargarArchivo(request);
                case "listarArchivos" -> handleListarArchivos(request);
                
                
                // --- ACCIONES DE SUBIDA DE ARCHIVOS (Cliente) ---
                case "startFileUpload" -> handleStartFileUpload(request, ctx);
                case "endFileUpload" -> handleEndFileUpload(request);
                
                // --- ACCIONES DE SUBIDA DE ARCHIVOS (Solo para usuarios autenticados) ---
                case "uploadFileChunk" -> handleUploadFileChunk(request, ctx);
                
                // --- ACCIONES DE SUBIDA DE ARCHIVOS PARA REGISTRO (Sin autenticación) ---
                case "uploadFileForRegistration" -> handleUploadFileForRegistration(request);
                case "uploadFileChunkForRegistration" -> handleUploadFileChunkForRegistration(request);
                case "endFileUploadForRegistration" -> handleEndFileUploadForRegistration(request);
                
                // --- ACCIONES DE NOTIFICACIONES ---
                case "suscribir_notificaciones" -> handleSuscribirNotificaciones(request, ctx);
                case "desuscribir_notificaciones" -> handleDesuscribirNotificaciones(request);
                case "obtenerNotificaciones" -> handleObtenerNotificaciones(request);
                case "marcarNotificacionLeida" -> handleMarcarNotificacionLeida(request);
                case "responderSolicitudAmistad" -> handleResponderSolicitudAmistad(request);
                case "responderInvitacionCanal" -> handleResponderInvitacionCanal(request);
                
                default -> DTOResponse.error(action, "Acción no reconocida: " + action);
            };
            
        } catch (Exception e) {
            return exceptionHandler.handleException(e, action, null, ipAddress);
        }
    }

    /**
     * Maneja la acción PING - verifica que el servidor esté activo.
     *
     * @return DTOResponse con pong y timestamp
     */
    private DTOResponse handlePing() {
        Map<String, String> data = Map.of(
            "message", "pong",
            "timestamp", java.time.LocalDateTime.now().toString(),
            "version", "1.0.0"
        );
        return DTOResponse.success("ping", "Servidor activo", data);
    }

    // --- MÉTODOS PRIVADOS PARA MANEJAR CADA ACCIÓN ---

    /**
     * Maneja la acción de registro de nuevos usuarios.
     */
    private DTOResponse handleRegistro(DTORequest request, String ipAddress) {
        // El cliente envía name, pero el servidor espera nombre
        @SuppressWarnings("unchecked")
        Map<String, Object> clientPayload = (Map<String, Object>) request.getPayload();
        
        String name = (String) clientPayload.get("name");
        String email = (String) clientPayload.get("email");
        String password = (String) clientPayload.get("password");
        String photoId = (String) clientPayload.get("photoId");
        
        if (name == null || email == null || password == null) {
            return DTOResponse.error("registerUser", "Faltan datos requeridos en el payload");
        }
        
        // Crear el DTO que espera el servidor (name -> nombre)
        DTORegistro dto = new DTORegistro(name, email, password, photoId);
        return autenticacionService.registrarUsuario(dto, ipAddress);
    }

    /**
     * Maneja la acción de login.
     */
    private DTOResponse handleLogin(DTORequest request, ChannelHandlerContext ctx, String ipAddress) {
        // El cliente envía emailUsuario y passwordUsuario, pero el servidor espera email y password
        @SuppressWarnings("unchecked")
        Map<String, String> clientPayload = (Map<String, String>) request.getPayload();
        
        String email = clientPayload.get("emailUsuario");
        String password = clientPayload.get("passwordUsuario");
        
        if (email == null || password == null) {
            return DTOResponse.error("authenticateUser", "Faltan datos requeridos en el payload");
        }
        
        // Crear el DTO que espera el servidor
        DTOLogin dto = new DTOLogin(email, password);
        return autenticacionService.autenticarUsuario(dto, ctx, ipAddress);
    }

    /**
     * Maneja la acción de logout.
     */
    private DTOResponse handleLogout(DTORequest request, String ipAddress) {
        // El userId viene en el payload
        @SuppressWarnings("unchecked")
        Map<String, String> payload = (Map<String, String>) request.getPayload();
        String userId = payload.get("userId");
        return autenticacionService.logout(userId, ipAddress);
    }

    /**
     * Maneja la acción de actualizar perfil.
     */
    private DTOResponse handleActualizarPerfil(DTORequest request) {
        DTOActualizarPerfil dto = gson.fromJson(gson.toJson(request.getPayload()), DTOActualizarPerfil.class);
        return autenticacionService.actualizarPerfil(dto);
    }

    /**
     * Maneja la acción de cambiar estado.
     */
    private DTOResponse handleCambiarEstado(DTORequest request) {
        DTOCambiarEstado dto = gson.fromJson(gson.toJson(request.getPayload()), DTOCambiarEstado.class);
        return autenticacionService.cambiarEstado(dto);
    }

    // --- MÉTODOS PRIVADOS PARA MANEJAR ACCIONES DE CANALES ---

    /**
     * Maneja la acción de crear un canal.
     */
    private DTOResponse handleCrearCanal(DTORequest request) {
        DTOCrearCanal dto = gson.fromJson(gson.toJson(request.getPayload()), DTOCrearCanal.class);
        return canalService.crearCanal(dto);
    }

    /**
     * Maneja la acción de unirse a un canal.
     */
    private DTOResponse handleUnirseCanal(DTORequest request) {
        DTOUnirseCanal dto = gson.fromJson(gson.toJson(request.getPayload()), DTOUnirseCanal.class);
        return canalService.unirseCanal(dto);
    }

    /**
     * Maneja la acción de salir de un canal.
     */
    private DTOResponse handleSalirCanal(DTORequest request) {
        DTOSalirCanal dto = gson.fromJson(gson.toJson(request.getPayload()), DTOSalirCanal.class);
        return canalService.salirCanal(dto);
    }

    /**
     * Maneja la acción de listar canales.
     */
    private DTOResponse handleListarCanales(DTORequest request) {
        DTOListarCanales dto = gson.fromJson(gson.toJson(request.getPayload()), DTOListarCanales.class);
        return canalService.listarCanales(dto);
    }

    /**
     * Maneja la acción de listar miembros de un canal.
     */
    private DTOResponse handleListarMiembros(DTORequest request) {
        @SuppressWarnings("unchecked")
        Map<String, String> payload = (Map<String, String>) request.getPayload();
        String canalId = payload.get("canalId");
        String solicitanteId = payload.get("solicitanteId");
        return canalService.listarMiembros(canalId, solicitanteId);
    }

    /**
     * Maneja la acción de gestionar miembros de un canal.
     */
    private DTOResponse handleGestionarMiembro(DTORequest request) {
        DTOGestionarMiembro dto = gson.fromJson(gson.toJson(request.getPayload()), DTOGestionarMiembro.class);
        return canalService.gestionarMiembro(dto);
    }

    // --- MÉTODOS PRIVADOS PARA MANEJAR ACCIONES DE MENSAJERÍA ---

    /**
     * Maneja la acción de enviar un mensaje directo.
     */
    private DTOResponse handleEnviarMensajeDirecto(DTORequest request) {
        DTOEnviarMensaje dto = gson.fromJson(gson.toJson(request.getPayload()), DTOEnviarMensaje.class);
        return mensajeriaService.enviarMensajeDirecto(dto);
    }

    /**
     * Maneja la acción de enviar un mensaje a un canal.
     */
    private DTOResponse handleEnviarMensajeCanal(DTORequest request) {
        DTOEnviarMensaje dto = gson.fromJson(gson.toJson(request.getPayload()), DTOEnviarMensaje.class);
        return mensajeriaService.enviarMensajeCanal(dto);
    }

    /**
     * Maneja la acción de obtener el historial de mensajes.
     */
    private DTOResponse handleObtenerHistorial(DTORequest request) {
        DTOHistorial dto = gson.fromJson(gson.toJson(request.getPayload()), DTOHistorial.class);
        return mensajeriaService.obtenerHistorial(dto);
    }

    /**
     * Maneja la acción de solicitar la lista de contactos (usuarios).
     */
    private DTOResponse handleSolicitarListaContactos(DTORequest request) {
        // El cliente no envía payload específico, solo solicita la lista
        // Obtenemos todos los usuarios disponibles como contactos
        try {
            var usuarios = adminUsersService.listUsers(null, 100, 0, null);
            return DTOResponse.success("solicitarListaContactos", 
                "Lista de contactos obtenida exitosamente", 
                usuarios);
        } catch (Exception e) {
            logger.error("Error al obtener lista de contactos", e);
            return DTOResponse.error("solicitarListaContactos", 
                "Error al obtener la lista de contactos: " + e.getMessage());
        }
    }

    // --- MÉTODOS PRIVADOS PARA MANEJAR ACCIONES DE ARCHIVOS ---

    private DTOResponse handleSubirArchivo(DTORequest request) {
        DTOSubirArchivo dto = gson.fromJson(gson.toJson(request.getPayload()), DTOSubirArchivo.class);
        return archivoService.subirArchivo(dto);
    }

    private DTOResponse handleDescargarArchivo(DTORequest request) {
        DTODescargarArchivo dto = gson.fromJson(gson.toJson(request.getPayload()), DTODescargarArchivo.class);
        return archivoService.descargarArchivo(dto);
    }

    private DTOResponse handleListarArchivos(DTORequest request) {
        DTOListarArchivos dto = gson.fromJson(gson.toJson(request.getPayload()), DTOListarArchivos.class);
        return archivoService.listarArchivos(dto);
    }

    // --- HANDLERS DE NOTIFICACIONES ---

    private DTOResponse handleSuscribirNotificaciones(DTORequest request, ChannelHandlerContext ctx) {
        DTOSuscribirNotificaciones dto = gson.fromJson(gson.toJson(request.getPayload()), DTOSuscribirNotificaciones.class);
        
        // Generar un ID único para el cliente si no se proporciona
        String clienteId = dto.getClienteId();
        if (clienteId == null || clienteId.trim().isEmpty()) {
            clienteId = "gui-" + System.currentTimeMillis();
        }
        
        notificationManager.suscribir(clienteId, ctx, dto.getTiposInteres());
        
        return DTOResponse.success("suscribir_notificaciones", 
            "Suscripción exitosa", 
            Map.of("clienteId", clienteId, "tipos", dto.getTiposInteres()));
    }

    private DTOResponse handleDesuscribirNotificaciones(DTORequest request) {
        DTOSuscribirNotificaciones dto = gson.fromJson(gson.toJson(request.getPayload()), DTOSuscribirNotificaciones.class);
        
        notificationManager.desuscribir(dto.getClienteId());
        
        return DTOResponse.success("desuscribir_notificaciones", 
            "Desuscripción exitosa", 
            Map.of("clienteId", dto.getClienteId()));
    }

    /**
     * Extrae la dirección IP del cliente desde el contexto de Netty.
     *
     * @param ctx Contexto del canal de Netty
     * @return Dirección IP del cliente
     */
    private String extractIpAddress(ChannelHandlerContext ctx) {
        try {
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            return socketAddress.getAddress().getHostAddress();
        } catch (Exception e) {
            logger.warn("No se pudo extraer la IP del cliente", e);
            return "unknown";
        }
    }

    /**
     * Maneja la acción marcar_mensaje_leido.
     *
     * @param request DTORequest con los datos del mensaje a marcar como leído
     * @return DTOResponse con el resultado de la operación
     */
    private DTOResponse handleMarcarMensajeLeido(DTORequest request) {
        DTOMarcarMensajeLeido dto = gson.fromJson(gson.toJson(request.getPayload()), DTOMarcarMensajeLeido.class);
        return mensajeriaService.marcarComoLeido(dto);
    }

    /**
     * Maneja la acción startFileUpload del cliente.
     * Adapta el payload del cliente al formato esperado por el servidor.
     *
     * @param request DTORequest con los datos de inicio de subida del cliente
     * @param ctx Contexto de Netty para obtener el userId
     * @return DTOResponse con el resultado de la operación
     */
    private DTOResponse handleStartFileUpload(DTORequest request, ChannelHandlerContext ctx) {
        try {
            // Obtener el userId del contexto de conexión
            String userId = connectionManager.getUserIdByContext(ctx);
            if (userId == null) {
                return DTOResponse.error("startFileUpload", "Usuario no autenticado");
            }

            // Deserializar el payload del cliente
            @SuppressWarnings("unchecked")
            Map<String, Object> clientPayload = (Map<String, Object>) request.getPayload();
            
            String fileName = (String) clientPayload.get("fileName");
            String fileMimeType = (String) clientPayload.get("fileMimeType");
            
            // Manejar tanto Integer como Double para totalChunks
            Object totalChunksObj = clientPayload.get("totalChunks");
            int totalChunks;
            if (totalChunksObj instanceof Integer) {
                totalChunks = (Integer) totalChunksObj;
            } else if (totalChunksObj instanceof Double) {
                totalChunks = ((Double) totalChunksObj).intValue();
            } else {
                return DTOResponse.error("startFileUpload", "totalChunks debe ser un número");
            }
            
            if (fileName == null || fileMimeType == null || totalChunksObj == null) {
                return DTOResponse.error("startFileUpload", "Faltan datos requeridos en el payload");
            }
            
            // Calcular el tamaño total estimado (usando el mismo CHUNK_SIZE que el cliente: 512KB)
            // En un escenario real, el cliente debería enviar el tamaño real
            long estimatedSize = totalChunks * 1024 * 512; // 512 KB por chunk, igual que el cliente
            
            // Crear el DTO que espera el servidor
            DTOIniciarSubida dto = new DTOIniciarSubida(
                userId,
                fileName,
                fileMimeType,
                estimatedSize,
                totalChunks
            );
            
            // Llamar al método existente
            DTOResponse response = chunkingService.iniciarSubida(dto);
            
            // Cambiar la acción de la respuesta para que coincida con la petición del cliente
            if ("success".equals(response.getStatus())) {
                return DTOResponse.success("startFileUpload", response.getMessage(), response.getData());
            } else {
                return DTOResponse.error("startFileUpload", response.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error al procesar startFileUpload", e);
            return DTOResponse.error("startFileUpload", "Error interno del servidor: " + e.getMessage());
        }
    }


    /**
     * Maneja la subida de chunks de archivos.
     * Requiere que el usuario esté autenticado.
     *
     * @param request DTORequest con los datos del chunk
     * @param ctx Contexto de Netty para obtener el userId
     * @return DTOResponse con el resultado de la operación
     */
    private DTOResponse handleUploadFileChunk(DTORequest request, ChannelHandlerContext ctx) {
        try {
            // Obtener el userId del contexto de conexión
            String userId = connectionManager.getUserIdByContext(ctx);
            if (userId == null) {
                return DTOResponse.error("uploadFileChunk", "Usuario no autenticado");
            }

            // Deserializar el payload del cliente
            @SuppressWarnings("unchecked")
            Map<String, Object> clientPayload = (Map<String, Object>) request.getPayload();
            
            String uploadId = (String) clientPayload.get("uploadId");
            Object chunkNumberObj = clientPayload.get("chunkNumber");
            // El cliente envía chunkData_base64, pero también puede venir como chunkData
            String chunkDataBase64 = (String) clientPayload.get("chunkData_base64");
            if (chunkDataBase64 == null) {
                chunkDataBase64 = (String) clientPayload.get("chunkData");
            }
            
            // Validar campos requeridos
            if (uploadId == null || chunkNumberObj == null || chunkDataBase64 == null) {
                return DTOResponse.error("uploadFileChunk", "Faltan datos requeridos en el payload");
            }
            
            // Manejar tanto Integer como Double para chunkNumber
            int chunkNumber;
            if (chunkNumberObj instanceof Integer) {
                chunkNumber = (Integer) chunkNumberObj;
            } else if (chunkNumberObj instanceof Double) {
                chunkNumber = ((Double) chunkNumberObj).intValue();
            } else {
                return DTOResponse.error("uploadFileChunk", "chunkNumber debe ser un número");
            }
            
            // El uploadId del cliente es el sessionId del servidor
            String sessionId = uploadId;
            
            // Obtener información de la sesión para completar el DTO
            DTOResponse sessionInfo = chunkingService.obtenerInformacionSesion(sessionId);
            if (!"success".equals(sessionInfo.getStatus())) {
                return DTOResponse.error("uploadFileChunk", "No se pudo obtener información de la sesión");
            }
            
            // Extraer información de la sesión
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionData = (Map<String, Object>) sessionInfo.getData();
            String nombreArchivo = (String) sessionData.get("nombreArchivo");
            String tipoMime = (String) sessionData.get("tipoMime");
            Object tamanoTotalObj = sessionData.get("tamanoTotal");
            Object totalChunksObj = sessionData.get("totalChunks");
            
            long tamanoTotal;
            int totalChunks;
            if (tamanoTotalObj instanceof Integer) {
                tamanoTotal = ((Integer) tamanoTotalObj).longValue();
            } else if (tamanoTotalObj instanceof Long) {
                tamanoTotal = (Long) tamanoTotalObj;
            } else if (tamanoTotalObj instanceof Double) {
                tamanoTotal = ((Double) tamanoTotalObj).longValue();
            } else {
                return DTOResponse.error("uploadFileChunk", "Tipo de datos inválido para tamanoTotal");
            }
            
            if (totalChunksObj instanceof Integer) {
                totalChunks = (Integer) totalChunksObj;
            } else if (totalChunksObj instanceof Double) {
                totalChunks = ((Double) totalChunksObj).intValue();
            } else {
                return DTOResponse.error("uploadFileChunk", "Tipo de datos inválido para totalChunks");
            }
            
            // Crear el DTO completo que espera el servidor
            DTOSubirArchivoChunk dto = new DTOSubirArchivoChunk();
            dto.setSessionId(sessionId);
            dto.setUsuarioId(userId); // Usar el userId real del usuario autenticado
            dto.setNombreArchivo(nombreArchivo);
            dto.setTipoMime(tipoMime);
            dto.setTamanoTotal(tamanoTotal);
            dto.setNumeroChunk(chunkNumber);
            dto.setTotalChunks(totalChunks);
            dto.setBase64ChunkData(chunkDataBase64);
            dto.setHashChunk(""); // Hash vacío para simplificar
            
            // Llamar al método normal de subida de chunks
            DTOResponse response = chunkingService.subirChunk(dto);
            
            // Generar la acción específica que espera el cliente
            String clientExpectedAction = "uploadFileChunk_" + uploadId + "_" + chunkNumber;
            
            // Cambiar la acción de la respuesta para que coincida con la petición del cliente
            if ("success".equals(response.getStatus())) {
                return DTOResponse.success(clientExpectedAction, response.getMessage(), response.getData());
            } else {
                return DTOResponse.error(clientExpectedAction, response.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error al procesar uploadFileChunk", e);
            return DTOResponse.error("uploadFileChunk", "Error interno del servidor: " + e.getMessage());
        }
    }

    /**
     * Maneja la finalización de la subida de archivos.
     * Ensambla los chunks y crea la entrada en la base de datos.
     */
    private DTOResponse handleEndFileUpload(DTORequest request) {
        try {
            // Deserializar el payload del cliente
            @SuppressWarnings("unchecked")
            Map<String, String> clientPayload = (Map<String, String>) request.getPayload();
            
            String uploadId = clientPayload.get("uploadId");
            String fileHash = clientPayload.get("fileHash_sha256"); // El cliente envía fileHash_sha256
            
            if (uploadId == null || fileHash == null) {
                return DTOResponse.error("endFileUpload", "Faltan datos requeridos en el payload");
            }
            
            // Llamar al servicio de chunking para finalizar la subida
            DTOResponse response = chunkingService.finalizarSubida(uploadId);
            
            // Cambiar la acción de la respuesta para que coincida con la petición del cliente
            if ("success".equals(response.getStatus())) {
                return DTOResponse.success("endFileUpload", response.getMessage(), response.getData());
            } else {
                return DTOResponse.error("endFileUpload", response.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error al procesar endFileUpload", e);
            return DTOResponse.error("endFileUpload", "Error interno del servidor: " + e.getMessage());
        }
    }

    // ===== NUEVOS MÉTODOS PARA FASE 1 =====

    /**
     * Maneja la solicitud de historial de canal.
     * El cliente envía: { "canalId": "uuid", "limite": 50 }
     */
    private DTOResponse handleSolicitarHistorialCanal(DTORequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) request.getPayload();
            
            String canalId = (String) payload.get("canalId");
            Object limiteObj = payload.get("limite");
            int limite = 50; // Valor por defecto
            
            if (limiteObj instanceof Integer) {
                limite = (Integer) limiteObj;
            } else if (limiteObj instanceof Double) {
                limite = ((Double) limiteObj).intValue();
            }
            
            if (canalId == null || canalId.trim().isEmpty()) {
                return DTOResponse.error("solicitarHistorialCanal", "canalId es requerido");
            }
            
            // Crear DTOHistorial para el servicio existente
            DTOHistorial dto = new DTOHistorial();
            dto.setCanalId(canalId);
            dto.setLimit(limite);
            dto.setOffset(0);
            
            // Usar el servicio existente y convertir a DTO específico
            DTOResponse response = mensajeriaService.obtenerHistorial(dto);
            
            if ("success".equals(response.getStatus())) {
                @SuppressWarnings("unchecked")
                List<DTOMensaje> mensajes = (List<DTOMensaje>) response.getData();
                
                // Convertir a DTOs específicos para el cliente
                List<DTOMensajeCanalResponse> mensajesResponse = mensajes.stream()
                    .map(this::convertirAMensajeCanalResponse)
                    .toList();
                
                // Crear respuesta estructurada
                DTOResponseHistorialCanal historialResponse = new DTOResponseHistorialCanal(
                    mensajesResponse,
                    mensajes.size() >= limite, // Si devolvió el límite completo, probablemente hay más
                    mensajes.size(),
                    canalId,
                    "Canal" // TODO: Obtener nombre real del canal desde CanalRepository
                );
                
                return DTOResponse.success("solicitarHistorialCanal", 
                    String.format("Historial obtenido: %d mensajes", mensajes.size()), 
                    historialResponse);
            } else {
                return DTOResponse.error("solicitarHistorialCanal", response.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error al procesar solicitarHistorialCanal", e);
            return DTOResponse.error("solicitarHistorialCanal", "Error interno del servidor: " + e.getMessage());
        }
    }

    /**
     * Maneja la solicitud de historial privado.
     * El cliente envía solo el contactoId como string
     */
    private DTOResponse handleSolicitarHistorialPrivado(DTORequest request) {
        try {
            String contactoId = (String) request.getPayload();
            
            if (contactoId == null || contactoId.trim().isEmpty()) {
                return DTOResponse.error("solicitarHistorialPrivado", "contactoId es requerido");
            }
            
            // Crear DTOHistorial para el servicio existente
            DTOHistorial dto = new DTOHistorial();
            dto.setDestinatarioId(contactoId);
            dto.setLimit(50); // Valor por defecto
            dto.setOffset(0);
            
            // Usar el servicio existente y convertir a DTO específico
            DTOResponse response = mensajeriaService.obtenerHistorial(dto);
            
            if ("success".equals(response.getStatus())) {
                @SuppressWarnings("unchecked")
                List<DTOMensaje> mensajes = (List<DTOMensaje>) response.getData();
                
                // Convertir a DTOs específicos para el cliente
                List<DTOMensajePrivadoResponse> mensajesResponse = mensajes.stream()
                    .map(this::convertirAMensajePrivadoResponse)
                    .toList();
                
                // Crear respuesta estructurada
                DTOResponseHistorialPrivado historialResponse = new DTOResponseHistorialPrivado(
                    mensajesResponse,
                    mensajes.size() >= 50, // Si devolvió el límite completo, probablemente hay más
                    mensajes.size(),
                    contactoId,
                    "Contacto" // TODO: Obtener nombre real del contacto desde UsuarioRepository
                );
                
                return DTOResponse.success("solicitarHistorialPrivado", 
                    String.format("Historial obtenido: %d mensajes", mensajes.size()), 
                    historialResponse);
            } else {
                return DTOResponse.error("solicitarHistorialPrivado", response.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error al procesar solicitarHistorialPrivado", e);
            return DTOResponse.error("solicitarHistorialPrivado", "Error interno del servidor: " + e.getMessage());
        }
    }

    /**
     * Maneja la solicitud de notificaciones.
     * El cliente envía: { "usuarioId": "uuid" }
     */
    private DTOResponse handleObtenerNotificaciones(DTORequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) request.getPayload();
            
            String usuarioId = (String) payload.get("usuarioId");
            
            if (usuarioId == null || usuarioId.trim().isEmpty()) {
                return DTOResponse.error("obtenerNotificaciones", "usuarioId es requerido");
            }
            
            // Usar el NotificationService real
            return notificationService.obtenerNotificaciones(usuarioId);
            
        } catch (Exception e) {
            logger.error("Error al procesar obtenerNotificaciones", e);
            return DTOResponse.error("obtenerNotificaciones", "Error interno del servidor: " + e.getMessage());
        }
    }

    /**
     * Maneja marcar notificación como leída.
     * El cliente envía: { "notificacionId": "uuid" }
     */
    private DTOResponse handleMarcarNotificacionLeida(DTORequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) request.getPayload();
            
            String notificacionId = (String) payload.get("notificacionId");
            
            if (notificacionId == null || notificacionId.trim().isEmpty()) {
                return DTOResponse.error("marcarNotificacionLeida", "notificacionId es requerido");
            }
            
            // Usar el NotificationService real
            return notificationService.marcarNotificacionLeida(notificacionId);
            
        } catch (Exception e) {
            logger.error("Error al procesar marcarNotificacionLeida", e);
            return DTOResponse.error("marcarNotificacionLeida", "Error interno del servidor: " + e.getMessage());
        }
    }

    /**
     * Maneja responder solicitud de amistad.
     * El cliente envía: { "solicitudId": "uuid", "usuarioId": "uuid", "aceptar": true/false }
     */
    private DTOResponse handleResponderSolicitudAmistad(DTORequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) request.getPayload();
            
            String solicitudId = (String) payload.get("solicitudId");
            String usuarioId = (String) payload.get("usuarioId");
            Boolean aceptar = (Boolean) payload.get("aceptar");
            
            if (solicitudId == null || solicitudId.trim().isEmpty()) {
                return DTOResponse.error("responderSolicitudAmistad", "solicitudId es requerido");
            }
            
            if (usuarioId == null || usuarioId.trim().isEmpty()) {
                return DTOResponse.error("responderSolicitudAmistad", "usuarioId es requerido");
            }
            
            if (aceptar == null) {
                return DTOResponse.error("responderSolicitudAmistad", "aceptar es requerido");
            }
            
            // Usar el ContactService real
            return contactService.responderSolicitudAmistad(solicitudId, usuarioId, aceptar);
            
        } catch (Exception e) {
            logger.error("Error al procesar responderSolicitudAmistad", e);
            return DTOResponse.error("responderSolicitudAmistad", "Error interno del servidor: " + e.getMessage());
        }
    }

    /**
     * Maneja responder invitación a canal.
     * El cliente envía: { "invitacionId": "uuid", "usuarioId": "uuid", "aceptar": true/false }
     */
    private DTOResponse handleResponderInvitacionCanal(DTORequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) request.getPayload();
            
            String invitacionId = (String) payload.get("invitacionId");
            String usuarioId = (String) payload.get("usuarioId");
            Boolean aceptar = (Boolean) payload.get("aceptar");
            
            if (invitacionId == null || invitacionId.trim().isEmpty()) {
                return DTOResponse.error("responderInvitacionCanal", "invitacionId es requerido");
            }
            
            if (usuarioId == null || usuarioId.trim().isEmpty()) {
                return DTOResponse.error("responderInvitacionCanal", "usuarioId es requerido");
            }
            
            if (aceptar == null) {
                return DTOResponse.error("responderInvitacionCanal", "aceptar es requerido");
            }
            
            // Por ahora retornar una respuesta de éxito
            // TODO: Implementar CanalService.invitarMiembro cuando esté disponible
            String mensaje = aceptar ? "Invitación a canal aceptada" : "Invitación a canal rechazada";
            return DTOResponse.success("responderInvitacionCanal", mensaje, null);
            
        } catch (Exception e) {
            logger.error("Error al procesar responderInvitacionCanal", e);
            return DTOResponse.error("responderInvitacionCanal", "Error interno del servidor: " + e.getMessage());
        }
    }

    // ===== MÉTODOS DE CONVERSIÓN DE DTOs =====

    /**
     * Convierte DTOMensaje a DTOMensajeCanalResponse
     */
    private DTOMensajeCanalResponse convertirAMensajeCanalResponse(DTOMensaje mensaje) {
        return new DTOMensajeCanalResponse(
            mensaje.getId() != null ? mensaje.getId().toString() : null,
            mensaje.getCanalId(),
            mensaje.getRemitenteId(),
            mensaje.getRemitenteNombre(),
            mensaje.getContenido(),
            mensaje.getTipo(),
                mensaje.getFileId(),
            mensaje.getFechaEnvio() != null ? java.time.LocalDateTime.parse(mensaje.getFechaEnvio()) : null,
                mensaje.getFileName()
        );
    }

    /**
     * Convierte DTOMensaje a DTOMensajePrivadoResponse
     */
    private DTOMensajePrivadoResponse convertirAMensajePrivadoResponse(DTOMensaje mensaje) {
        return new DTOMensajePrivadoResponse(
            mensaje.getId() != null ? mensaje.getId().toString() : null,
            mensaje.getRemitenteId(),
            mensaje.getDestinatarioId(),
            mensaje.getRemitenteNombre(),
            mensaje.getDestinatarioNombre(),
            mensaje.getContenido(),
            mensaje.getTipo(),
                mensaje.getFileId(),
            mensaje.getFechaEnvio() != null ? java.time.LocalDateTime.parse(mensaje.getFechaEnvio()) : null,
                mensaje.getFileName()
        );
    }

    // --- HANDLERS DE SUBIDA DE ARCHIVOS PARA REGISTRO ---

    /**
     * Maneja la acción uploadFileForRegistration del cliente.
     * Permite iniciar la subida de archivos durante el proceso de registro sin autenticación.
     *
     * @param request DTORequest con los datos de inicio de subida del cliente
     * @return DTOResponse con el resultado de la operación
     */
    private DTOResponse handleUploadFileForRegistration(DTORequest request) {
        try {
            // Deserializar el payload del cliente
            @SuppressWarnings("unchecked")
            Map<String, Object> clientPayload = (Map<String, Object>) request.getPayload();
            
            String fileName = (String) clientPayload.get("fileName");
            String fileMimeType = (String) clientPayload.get("fileMimeType");
            
            // Manejar tanto Integer como Double para totalChunks
            Object totalChunksObj = clientPayload.get("totalChunks");
            int totalChunks;
            if (totalChunksObj instanceof Integer) {
                totalChunks = (Integer) totalChunksObj;
            } else if (totalChunksObj instanceof Double) {
                totalChunks = ((Double) totalChunksObj).intValue();
            } else {
                return DTOResponse.error("uploadFileForRegistration", "totalChunks debe ser un número");
            }
            
            if (fileName == null || fileMimeType == null || totalChunksObj == null) {
                return DTOResponse.error("uploadFileForRegistration", "Faltan datos requeridos en el payload");
            }
            
            // Calcular el tamaño total estimado (usando el mismo CHUNK_SIZE que el cliente: 512KB)
            long estimatedSize = totalChunks * 1024 * 512; // 512 KB por chunk, igual que el cliente
            
            // Crear un usuario temporal para la subida (usaremos el fileName como identificador temporal)
            String tempUserId = "temp_" + fileName.hashCode();
            
            // Crear el DTO que espera el servidor
            DTOIniciarSubida dto = new DTOIniciarSubida(
                tempUserId,
                fileName,
                fileMimeType,
                estimatedSize,
                totalChunks
            );
            
            // Usar el método de chunking service para registro
            return chunkingService.iniciarSubidaParaRegistro(dto);
            
        } catch (Exception e) {
            logger.error("Error en handleUploadFileForRegistration: {}", e.getMessage(), e);
            return DTOResponse.error("uploadFileForRegistration", "Error interno del servidor");
        }
    }

    /**
     * Maneja la acción uploadFileChunkForRegistration del cliente.
     * Permite subir chunks de archivos durante el proceso de registro sin autenticación.
     *
     * @param request DTORequest con los datos del chunk del cliente
     * @return DTOResponse con el resultado de la operación
     */
    private DTOResponse handleUploadFileChunkForRegistration(DTORequest request) {
        try {
            // Deserializar el payload del cliente
            @SuppressWarnings("unchecked")
            Map<String, Object> clientPayload = (Map<String, Object>) request.getPayload();
            
            String sessionId = (String) clientPayload.get("sessionId");
            String chunkData = (String) clientPayload.get("chunkData_base64");
            
            // Manejar tanto Integer como Double para numeroChunk
            Object numeroChunkObj = clientPayload.get("numeroChunk");
            int numeroChunk;
            if (numeroChunkObj instanceof Integer) {
                numeroChunk = (Integer) numeroChunkObj;
            } else if (numeroChunkObj instanceof Double) {
                numeroChunk = ((Double) numeroChunkObj).intValue();
            } else {
                return DTOResponse.error("uploadFileChunkForRegistration", "numeroChunk debe ser un número");
            }
            
            // Manejar tanto Integer como Double para totalChunks
            Object totalChunksObj = clientPayload.get("totalChunks");
            int totalChunks;
            if (totalChunksObj instanceof Integer) {
                totalChunks = (Integer) totalChunksObj;
            } else if (totalChunksObj instanceof Double) {
                totalChunks = ((Double) totalChunksObj).intValue();
            } else {
                return DTOResponse.error("uploadFileChunkForRegistration", "totalChunks debe ser un número");
            }
            
            if (sessionId == null || chunkData == null || numeroChunkObj == null || totalChunksObj == null) {
                return DTOResponse.error("uploadFileChunkForRegistration", "Faltan datos requeridos en el payload");
            }
            
            // Crear el DTO que espera el servidor
            DTOSubirArchivoChunk dto = new DTOSubirArchivoChunk(
                sessionId,
                null, // userId es null para registro
                null, // nombreArchivo es null para registro
                null, // tipoMime es null para registro
                0L,   // tamanoTotal es 0 para registro
                numeroChunk,
                totalChunks,
                chunkData,
                null  // hashChunk es null para registro (simplificado)
            );
            
            // Usar el método de chunking service para registro
            return chunkingService.subirChunkParaRegistro(dto);
            
        } catch (Exception e) {
            logger.error("Error en handleUploadFileChunkForRegistration: {}", e.getMessage(), e);
            return DTOResponse.error("uploadFileChunkForRegistration", "Error interno del servidor");
        }
    }

    /**
     * Maneja la acción endFileUploadForRegistration del cliente.
     * Permite finalizar la subida de archivos durante el proceso de registro sin autenticación.
     *
     * @param request DTORequest con los datos de finalización de subida del cliente
     * @return DTOResponse con el resultado de la operación
     */
    private DTOResponse handleEndFileUploadForRegistration(DTORequest request) {
        try {
            // Deserializar el payload del cliente
            @SuppressWarnings("unchecked")
            Map<String, Object> clientPayload = (Map<String, Object>) request.getPayload();
            
            String sessionId = (String) clientPayload.get("sessionId");
            
            if (sessionId == null) {
                return DTOResponse.error("endFileUploadForRegistration", "sessionId es requerido");
            }
            
            // Crear el DTO que espera el servidor
            DTOEndUpload dto = new DTOEndUpload(
                sessionId,
                null  // userId es null para registro
            );
            
            // Usar el método de chunking service para registro
            return chunkingService.finalizarSubidaParaRegistro(dto);
            
        } catch (Exception e) {
            logger.error("Error en handleEndFileUploadForRegistration: {}", e.getMessage(), e);
            return DTOResponse.error("endFileUploadForRegistration", "Error interno del servidor");
        }
    }
}

