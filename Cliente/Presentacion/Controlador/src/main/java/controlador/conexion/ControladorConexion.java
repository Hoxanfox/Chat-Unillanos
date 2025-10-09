package controlador.conexion;

import servicio.conexion.IServicioConexion;
import servicio.conexion.ServicioConexion;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación del controlador de conexión.
 * Delega la tarea al servicio correspondiente.
 */
public class ControladorConexion implements IControladorConexion {

    private final IServicioConexion servicioConexion;

    public ControladorConexion() {
        this.servicioConexion = new ServicioConexion();
    }

    @Override
    public CompletableFuture<Boolean> conectar() {
        return servicioConexion.conectar();
    }
}

