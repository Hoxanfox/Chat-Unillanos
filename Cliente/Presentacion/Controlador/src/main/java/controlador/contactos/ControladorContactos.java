package controlador.contactos;

import observador.IObservador;
import servicio.contactos.IServicioContactos;
import servicio.contactos.ServicioContactosImpl;
import dto.featureContactos.DTOContacto;
import java.util.List;

/**
 * Implementación del controlador de contactos.
 * Actúa como un puente entre la Vista y el Servicio.
 */
public class ControladorContactos implements IControladorContactos {

    // El controlador depende del servicio, no la vista.
    private final IServicioContactos servicioContactos;

    public ControladorContactos() {
        // En una aplicación real, esto se inyectaría.
        this.servicioContactos = new ServicioContactosImpl();
    }

    @Override
    public void registrarObservador(IObservador observador) {
        servicioContactos.registrarObservador(observador);
    }

    @Override
    public List<DTOContacto> getContactos() {
        return servicioContactos.getContactos();
    }
}
