package servicio.contactos;

import dto.featureContactos.DTOContacto;
import fachada.FachadaGeneralImpl;

import observador.IObservador;
import fachada.gestionContactos.IFachadaContactos;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del servicio de contactos con logging detallado.
 */
public class ServicioContactosImpl implements IServicioContactos, IObservador {

    private final List<IObservador> observadores = new ArrayList<>();
    private final IFachadaContactos fachadaContactos;
    private List<DTOContacto> contactosCache = new ArrayList<>();

    public ServicioContactosImpl() {
        this.fachadaContactos = FachadaGeneralImpl.getInstancia().getFachadaContactos();
        System.out.println("✅ [ServicioContactos]: Creado. Registrándose como observador en la FachadaContactos.");
        this.fachadaContactos.registrarObservador(this);
    }

    @Override
    public List<DTOContacto> getContactos() {
        System.out.println("ℹ️ [ServicioContactos]: getContactos() llamado. Devolviendo " + contactosCache.size() + " contactos desde la caché.");
        return new ArrayList<>(contactosCache);
    }

    @Override
    public void solicitarActualizacionContactos() {
        System.out.println("➡️ [ServicioContactos]: Delegando solicitud de actualización de contactos a la Fachada.");
        fachadaContactos.solicitarActualizacionContactos();
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        System.out.println("🔔 [ServicioContactos]: ¡Notificación recibida de la Fachada! Tipo: " + tipoDeDato);
        if ("ACTUALIZAR_CONTACTOS".equals(tipoDeDato) && datos instanceof List) {
            this.contactosCache = (List<DTOContacto>) datos;
            System.out.println("  -> Caché de contactos actualizada con " + this.contactosCache.size() + " contactos.");
            notificarObservadores(tipoDeDato, this.contactosCache);
        } else {
            System.out.println("  -> La notificación no es para actualizar contactos o los datos son inválidos.");
        }
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("➕ [ServicioContactos]: Nuevo observador (Controlador/Vista) registrado. Total: " + observadores.size());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        if(observadores.remove(observador)) {
            System.out.println("➖ [ServicioContactos]: Observador (Controlador/Vista) removido. Total: " + observadores.size());
        }
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📣 [ServicioContactos]: Notificando a " + observadores.size() + " observador(es) (Controlador/Vista)...");
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}

