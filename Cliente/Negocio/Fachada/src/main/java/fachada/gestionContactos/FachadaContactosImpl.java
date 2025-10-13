package fachada.gestionContactos;

import dto.featureContactos.DTOContacto;
import fachada.gestionContactos.IFachadaContactos;
import observador.IObservador;
import gestionContactos.actualizacion.GestionContactosImpl;
import gestionContactos.actualizacion.IGestionContactos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la Fachada de contactos. Orquesta el componente de gestión de contactos.
 */
public class FachadaContactosImpl implements IFachadaContactos, IObservador {

    private final List<IObservador> observadores = new ArrayList<>(); // Sus observadores (el Servicio)
    private final IGestionContactos gestionContactos;

    public FachadaContactosImpl() {
        // La fachada específica crea su propio componente de gestión.
        this.gestionContactos = new GestionContactosImpl();
        // La fachada se suscribe como observador del gestor para recibir notificaciones.
        this.gestionContactos.registrarObservador(this);
    }

    @Override
    public void solicitarActualizacionContactos() {
        gestionContactos.solicitarActualizacionContactos();
    }

    @Override
    public List<DTOContacto> getContactos() {
        return gestionContactos.getContactos();
    }

    /**
     * Este método es llamado por GestionContactos cuando hay datos nuevos.
     */
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        // La fachada simplemente pasa la notificación hacia arriba a sus propios observadores.
        notificarObservadores(tipoDeDato, datos);
    }

    // --- MÉTODOS AÑADIDOS PARA CUMPLIR CON LA INTERFAZ ---

    @Override
    public void solicitarHistorial(String contactoId) {
        System.out.println("➡️ [FachadaContactos]: Delegando solicitud de historial al gestor.");
        // gestionContactos.solicitarHistorial(contactoId); // Se delegará cuando el método exista en IGestionContactos
    }

    @Override
    public CompletableFuture<Void> enviarMensajeTexto(String destinatarioId, String contenido) {
        System.out.println("➡️ [FachadaContactos]: Delegando envío de mensaje de texto al gestor.");
        // return gestionContactos.enviarMensajeTexto(destinatarioId, contenido); // Se delegará cuando el método exista
        return CompletableFuture.completedFuture(null); // Retorno temporal
    }

    // --- Métodos del Patrón Sujeto ---
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

