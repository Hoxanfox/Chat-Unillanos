package fachada;

import dto.featureNotificaciones.DTONotificacion;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interfaz simplificada de la Fachada para uso en controladores.
 * Proporciona acceso directo a las operaciones de notificaciones.
 */
public interface IFachada {

    /**
     * Obtiene la lista de notificaciones del usuario actual.
     */
    CompletableFuture<List<DTONotificacion>> obtenerNotificaciones();

    /**
     * Marca una notificación como leída.
     */
    CompletableFuture<Void> marcarNotificacionLeida(String notificacionId);

    /**
     * Marca todas las notificaciones como leídas.
     */
    CompletableFuture<Void> marcarTodasNotificacionesLeidas();
}

