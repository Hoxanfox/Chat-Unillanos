package gestionContactos.mensajes;

import observador.ISujeto;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el especialista en la lógica de mensajes.
 */
public interface IGestionMensajes extends ISujeto {
    void solicitarHistorial(String contactoId);
    CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido);

    /**
     * Envía un mensaje de audio con contenido en Base64.
     * @param destinatarioId El ID del contacto que recibirá el audio.
     * @param audioBase64 El contenido del audio codificado en Base64.
     * @return Una promesa que se completa cuando la petición es enviada.
     */
    CompletableFuture<Void> enviarMensajeAudio(String destinatarioId, String audioBase64);
}
