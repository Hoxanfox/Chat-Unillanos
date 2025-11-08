package controlador.chat;

import dto.vistaContactoChat.DTOMensaje;
import observador.IObservador;
import java.io.File;
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
     * Descarga y reproduce un archivo de audio (guarda en disco temporal).
     * @param fileId El ID del archivo de audio en el servidor.
     */
    void reproducirAudio(String fileId);

    /**
     * Descarga y reproduce un archivo de audio EN MEMORIA (sin guardar en disco).
     * @param fileId El ID del archivo de audio en el servidor.
     * @return CompletableFuture que se completa cuando la reproducción inicia
     */
    CompletableFuture<Void> reproducirAudioEnMemoria(String fileId);

    /**
     * Descarga un audio a la carpeta local para tener caché.
     * @param fileId El ID del archivo en el servidor
     * @return CompletableFuture que se completa con el archivo descargado
     */
    CompletableFuture<File> descargarAudioALocal(String fileId);

    /**
     * ✅ NUEVO: Guarda un audio que viene en Base64 (desde PUSH del servidor) como archivo físico
     * y en la base de datos local para uso offline.
     *
     * @param base64Audio El contenido del audio en Base64
     * @param mensajeId El ID del mensaje (usado para generar nombre único)
     * @return CompletableFuture que se completa con el archivo guardado
     */
    CompletableFuture<File> guardarAudioDesdeBase64(String base64Audio, String mensajeId);

    /**
     * Permite que la vista se registre como observador de nuevos mensajes.
     * @param observador La vista que desea ser notificada.
     */
    void registrarObservador(IObservador observador);
}
