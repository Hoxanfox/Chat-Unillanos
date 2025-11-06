// java
// File: `Negocio/GestionContactos/src/main/java/gestionContactos/mensajes/IGestionMensajes.java`
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
     * Envía un mensaje de audio referenciando un archivo ya subido (fileId).
     * @param destinatarioId El ID del contacto que recibirá el audio.
     * @param audioFileId Identificador del archivo de audio en el servidor/almacenamiento.
     * @return Una promesa que se completa cuando la petición es enviada.
     */
    CompletableFuture<Void> enviarMensajeAudio(String destinatarioId, String audioFileId);
}
