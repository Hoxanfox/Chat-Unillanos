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
    private final Gson gson;

    public ActionDispatcherImpl(GlobalExceptionHandler exceptionHandler, 
                                LoggerService loggerService,
                                AutenticacionService autenticacionService,
                                CanalService canalService,
                                MensajeriaService mensajeriaService,
                                ArchivoService archivoService) {
        this.exceptionHandler = exceptionHandler;
        this.loggerService = loggerService;
        this.autenticacionService = autenticacionService;
        this.canalService = canalService;
        this.mensajeriaService = mensajeriaService;
        this.archivoService = archivoService;
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
                case "obtenerHistorial" -> handleObtenerHistorial(request);
                
                // --- ACCIONES DE ARCHIVOS ---
                case "subirArchivo" -> handleSubirArchivo(request);
                case "descargarArchivo" -> handleDescargarArchivo(request);
                case "listarArchivos" -> handleListarArchivos(request);
                
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
        DTORegistro dto = gson.fromJson(gson.toJson(request.getPayload()), DTORegistro.class);
        return autenticacionService.registrarUsuario(dto, ipAddress);
    }

    /**
     * Maneja la acción de login.
     */
    private DTOResponse handleLogin(DTORequest request, ChannelHandlerContext ctx, String ipAddress) {
        DTOLogin dto = gson.fromJson(gson.toJson(request.getPayload()), DTOLogin.class);
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
}

