package fachada;

import dto.featureNotificaciones.DTONotificacion;
import observador.IObservador;

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
     * @param canalId ID del canal
     * @return CompletableFuture que se completa cuando se rechaza la invitación
     */
    CompletableFuture<Void> rechazarInvitacionCanal(String invitacionId, String canalId);

    /**
     * Obtiene las notificaciones desde el caché local.
     * @return Lista de notificaciones en caché
     */
    List<DTONotificacion> obtenerNotificacionesCache();

    /**
     * Registra un observador para notificaciones en tiempo real.
     * @param observador El observador a registrar
     */
    void registrarObservadorNotificaciones(IObservador observador);

    /**
     * Remueve un observador de notificaciones.
     * @param observador El observador a remover
     */
    void removerObservadorNotificaciones(IObservador observador);
}