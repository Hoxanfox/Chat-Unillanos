package controlador.conexion;


import servicio.conexion.IServicioConexion;
import servicio.conexion.ServicioConexion;

/**
 * Implementación del controlador que gestiona la lógica de conexión.
 * Actúa como intermediario entre la vista y el servicio de conexión.
 */
public class ControladorConexion implements IControladorConexion {

    private final IServicioConexion servicioConexion;

    /**
     * Constructor que inicializa el controlador con una instancia del servicio.
     * Esto permite la inyección de dependencias y facilita las pruebas.
     */
    public ControladorConexion() {
        // En una aplicación más grande, esto podría ser inyectado por un framework.
        this.servicioConexion = new ServicioConexion();
    }

    /**
     * Implementación del método conectar que delega la llamada al servicio correspondiente.
     * @param ip La dirección IP a la que se intentará conectar.
     * @return el resultado de la operación de conexión del servicio.
     */
    @Override
    public boolean conectar(String ip) {
        // El controlador delega la lógica de negocio al servicio.
        return servicioConexion.conectar(ip);
    }
}
