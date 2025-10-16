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
<<<<<<< HEAD

    /**
     * Acepta una invitación a un canal.
     * @param invitacionId ID de la invitación
     * @param canalId ID del canal
     * @return CompletableFuture que se completa cuando se acepta la invitación
     */
    CompletableFuture<Void> aceptarInvitacionCanal(String invitacionId, String canalId);

    /**
     * Rechaza una invitación a un canal.
     * @param invitacionId ID de la invitación
     * @return CompletableFuture que se completa cuando se rechaza la invitación
     */
    CompletableFuture<Void> rechazarInvitacionCanal(String invitacionId);
}
=======
}

>>>>>>> refs/remotes/origin/develop
