package servicio.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import gestionUsuario.autenticacion.AutenticarUsuario;
import gestionUsuario.autenticacion.IAutenticarUsuario;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de autenticación.
 * Este componente es el punto de entrada a la capa de negocio y delega
 * las tareas a los componentes de gestión específicos.
 */
public class ServicioAutenticacion implements IServicioAutenticacion {

    // El servicio depende del componente de gestión de usuarios.
    // En una aplicación real, esto se inyectaría con un framework de dependencias.
    private final IAutenticarUsuario gestionUsuario = new AutenticarUsuario();

    /**
     * Procesa la solicitud de autenticación llamando al componente de gestión apropiado.
     * @param datos Los datos de autenticación del usuario.
     * @return el resultado de la operación de autenticación.
     */
    @Override
    public CompletableFuture<Boolean> autenticar(DTOAutenticacion datos) {
        // La lógica del servicio es simple: delega la llamada al componente de gestión de usuarios.
        System.out.println("ServicioAutenticacion: Delegando autenticación a GestionUsuario...");
        return gestionUsuario.autenticar(datos);
    }
}

