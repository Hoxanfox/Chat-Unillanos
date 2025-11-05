package fachada.gestionContactos.chat;

import gestionArchivos.GestionArchivosImpl;
import gestionArchivos.IGestionArchivos;
import gestionContactos.mensajes.GestionMensajesImpl;
import gestionContactos.mensajes.IGestionMensajes;
import observador.IObservador;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la Fachada que orquesta las operaciones de chat.
 * AHORA vuelve a ser responsable de crear sus propias dependencias.
 */
public class FachadaChatImpl implements IFachadaChat, IObservador {

    private final List<IObservador> observadores = new ArrayList<>();
    private final IGestionMensajes gestionMensajes;
    private final IGestionArchivos gestionArchivos;

    public FachadaChatImpl() {
        // La fachada específica crea los especialistas que necesita.
        this.gestionMensajes = new GestionMensajesImpl();
        this.gestionArchivos = new GestionArchivosImpl();
        // Se suscribe a las notificaciones del gestor de mensajes.
        this.gestionMensajes.registrarObservador(this);
    }

    @Override
    public void solicitarHistorial(String contactoId) {
        gestionMensajes.solicitarHistorial(contactoId);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido) {
        return gestionMensajes.enviarMensajeTexto(destinatarioId, contenido);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeAudio(String destinatarioId, File audioFile) {
        // ORQUESTACIÓN:
        return gestionArchivos.subirArchivo(audioFile)
                .thenCompose(audioFileId -> {
                    return gestionMensajes.enviarMensajeAudio(destinatarioId, audioFileId);
                });
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
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

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}
