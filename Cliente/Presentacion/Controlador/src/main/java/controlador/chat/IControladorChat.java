package controlador.chat;

import observador.IObservador;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el controlador que gestiona las interacciones
 * de una ventana de chat privado, incluyendo texto y audio.
 */
public interface IControladorChat {

    /**
     * Solicita el historial de mensajes para un contacto específico.
     * @param contactoId El ID del contacto.
     */
    void solicitarHistorial(String contactoId);

    /**
     * Envía un mensaje de texto a un destinatario.
     * @param destinatarioId El ID del destinatario.
     * @param contenido El texto del mensaje.
     * @return Una promesa que se completa cuando la petición de envío es reconocida.
     */
    CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido);

    /**
     * Inicia la grabación de un mensaje de audio.
     */
    void iniciarGrabacionAudio();

    /**
     * Detiene la grabación de audio actual y la envía al servidor.
     * @param destinatarioId El ID del destinatario del mensaje de audio.
     * @return Una promesa que se completa cuando la subida del audio es reconocida.
     */
    CompletableFuture<Void> detenerYEnviarGrabacion(String destinatarioId);

    /**
     * Cancela la grabación de audio actual sin enviar nada.
     */
    void cancelarGrabacion();

    /**
     * Permite que la vista se registre como observador de nuevos mensajes.
     * @param observador La vista que desea ser notificada.
     */
    void registrarObservador(IObservador observador);
}

