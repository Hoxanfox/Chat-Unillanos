package controlador.conexion;

/**
 * Interfaz (contrato) para el controlador de conexión.
 * Define las acciones que la vista puede solicitar al controlador.
 */
public interface IControladorConexion {
    /**
     * Pasa la solicitud de conexión desde la vista a las capas inferiores.
     * @param ip La dirección IP a la que se intentará conectar.
     * @return true si la conexión fue exitosa, false de lo contrario.
     */
    boolean conectar(String ip);
}
