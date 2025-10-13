package gestionContactos.mensajes;

import observador.ISujeto;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el especialista en la lógica de mensajes.
 */
public interface IGestionMensajes extends ISujeto {
    void solicitarHistorial(String contactoId);
    CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido);

    /**
     * Envía la petición de un mensaje de audio, usando el ID del archivo ya subido.
     * @param destinatarioId El ID del contacto que recibirá el audio.
     * @param audioFileId El identificador del archivo de audio en el servidor.
     * @return Una promesa que se completa cuando la petición es enviada.
     */
    CompletableFuture<Void> enviarMensajeAudio(String destinatarioId, String audioFileId);
}

