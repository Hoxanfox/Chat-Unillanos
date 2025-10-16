package gestionCanales.mensajes;

import dto.canales.DTOMensajeCanal;
import observador.ISujeto;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el gestor de mensajes de canal.
 * Implementa ISujeto para notificar a los observadores (UI) sobre nuevos mensajes.
 */
public interface IGestorMensajesCanal extends ISujeto {

    /**
     * Solicita el historial de mensajes de un canal al servidor.
     * 
     * @param canalId El ID del canal.
     * @param limite Cantidad máxima de mensajes a obtener.
     */
    void solicitarHistorialCanal(String canalId, int limite);

    /**
     * Envía un mensaje de texto a un canal.
     * 
     * @param canalId El ID del canal destino.
     * @param contenido El contenido del mensaje.
     * @return CompletableFuture que se completa cuando el mensaje es enviado.
     */
    CompletableFuture<Void> enviarMensajeTexto(String canalId, String contenido);

    /**
     * Envía un mensaje de audio a un canal.
     * 
     * @param canalId El ID del canal destino.
     * @param audioFileId El ID del archivo de audio previamente subido.
     * @return CompletableFuture que se completa cuando el mensaje es enviado.
     */
    CompletableFuture<Void> enviarMensajeAudio(String canalId, String audioFileId);

    /**
     * Envía un archivo a un canal.
     * 
     * @param canalId El ID del canal destino.
     * @param fileId El ID del archivo previamente subido.
     * @return CompletableFuture que se completa cuando el mensaje es enviado.
     */
    CompletableFuture<Void> enviarArchivo(String canalId, String fileId);

    /**
     * Inicializa los manejadores de respuestas del servidor.
     * Debe llamarse durante la inicialización del sistema.
     */
    void inicializarManejadores();
}

