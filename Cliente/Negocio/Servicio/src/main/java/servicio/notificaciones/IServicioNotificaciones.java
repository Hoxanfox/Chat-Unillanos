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
     * Registra un observador para recibir actualizaciones.
     */
    void registrarObservador(IObservador observador);
    
    /**
     * Remueve un observador.
     */
    void removerObservador(IObservador observador);
}

