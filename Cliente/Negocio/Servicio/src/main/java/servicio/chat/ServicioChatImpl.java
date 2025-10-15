package servicio.chat;

import fachada.FachadaGeneralImpl;
import observador.IObservador;
import fachada.gestionContactos.contactos.IFachadaContactos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de chat que AHORA depende de la FachadaContactos.
 */
public class ServicioChatImpl implements IServicioChat, IObservador {

    private final List<IObservador> observadores = new ArrayList<>();
    // CORRECCIÓN: La dependencia ahora es con la fachada de contactos.
    private final IFachadaContactos fachadaContactos;

    public ServicioChatImpl() {
        // Obtiene la fachada correcta desde la Fachada General.
        this.fachadaContactos = FachadaGeneralImpl.getInstancia().getFachadaContactos();
        // Se suscribe a la fachada de contactos para recibir notificaciones (de nuevos mensajes, etc.)
        this.fachadaContactos.registrarObservador(this);
    }

    @Override
    public void solicitarHistorial(String contactoId) {
        fachadaContactos.solicitarHistorial(contactoId);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido) {
        return fachadaContactos.enviarMensajeTexto(destinatarioId, contenido);
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        // Pasa la notificación (ej. "NUEVO_MENSAJE_PRIVADO") hacia arriba a la vista.
        notificarObservadores(tipoDeDato, datos);
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) observadores.add(observador);
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
    }

    // Este método es necesario para notificar a la VISTA (su observador).
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador obs : observadores) {
            obs.actualizar(tipoDeDato, datos);
        }
    }
}

