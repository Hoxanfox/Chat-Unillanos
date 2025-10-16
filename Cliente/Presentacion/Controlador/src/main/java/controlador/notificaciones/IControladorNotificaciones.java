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
}

