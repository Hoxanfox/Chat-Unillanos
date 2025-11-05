package controlador.notificaciones;

import observador.ISujeto;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el controlador que gestiona las notificaciones del sistema.
 */
public interface IControladorNotificaciones extends ISujeto {

    /**
     * Solicita la lista actualizada de notificaciones al servidor.
     */
    void solicitarActualizacionNotificaciones();

    /**
     * Marca una notificación como leída.
     * @param notificacionId ID de la notificación.
     * @return CompletableFuture que se completa cuando se marca como leída.
     */
    CompletableFuture<Void> marcarComoLeida(String notificacionId);

    /**
     * Marca todas las notificaciones como leídas.
     * @return CompletableFuture que se completa cuando todas están marcadas.
     */
    CompletableFuture<Void> marcarTodasComoLeidas();

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