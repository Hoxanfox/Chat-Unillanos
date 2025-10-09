package controlador.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import servicio.autenticacion.IServicioAutenticacion;
import servicio.autenticacion.ServicioAutenticacion;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación del controlador de autenticación.
 */
public class ControladorAutenticacion implements IControladorAutenticacion {

    // El controlador solo depende de la capa de servicio, manteniendo la arquitectura limpia.
    private final IServicioAutenticacion servicioAutenticacion = new ServicioAutenticacion();

    @Override
    public CompletableFuture<Boolean> autenticar(DTOAutenticacion datos) {
        // Simplemente delega la llamada al servicio y devuelve el Future que este le proporciona.
        // La vista se encargará de reaccionar cuando este Future se complete.
        return servicioAutenticacion.autenticar(datos);
    }
}

