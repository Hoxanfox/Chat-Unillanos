package fachada.gestionContactos.contactos;

import dto.featureContactos.DTOContacto;
import observador.IObservador;
import gestionContactos.actualizacion.GestionContactosImpl;
import gestionContactos.actualizacion.IGestionContactos;
import gestionContactos.mensajes.GestionMensajesImpl;
import gestionContactos.mensajes.IGestionMensajes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la Fachada de contactos. Orquesta el componente de gestión de contactos.
 */
public class FachadaContactosImpl implements IFachadaContactos, IObservador {

    private final List<IObservador> observadores = new ArrayList<>(); // Sus observadores (el Servicio)
    private final IGestionContactos gestionContactos;
    private final IGestionMensajes gestionMensajes;

    public FachadaContactosImpl() {
        System.out.println("🔧 [FachadaContactos]: Inicializando fachada de contactos...");

        // La fachada específica crea sus propios componentes de gestión.
        this.gestionContactos = new GestionContactosImpl();
        this.gestionMensajes = new GestionMensajesImpl();

        // La fachada se suscribe como observador de ambos gestores para recibir notificaciones.
        this.gestionContactos.registrarObservador(this);
        this.gestionMensajes.registrarObservador(this);

        System.out.println("✅ [FachadaContactos]: Fachada inicializada con gestores de contactos y mensajes");
    }

    @Override
    public void solicitarActualizacionContactos() {
        System.out.println("➡️ [FachadaContactos]: Solicitando actualización de contactos al gestor");
        gestionContactos.solicitarActualizacionContactos();
    }

    @Override
    public List<DTOContacto> getContactos() {
        List<DTOContacto> contactos = gestionContactos.getContactos();
        System.out.println("📋 [FachadaContactos]: Obteniendo lista de contactos - Total: " + contactos.size());
        return contactos;
    }

    /**
     * Este método es llamado por GestionContactos cuando hay datos nuevos.
     */
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("📢 [FachadaContactos]: Recibida notificación - Tipo: " + tipoDeDato);
        // La fachada simplemente pasa la notificación hacia arriba a sus propios observadores.
        notificarObservadores(tipoDeDato, datos);
    }

    // --- MÉTODOS DE CHAT ---

    @Override
    public void solicitarHistorial(String contactoId) {
        System.out.println("➡️ [FachadaContactos]: Delegando solicitud de historial al gestor de mensajes - ContactoId: " + contactoId);
        gestionMensajes.solicitarHistorial(contactoId);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido) {
        System.out.println("➡️ [FachadaContactos]: Delegando envío de mensaje de texto al gestor - DestinatarioId: " + destinatarioId);
        return gestionMensajes.enviarMensajeTexto(destinatarioId, contenido);
    }

    @Override
    public CompletableFuture<Void> enviarMensajeAudio(String destinatarioId, String audioFileId) {
        System.out.println("➡️ [FachadaContactos]: Delegando envío de mensaje de audio al gestor - DestinatarioId: " + destinatarioId);
        return gestionMensajes.enviarMensajeAudio(destinatarioId, audioFileId);
    }

    // --- Métodos del Patrón Sujeto ---
    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("🔔 [FachadaContactos]: Observador registrado - Total: " + observadores.size());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("🔕 [FachadaContactos]: Observador removido - Total: " + observadores.size());
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📣 [FachadaContactos]: Notificando a " + observadores.size() + " observadores - Tipo: " + tipoDeDato);
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}
