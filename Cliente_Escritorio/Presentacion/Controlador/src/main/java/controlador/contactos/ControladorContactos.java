package controlador.contactos;

import observador.IObservador;
import servicio.contactos.IServicioContactos;
import servicio.contactos.ServicioContactosImpl;
import dto.featureContactos.DTOContacto;
import java.util.List;

/**
 * Implementación del controlador de contactos con logging detallado.
 * Actúa como un puente entre la Vista y el Servicio.
 */
public class ControladorContactos implements IControladorContactos {

    private final IServicioContactos servicioContactos;

    public ControladorContactos() {
        this.servicioContactos = new ServicioContactosImpl();
        System.out.println("✅ [ControladorContactos]: Creado e instanciado el ServicioContactos.");
    }

    @Override
    public void registrarObservador(IObservador observador) {
        System.out.println("➡️ [ControladorContactos]: Delegando registro de observador (Vista) al Servicio.");
        servicioContactos.registrarObservador(observador);
    }

    @Override
    public List<DTOContacto> getContactos() {
        System.out.println("➡️ [ControladorContactos]: Delegando solicitud de getContactos al Servicio.");
        return servicioContactos.getContactos();
    }

    /**
     * Implementación del método para solicitar la actualización de contactos.
     */
    @Override
    public void solicitarActualizacionContactos() {
        System.out.println("➡️ [ControladorContactos]: Delegando solicitud de actualización de contactos al Servicio.");
        servicioContactos.solicitarActualizacionContactos();
    }
}

