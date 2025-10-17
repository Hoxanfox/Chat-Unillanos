package com.unillanos.server.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unillanos.server.dto.*;
import com.unillanos.server.dto.response.*;
import com.unillanos.server.exception.ValidationException;
import com.unillanos.server.service.interfaces.IActionDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final NotificationManager notificationManager;
    private final AdminUsersService adminUsersService;
    private final ConnectionManager connectionManager;
    private final NotificationService notificationService;
    private final ContactService contactService;
    private final Gson gson;
    private final FileTransferService fileTransferService;

    public ActionDispatcherImpl(GlobalExceptionHandler exceptionHandler, 
                                LoggerService loggerService,
                                AutenticacionService autenticacionService,
                                CanalService canalService,
                                MensajeriaService mensajeriaService,
                                ArchivoService archivoService,
                                NotificationManager notificationManager,
                                AdminUsersService adminUsersService,
                                ConnectionManager connectionManager,
                                NotificationService notificationService,
                                ContactService contactService,
                                FileTransferService fileTransferService) {
        this.exceptionHandler = exceptionHandler;
        this.loggerService = loggerService;
        this.autenticacionService = autenticacionService;
        this.canalService = canalService;
        this.mensajeriaService = mensajeriaService;
        this.archivoService = archivoService;
        this.notificationManager = notificationManager;
        this.adminUsersService = adminUsersService;
        this.connectionManager = connectionManager;
        this.notificationService = notificationService;
        this.contactService = contactService;
        this.fileTransferService = fileTransferService;
        // Configurar Gson con adaptador para LocalDateTime
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .create();
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
                
                // --- ACCIONES DE TRANSFERENCIA DE ARCHIVOS POR CHUNKS ---
                case "startFileUpload" -> fileTransferService.startFileUpload(request, ctx);
                case "uploadFileChunk" -> fileTransferService.uploadFileChunk(request, ctx);
                case "endFileUpload" -> fileTransferService.endFileUpload(request, ctx);
                case "startFileDownload" -> fileTransferService.startFileDownload(request, ctx);
                case "requestFileChunk" -> fileTransferService.requestFileChunk(request, ctx);
                case "uploadFileForRegistration" -> fileTransferService.uploadFileForRegistration(request, ctx);
                case "getFileTransferStats" -> fileTransferService.getFileTransferStats(request, ctx);

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
        // El userId viene en el payload como "usuarioId"
        @SuppressWarnings("unchecked")
        Map<String, String> payload = (Map<String, String>) request.getPayload();
        String userId = payload.get("usuarioId"); // Cambiado de "userId" a "usuarioId"
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
}
