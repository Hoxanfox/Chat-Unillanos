package servicio.negocio;

import fachada.FachadaGeneralImpl;

/**
 * Implementación del servicio de negocio.
 * Su única función es "despertar" a la Fachada General.
 */
public class ServicioNegocioImpl implements IServicioNegocio {

    @Override
    public void inicializar() {
        System.out.println("ServicioNegocio: Solicitando inicialización a la Fachada General...");
        // Al llamar a getInstancia(), se ejecuta el constructor del Singleton
        // y se inicializan todos los componentes de negocio.
        FachadaGeneralImpl.getInstancia();
    }
}
