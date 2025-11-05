package com.unillanos.server.repository.interfaces;

import com.unillanos.server.entity.NotificacionEntity;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz para el repositorio de notificaciones.
 */
public interface INotificacionRepository {
    
    /**
     * Guarda una nueva notificación.
     */
    NotificacionEntity save(NotificacionEntity notificacion);
    
    /**
     * Busca una notificación por ID.
     */
    Optional<NotificacionEntity> findById(String id);
    
    /**
     * Obtiene todas las notificaciones de un usuario.
     */
    List<NotificacionEntity> findByUsuarioId(String usuarioId);
    
    /**
     * Obtiene notificaciones no leídas de un usuario.
     */
    List<NotificacionEntity> findNoLeidasByUsuarioId(String usuarioId);
    
    /**
     * Cuenta notificaciones no leídas de un usuario.
     */
    int countNoLeidasByUsuarioId(String usuarioId);
    
    /**
     * Marca una notificación como leída.
     */
    void marcarComoLeida(String notificacionId);
    
    /**
     * Marca todas las notificaciones de un usuario como leídas.
     */
    void marcarTodasComoLeidas(String usuarioId);
    
    /**
     * Elimina una notificación.
     */
    void delete(String notificacionId);
    
    /**
     * Elimina notificaciones antiguas (más de X días).
     */
    void deleteAntiguas(int dias);
}
