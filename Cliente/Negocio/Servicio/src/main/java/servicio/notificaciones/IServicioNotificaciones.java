package servicio.notificaciones;

import observador.IObservador;
import java.util.concurrent.CompletableFuture;

/**
 * Interfaz del servicio de notificaciones.
 */
public interface IServicioNotificaciones {
    
    /**
     * Solicita la actualización de las notificaciones del usuario.
     */
    void solicitarActualizacionNotificaciones();
    
    /**
     * Marca una notificación específica como leída.
     */
    CompletableFuture<Void> marcarComoLeida(String notificacionId);
    
    /**
     * Marca todas las notificaciones como leídas.
     */
    CompletableFuture<Void> marcarTodasComoLeidas();
    
    /**
<<<<<<< HEAD
     * Acepta una invitación a un canal.
     */
    CompletableFuture<Void> aceptarInvitacionCanal(String invitacionId, String canalId);

    /**
     * Rechaza una invitación a un canal.
     */
    CompletableFuture<Void> rechazarInvitacionCanal(String invitacionId);

    /**
=======
>>>>>>> refs/remotes/origin/develop
     * Registra un observador para recibir actualizaciones.
     */
    void registrarObservador(IObservador observador);
    
    /**
     * Remueve un observador.
     */
    void removerObservador(IObservador observador);
}
<<<<<<< HEAD
=======

>>>>>>> refs/remotes/origin/develop
