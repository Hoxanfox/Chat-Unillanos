package repositorio.mensaje;

import dominio.MensajeEnviadoCanal;
import dominio.MensajeRecibidoCanal;
import dto.canales.DTOMensajeCanal;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el repositorio que gestiona la persistencia de mensajes de canal.
 * Maneja tanto mensajes enviados como recibidos.
 */
public interface IRepositorioMensajeCanal {

    /**
     * Guarda un mensaje enviado a un canal en la base de datos local.
     *
     * @param mensaje El mensaje enviado a persistir.
     * @return CompletableFuture que se completa con true si se guardó exitosamente.
     */
    CompletableFuture<Boolean> guardarMensajeEnviado(MensajeEnviadoCanal mensaje);

    /**
     * Guarda un mensaje recibido de un canal en la base de datos local.
     *
     * @param mensaje El mensaje recibido a persistir.
     * @return CompletableFuture que se completa con true si se guardó exitosamente.
     */
    CompletableFuture<Boolean> guardarMensajeRecibido(MensajeRecibidoCanal mensaje);

    /**
     * Obtiene el historial de mensajes de un canal específico.
     * Combina mensajes enviados y recibidos, ordenados por fecha.
     *
     * @param canalId El ID del canal.
     * @param usuarioId El ID del usuario (para marcar mensajes propios).
     * @param limite Cantidad máxima de mensajes a obtener.
     * @return CompletableFuture con la lista de mensajes.
     */
    CompletableFuture<List<DTOMensajeCanal>> obtenerHistorialCanal(String canalId, String usuarioId, int limite);

    /**
     * Sincroniza el historial de mensajes de un canal con los datos del servidor.
     * 
     * @param canalId El ID del canal.
     * @param mensajes Lista de mensajes del servidor.
     * @return CompletableFuture que se completa cuando finaliza la sincronización.
     */
    CompletableFuture<Void> sincronizarHistorial(String canalId, List<DTOMensajeCanal> mensajes);

    /**
     * Elimina todos los mensajes de un canal específico.
     *
     * @param canalId El ID del canal.
     * @return CompletableFuture que se completa con true si se eliminaron exitosamente.
     */
    CompletableFuture<Boolean> eliminarMensajesDeCanal(String canalId);

    /**
     * Obtiene la cantidad de mensajes no leídos de un canal.
     *
     * @param canalId El ID del canal.
     * @param usuarioId El ID del usuario.
     * @return CompletableFuture con la cantidad de mensajes no leídos.
     */
    CompletableFuture<Integer> contarMensajesNoLeidos(String canalId, String usuarioId);
}

