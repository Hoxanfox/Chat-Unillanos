package servicio.conexion;

import fachada.gestionConexion.FachadaConexionImpl;
import fachada.gestionConexion.IFachadaConexion;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de conexión.
 * Delega la tarea a la Fachada.
 */
public class ServicioConexion implements IServicioConexion {

    private final IFachadaConexion fachadaConexion;

    public ServicioConexion() {
        this.fachadaConexion = new FachadaConexionImpl();
    }

    @Override
    public CompletableFuture<Boolean> conectar() {
        return fachadaConexion.conectar();
    }
}

