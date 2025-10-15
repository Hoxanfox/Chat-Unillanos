package com.unillanos.server.service.impl;

import com.google.gson.Gson;
import com.unillanos.server.dto.*;
import com.unillanos.server.exception.ValidationException;
import com.unillanos.server.service.interfaces.IActionDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.HashMap;
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
                                ConnectionManager connectionManager) {
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
                case "solicitarHistorialPrivado" -> handleObtenerHistorial(request);
                case "marcar_mensaje_leido" -> handleMarcarMensajeLeido(request);
                
                // --- ACCIONES DE CONTACTOS ---
                case "solicitarListaContactos" -> handleSolicitarListaContactos(request);
                
                // --- ACCIONES DE ARCHIVOS ---
                case "subirArchivo" -> handleSubirArchivo(request);
                case "descargarArchivo" -> handleDescargarArchivo(request);
                case "listarArchivos" -> handleListarArchivos(request);
                
                // --- ACCIONES DE CHUNKING ---
                case "iniciar_subida" -> handleIniciarSubida(request);
                case "subir_chunk" -> handleSubirChunk(request);
                case "finalizar_subida" -> handleFinalizarSubida(request);
                case "descargar_chunk" -> handleDescargarChunk(request);
                
                // --- ACCIONES DE SUBIDA DE ARCHIVOS (Cliente) ---
                case "startFileUpload" -> handleStartFileUpload(request, ctx);
                case "endFileUpload" -> handleEndFileUpload(request);
                
                // --- ACCIONES DE SUBIDA DURANTE REGISTRO (Sin autenticación) ---
                case "uploadFileForRegistration" -> handleUploadFileForRegistration(request);
                case "uploadFileChunk" -> handleUploadFileChunk(request);
                
                // --- ACCIONES DE NOTIFICACIONES ---
                case "suscribir_notificaciones" -> handleSuscribirNotificaciones(request, ctx);
                case "desuscribir_notificaciones" -> handleDesuscribirNotificaciones(request);
                
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

    // --- HANDLERS DE CHUNKING ---

    private DTOResponse handleIniciarSubida(DTORequest request) {
        DTOIniciarSubida dto = gson.fromJson(gson.toJson(request.getPayload()), DTOIniciarSubida.class);
        return chunkingService.iniciarSubida(dto);
    }

    private DTOResponse handleSubirChunk(DTORequest request) {
        DTOSubirArchivoChunk dto = gson.fromJson(gson.toJson(request.getPayload()), DTOSubirArchivoChunk.class);
        return chunkingService.subirChunk(dto);
    }

    private DTOResponse handleFinalizarSubida(DTORequest request) {
        Map<String, Object> payload = gson.fromJson(gson.toJson(request.getPayload()), Map.class);
        String sessionId = (String) payload.get("sessionId");
        return chunkingService.finalizarSubida(sessionId);
    }

    private DTOResponse handleDescargarChunk(DTORequest request) {
        DTODescargarArchivoChunk dto = gson.fromJson(gson.toJson(request.getPayload()), DTODescargarArchivoChunk.class);
        return chunkingService.descargarChunk(dto);
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
     * Maneja la subida de archivos durante el proceso de registro.
     * Permite subir archivos sin autenticación para el registro de usuarios.
     *
     * @param request DTORequest con los datos de inicio de subida
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
            
            // Para archivos de registro, usamos un tamaño estimado más flexible
            // El tamaño real se validará cuando se reciban los chunks
            long estimatedSize = totalChunks * 1024 * 512; // 512 KB por chunk, igual que el cliente
            
            // Crear un userId temporal para la subida (usaremos solo el UUID para que quepa en la columna)
            String tempUserId = java.util.UUID.randomUUID().toString();
            
            // Crear el DTO que espera el servidor
            DTOIniciarSubida dto = new DTOIniciarSubida(
                tempUserId,
                fileName,
                fileMimeType,
                estimatedSize,
                totalChunks
            );
            
            // Llamar al método especial para registro
            DTOResponse response = chunkingService.iniciarSubidaParaRegistro(dto);
            
            // El ChunkingService ya devuelve el DTO correcto con uploadId incluido
            // Solo necesitamos cambiar la acción de la respuesta para que coincida con la petición del cliente
            if ("success".equals(response.getStatus())) {
                return DTOResponse.success("uploadFileForRegistration", response.getMessage(), response.getData());
            } else {
                return DTOResponse.error("uploadFileForRegistration", response.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error al procesar uploadFileForRegistration", e);
            return DTOResponse.error("uploadFileForRegistration", "Error interno del servidor: " + e.getMessage());
        }
    }

    /**
     * Maneja la subida de chunks durante el proceso de registro.
     * Adapta el payload del cliente al formato esperado por el servidor.
     *
     * @param request DTORequest con los datos del chunk
     * @return DTOResponse con el resultado de la operación
     */
    private DTOResponse handleUploadFileChunk(DTORequest request) {
        try {
            // Deserializar el payload del cliente
            @SuppressWarnings("unchecked")
            Map<String, Object> clientPayload = (Map<String, Object>) request.getPayload();
            
            String uploadId = (String) clientPayload.get("uploadId");
            Object chunkNumberObj = clientPayload.get("chunkNumber");
            String chunkDataBase64 = (String) clientPayload.get("chunkData_base64");
            
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
            // Necesitamos obtener los datos de la sesión para completar los campos requeridos
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
            dto.setUsuarioId("temp"); // Valor temporal para registro
            dto.setNombreArchivo(nombreArchivo);
            dto.setTipoMime(tipoMime);
            dto.setTamanoTotal(tamanoTotal);
            dto.setNumeroChunk(chunkNumber);
            dto.setTotalChunks(totalChunks);
            dto.setBase64ChunkData(chunkDataBase64);
            dto.setHashChunk(""); // Hash vacío para registro
            
            // Llamar al método específico para registro
            DTOResponse response = chunkingService.subirChunkParaRegistro(dto);
            
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
}

