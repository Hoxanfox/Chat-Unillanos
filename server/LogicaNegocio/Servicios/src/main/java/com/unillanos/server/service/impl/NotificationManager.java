package com.unillanos.server.service.impl;

import com.unillanos.server.dto.DTONotificacion;
import com.unillanos.server.dto.DTOResponse;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de notificaciones push para el patrón Observer.
 * Permite que clientes GUI se suscriban a eventos del servidor y reciban notificaciones en tiempo real.
 */
@Service
public class NotificationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationManager.class);
    
    // Mapa de clientes suscritos: clienteId -> ChannelHandlerContext
    private final Map<String, ChannelHandlerContext> observadores = new ConcurrentHashMap<>();
    
    // Mapa de suscripciones: clienteId -> Set de tipos de interés
    private final Map<String, Set<String>> suscripciones = new ConcurrentHashMap<>();
    
    /**
     * Suscribe un cliente a notificaciones push.
     *
     * @param clienteId ID único del cliente
     * @param ctx Contexto del canal Netty
     * @param tipos Lista de tipos de notificaciones de interés
     */
    public void suscribir(String clienteId, ChannelHandlerContext ctx, List<String> tipos) {
        observadores.put(clienteId, ctx);
        suscripciones.put(clienteId, new HashSet<>(tipos));
        
        logger.info("Cliente {} suscrito a notificaciones: {}", clienteId, tipos);
        
        // Enviar confirmación de suscripción
        DTONotificacion confirmacion = new DTONotificacion(
            "SUSCRIPCION_CONFIRMADA",
            LocalDateTime.now(),
            Map.of("clienteId", clienteId, "tipos", tipos)
        );
        
        DTOResponse response = DTOResponse.success(
            "suscribir_notificaciones",
            "Suscripción exitosa",
            confirmacion
        );
        
        ctx.writeAndFlush(response);
    }
    
    /**
     * Desuscribe un cliente de las notificaciones.
     *
     * @param clienteId ID del cliente a desuscribir
     */
    public void desuscribir(String clienteId) {
        ChannelHandlerContext ctx = observadores.remove(clienteId);
        suscripciones.remove(clienteId);
        
        if (ctx != null) {
            logger.info("Cliente {} desuscrito de notificaciones", clienteId);
        }
    }
    
    /**
     * Desuscribe un cliente por su contexto de canal (útil cuando se cierra la conexión).
     *
     * @param ctx Contexto del canal que se cerró
     */
    public void desuscribirPorCanal(ChannelHandlerContext ctx) {
        String clienteId = null;
        
        // Buscar el cliente por su contexto
        for (Map.Entry<String, ChannelHandlerContext> entry : observadores.entrySet()) {
            if (entry.getValue().equals(ctx)) {
                clienteId = entry.getKey();
                break;
            }
        }
        
        if (clienteId != null) {
            desuscribir(clienteId);
        }
    }
    
    /**
     * Envía una notificación a todos los clientes suscritos al tipo especificado.
     *
     * @param tipo Tipo de notificación
     * @param datos Datos de la notificación
     */
    public void notificar(String tipo, Map<String, Object> datos) {
        DTONotificacion notificacion = new DTONotificacion(tipo, LocalDateTime.now(), datos);
        DTOResponse response = DTOResponse.success(
            "notificacion_push",
            "Notificación del servidor",
            notificacion
        );
        
        List<String> clientesAEliminar = new ArrayList<>();
        
        for (Map.Entry<String, ChannelHandlerContext> entry : observadores.entrySet()) {
            String clienteId = entry.getKey();
            ChannelHandlerContext ctx = entry.getValue();
            Set<String> tiposInteres = suscripciones.get(clienteId);
            
            // Verificar si el cliente está interesado en este tipo de notificación
            if (tiposInteres != null && tiposInteres.contains(tipo)) {
                try {
                    // Verificar si el canal sigue activo
                    if (ctx.channel().isActive()) {
                        ctx.writeAndFlush(response);
                        logger.debug("Notificación {} enviada a cliente {}", tipo, clienteId);
                    } else {
                        clientesAEliminar.add(clienteId);
                    }
                } catch (Exception e) {
                    logger.warn("Error al enviar notificación a cliente {}: {}", clienteId, e.getMessage());
                    clientesAEliminar.add(clienteId);
                }
            }
        }
        
        // Limpiar clientes inactivos
        for (String clienteId : clientesAEliminar) {
            desuscribir(clienteId);
        }
        
        logger.debug("Notificación {} enviada a {} clientes", tipo, observadores.size());
    }
    
    /**
     * Envía una notificación a un cliente específico.
     *
     * @param clienteId ID del cliente
     * @param tipo Tipo de notificación
     * @param datos Datos de la notificación
     */
    public void notificarCliente(String clienteId, String tipo, Map<String, Object> datos) {
        ChannelHandlerContext ctx = observadores.get(clienteId);
        if (ctx != null && ctx.channel().isActive()) {
            DTONotificacion notificacion = new DTONotificacion(tipo, LocalDateTime.now(), datos);
            DTOResponse response = DTOResponse.success(
                "notificacion_push",
                "Notificación del servidor",
                notificacion
            );
            
            try {
                ctx.writeAndFlush(response);
                logger.debug("Notificación {} enviada a cliente específico {}", tipo, clienteId);
            } catch (Exception e) {
                logger.warn("Error al enviar notificación a cliente específico {}: {}", clienteId, e.getMessage());
                desuscribir(clienteId);
            }
        }
    }
    
    /**
     * Retorna el número de clientes suscritos.
     *
     * @return Número de clientes activos
     */
    public int getNumeroClientesSuscritos() {
        return observadores.size();
    }
    
    /**
     * Retorna información sobre los clientes suscritos.
     *
     * @return Mapa con información de clientes
     */
    public Map<String, Object> getInfoClientes() {
        Map<String, Object> info = new HashMap<>();
        info.put("totalClientes", observadores.size());
        info.put("clientesActivos", observadores.keySet());
        info.put("suscripciones", new HashMap<>(suscripciones));
        return info;
    }
}
