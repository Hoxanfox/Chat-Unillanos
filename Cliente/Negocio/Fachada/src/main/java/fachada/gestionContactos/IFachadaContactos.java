package fachada.gestionContactos;

import dto.featureContactos.DTOContacto;
import observador.ISujeto;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para la Fachada que AHORA gestiona tanto la lista de contactos
 * como los mensajes de chat.
 */
public interface IFachadaContactos extends ISujeto {
    // Métodos para la lista de contactos
    void solicitarActualizacionContactos();
    List<DTOContacto> getContactos();

    // Métodos para el chat
    void solicitarHistorial(String contactoId);
    CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido);
}

