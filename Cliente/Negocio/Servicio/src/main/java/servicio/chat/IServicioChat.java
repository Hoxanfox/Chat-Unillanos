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
    CompletableFuture<Void> enviarMensajeAudio(String destinatarioId, String audioFileId);

    // Métodos para gestionar grabación de audio
    void iniciarGrabacionAudio() throws Exception;
    CompletableFuture<String> detenerYSubirGrabacion();
    void cancelarGrabacion();

    // Método para reproducir audio (descarga y reproduce desde disco - LEGACY)
    void reproducirAudio(String fileId);

    // Método para reproducir audio en memoria (SIN guardar en disco)
    CompletableFuture<Void> reproducirAudioEnMemoria(String fileId);

    void registrarObservador(IObservador observador);
    void removerObservador(IObservador observador);
}
