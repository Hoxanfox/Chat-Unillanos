package controlador.chat;

import observador.IObservador;
import servicio.chat.IServicioChat;
import servicio.chat.ServicioChatImpl;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación del controlador que gestiona las interacciones
 * de una ventana de chat privado.
 */
public class ControladorChat implements IControladorChat {

    private final IServicioChat servicioChat;

    public ControladorChat() {
        this.servicioChat = new ServicioChatImpl();
        System.out.println("✅ [ControladorChat]: Creado e instanciado el ServicioChat.");
    }

    @Override
    public void solicitarHistorial(String contactoId) {
        System.out.println("➡️ [ControladorChat]: Delegando solicitud de historial al Servicio para el contacto: " + contactoId);
        servicioChat.solicitarHistorial(contactoId);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido) {
        System.out.println("➡️ [ControladorChat]: Delegando envío de mensaje de texto al Servicio.");
        return servicioChat.enviarMensajeTexto(destinatarioId, contenido);
    }

    @Override
    public void iniciarGrabacionAudio() {
        System.out.println("➡️ [ControladorChat]: Delegando inicio de grabación de audio al Servicio.");
        // servicioChat.iniciarGrabacionAudio();
    }

    @Override
    public CompletableFuture<Void> detenerYEnviarGrabacion(String destinatarioId) {
        System.out.println("➡️ [ControladorChat]: Delegando detención y envío de grabación al Servicio.");
        // return servicioChat.detenerYEnviarGrabacion(destinatarioId);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void cancelarGrabacion() {
        System.out.println("➡️ [ControladorChat]: Delegando cancelación de grabación al Servicio.");
        // servicioChat.cancelarGrabacion();
    }

    @Override
    public void registrarObservador(IObservador observador) {
        System.out.println("➡️ [ControladorChat]: Delegando registro de observador (VistaChat) al Servicio.");
        servicioChat.registrarObservador(observador);
    }
}

