package fachada.gestionContactos;

import observador.ISujeto;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para la Fachada que orquesta las operaciones de chat.
 */
public interface IFachadaChat extends ISujeto {
    void solicitarHistorial(String contactoId);
    CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido);
    CompletableFuture<Void> enviarMensajeAudio(String destinatarioId, File audioFile);
}
