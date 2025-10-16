package fachada.gestionNotificaciones;

import dto.featureNotificaciones.DTONotificacion;

import java.util.concurrent.CompletableFuture;
import java.util.List;

/**
 * Contrato para la fachada que gestiona las notificaciones del sistema.
 */
public interface IFachadaNotificaciones {

    /**
     * Obtiene la lista de notificaciones del usuario actual.
     * @return CompletableFuture con la lista de notificaciones.
     */
    CompletableFuture<List<DTONotificacion>> obtenerNotificaciones();

    /**
     * Marca una notificación específica como leída.
     * @param notificacionId ID de la notificación.
     * @return CompletableFuture que se completa cuando se marca como leída.
     */
    CompletableFuture<Void> marcarNotificacionLeida(String notificacionId);

    /**
     * Marca todas las notificaciones como leídas.
     * @return CompletableFuture que se completa cuando todas están marcadas.
     */
    CompletableFuture<Void> marcarTodasNotificacionesLeidas();
}

