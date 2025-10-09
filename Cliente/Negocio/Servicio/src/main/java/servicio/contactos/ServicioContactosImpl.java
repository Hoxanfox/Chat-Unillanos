package servicio.contactos;

import dto.featureContactos.DTOContacto;
import observador.IObservador;
import fachada.gestionContactos.FachadaContactosImpl;
import fachada.gestionContactos.IFachadaContactos;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del servicio de contactos.
 * Ahora delega toda la lógica a la Fachada.
 */
public class ServicioContactosImpl implements IServicioContactos, IObservador {

    private final List<IObservador> observadores = new ArrayList<>();
    private final IFachadaContactos fachadaContactos;
    private List<DTOContacto> contactosCache = new ArrayList<>();

    public ServicioContactosImpl() {
        this.fachadaContactos = new FachadaContactosImpl();
        // El servicio se suscribe como observador de la fachada.
        this.fachadaContactos.registrarObservador(this);
    }

    @Override
    public List<DTOContacto> getContactos() {
        return new ArrayList<>(contactosCache);
    }

    @Override
    public void solicitarActualizacionContactos() {
        fachadaContactos.solicitarActualizacionContactos();
    }

    /**
     * Este método es llamado por la Fachada cuando hay datos nuevos.
     */
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        if ("ACTUALIZAR_CONTACTOS".equals(tipoDeDato) && datos instanceof List) {
            // El servicio actualiza su cache y notifica a sus observadores (la Vista).
            this.contactosCache = (List<DTOContacto>) datos;
            notificarObservadores(tipoDeDato, this.contactosCache);
        }
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

