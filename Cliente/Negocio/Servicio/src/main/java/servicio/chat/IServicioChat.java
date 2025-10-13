package servicio.chat;

import observador.IObservador;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el servicio que gestiona la lógica de negocio del chat.
 * AHORA delega sus responsabilidades a través de la Fachada de Contactos.
 */
public interface IServicioChat {
    void solicitarHistorial(String contactoId);
    CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido);
    void registrarObservador(IObservador observador);
    void removerObservador(IObservador observador);
}

