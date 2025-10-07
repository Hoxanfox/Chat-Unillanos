package controlador.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import servicio.autenticacion.IServicioAutenticacion;
import servicio.autenticacion.ServicioAutenticacionImpl;

/**
 * Implementación del controlador que gestiona la autenticación.
 * Delega la lógica de negocio al servicio correspondiente.
 */
public class ControladorAutenticacion implements IControladorAutenticacion {

    private final IServicioAutenticacion servicioAutenticacion;

    public ControladorAutenticacion() {
        this.servicioAutenticacion = new ServicioAutenticacionImpl();
    }

    @Override
    public boolean autenticar(DTOAutenticacion datos) {
        // Podríamos añadir validaciones de formato aquí antes de pasar al servicio.
        if (datos.getEmailUsuario() == null || datos.getEmailUsuario().trim().isEmpty() ||
                datos.getPasswordUsuario() == null || datos.getPasswordUsuario().isEmpty()) {
            return false;
        }
        return servicioAutenticacion.autenticar(datos);
    }
}
