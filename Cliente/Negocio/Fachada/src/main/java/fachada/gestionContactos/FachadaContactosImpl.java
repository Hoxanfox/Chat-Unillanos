package fachada.gestionContactos;

import observador.IObservador;
import gestionContactos.actualizacion.GestionContactosImpl;
import gestionContactos.actualizacion.IGestionContactos;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de la Fachada de contactos. Orquesta los componentes de gestión.
 */
public class FachadaContactosImpl implements IFachadaContactos, IObservador {

    private final List<IObservador> observadores = new ArrayList<>(); // Sus observadores (el Servicio)
    private final IGestionContactos gestionContactos;

    public FachadaContactosImpl() {
        this.gestionContactos = new GestionContactosImpl();
        // La fachada se suscribe como observador del gestor.
        this.gestionContactos.registrarObservador(this);
    }

    @Override
    public void solicitarActualizacionContactos() {
        gestionContactos.solicitarActualizacionContactos();
    }

    /**
     * Este método es llamado por GestionContactos cuando hay datos nuevos.
     */
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        // La fachada simplemente pasa la notificación hacia arriba a sus propios observadores.
        notificarObservadores(tipoDeDato, datos);
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
