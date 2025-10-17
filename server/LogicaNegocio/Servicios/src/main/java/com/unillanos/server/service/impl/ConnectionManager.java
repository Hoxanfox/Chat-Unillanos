package com.unillanos.server.service.impl;

import com.google.gson.Gson;
import com.unillanos.server.dto.DTOResponse;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gestor de conexiones activas de clientes.
 * Mantiene un mapa thread-safe de usuarios conectados y proporciona
 * métodos para enviar notificaciones en tiempo real.
 * Usa hilos virtuales de Java 21 para envío paralelo de notificaciones.
 */
@Component
public class ConnectionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    
    // Mapa thread-safe de conexiones: userId -> ChannelHandlerContext
    private final Map<String, ChannelHandlerContext> activeConnections;
    
    // Executor con hilos virtuales para envío asíncrono de notificaciones
    private final ExecutorService virtualThreadExecutor;
    
    // Gson para serializar DTOResponse a JSON
    private final Gson gson;
    
    // NotificationManager para notificar cambios de conexión
    private final NotificationManager notificationManager;

    public ConnectionManager(NotificationManager notificationManager) {
        this.activeConnections = new ConcurrentHashMap<>();
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.gson = new Gson();
        this.notificationManager = notificationManager;
    }

    /**
     * Registra una nueva conexión de usuario.
     *
     * @param userId ID del usuario
     * @param ctx Contexto del canal de Netty
     */
    public void registerConnection(String userId, ChannelHandlerContext ctx) {
        activeConnections.put(userId, ctx);
        logger.info("Conexión registrada - userId: {}, total conexiones: {}", 
                    userId, activeConnections.size());
        
        // Notificar a clientes GUI suscritos
        notificationManager.notificar("CONEXION", Map.of(
            "usuarioId", userId,
            "totalConexiones", activeConnections.size(),
            "accion", "CONECTADO"
        ));
    }

    /**
     * Elimina la conexión de un usuario.
     *
     * @param userId ID del usuario
     */
    public void removeConnection(String userId) {
        ChannelHandlerContext removed = activeConnections.remove(userId);
        if (removed != null) {
            logger.info("Conexión eliminada - userId: {}, total conexiones: {}", 
                       userId, activeConnections.size());
            
            // Notificar a clientes GUI suscritos
            notificationManager.notificar("DESCONEXION", Map.of(
                "usuarioId", userId,
                "totalConexiones", activeConnections.size(),
                "accion", "DESCONECTADO"
            ));
        }
    }

    /**
     * Obtiene el ID del usuario asociado a un contexto de canal sin remover la conexión.
     *
     * @param ctx Contexto del canal
     * @return ID del usuario (null si no se encontró)
     */
    public String getUserIdByContext(ChannelHandlerContext ctx) {
        for (Map.Entry<String, ChannelHandlerContext> entry : activeConnections.entrySet()) {
            if (entry.getValue() == ctx) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Elimina una conexión dado su contexto de canal.
     * Útil cuando se detecta una desconexión desde el handler de Netty.
     *
     * @param ctx Contexto del canal
     * @return ID del usuario desconectado (null si no se encontró)
     */
    public String removeConnectionByContext(ChannelHandlerContext ctx) {
        for (Map.Entry<String, ChannelHandlerContext> entry : activeConnections.entrySet()) {
            if (entry.getValue() == ctx) {
                String userId = entry.getKey();
                activeConnections.remove(userId);
                logger.info("Conexión eliminada por contexto - userId: {}, total conexiones: {}", 
                           userId, activeConnections.size());
                return userId;
            }
        }
        return null;
    }

    /**
     * Verifica si un usuario está actualmente conectado.
     *
     * @param userId ID del usuario
     * @return true si el usuario está online
     */
    public boolean isUserOnline(String userId) {
        return activeConnections.containsKey(userId);
    }

    /**
     * Envía una notificación a un usuario específico si está conectado.
     * El envío es asíncrono usando hilos virtuales.
     *
     * @param userId ID del usuario destinatario
     * @param notification DTO de respuesta a enviar
     */
    public void notifyUser(String userId, DTOResponse notification) {
        ChannelHandlerContext ctx = activeConnections.get(userId);
        if (ctx != null && ctx.channel().isActive()) {
            virtualThreadExecutor.submit(() -> {
                try {
                    String json = gson.toJson(notification);
                    ctx.writeAndFlush(json + "\n");
                    logger.debug("Notificación enviada a userId: {}, action: {}", 
                                userId, notification.getAction());
                } catch (Exception e) {
                    logger.error("Error al enviar notificación a userId: {}", userId, e);
                }
            });
        } else {
            logger.debug("Usuario no conectado, notificación no enviada: userId={}", userId);
        }
    }

    /**
     * Envía una notificación a todos los miembros de un canal.
     * Solo notifica a los usuarios que están actualmente conectados.
     * El envío es paralelo usando hilos virtuales.
     *
     * @param channelId ID del canal (para logging)
     * @param notification DTO de respuesta a enviar
     * @param memberIds Set de IDs de usuarios miembros del canal
     */
    public void notifyChannel(String channelId, DTOResponse notification, Set<String> memberIds) {
        logger.debug("Enviando notificación a canal: {}, miembros: {}", channelId, memberIds.size());
        
        for (String memberId : memberIds) {
            notifyUser(memberId, notification);
        }
    }

    /**
     * Envía una notificación broadcast a TODOS los usuarios conectados.
     * Útil para anuncios del sistema.
     * El envío es paralelo usando hilos virtuales.
     *
     * @param notification DTO de respuesta a enviar
     */
    public void broadcast(DTOResponse notification) {
        logger.info("Broadcast a {} usuarios: {}", activeConnections.size(), notification.getAction());
        
        for (Map.Entry<String, ChannelHandlerContext> entry : activeConnections.entrySet()) {
            String userId = entry.getKey();
            ChannelHandlerContext ctx = entry.getValue();
            
            if (ctx.channel().isActive()) {
                virtualThreadExecutor.submit(() -> {
                    try {
                        String json = gson.toJson(notification);
                        ctx.writeAndFlush(json + "\n");
                    } catch (Exception e) {
                        logger.error("Error al enviar broadcast a userId: {}", userId, e);
                    }
                });
            }
        }
    }

    /**
     * Envía una notificación a TODOS los usuarios conectados EXCEPTO uno.
     * Útil para notificar cambios de estado de un usuario a los demás.
     * El envío es paralelo usando hilos virtuales.
     *
     * @param excludeUserId ID del usuario a excluir
     * @param notification DTO de respuesta a enviar
     */
    public void notifyAllExcept(String excludeUserId, DTOResponse notification) {
        logger.debug("Notificando a todos excepto userId: {}, total destinatarios: {}",
                    excludeUserId, activeConnections.size() - 1);

        for (Map.Entry<String, ChannelHandlerContext> entry : activeConnections.entrySet()) {
            String userId = entry.getKey();

            // Saltar al usuario excluido
            if (userId.equals(excludeUserId)) {
                continue;
            }

            ChannelHandlerContext ctx = entry.getValue();

            if (ctx.channel().isActive()) {
                virtualThreadExecutor.submit(() -> {
                    try {
                        String json = gson.toJson(notification);
                        ctx.writeAndFlush(json + "\n");
                        logger.trace("Notificación enviada a userId: {} (excluido: {})", userId, excludeUserId);
                    } catch (Exception e) {
                        logger.error("Error al enviar notificación a userId: {}", userId, e);
                    }
                });
            }
        }
    }

    /**
     * Obtiene todas las conexiones activas.
     * Útil para la GUI de administración.
     *
     * @return Mapa inmutable de conexiones activas
     */
    public Map<String, ChannelHandlerContext> getAllConnections() {
        return Map.copyOf(activeConnections);
    }

    /**
     * Obtiene el número de usuarios conectados.
     *
     * @return Número de conexiones activas
     */
    public int getActiveConnectionsCount() {
        return activeConnections.size();
    }
}

