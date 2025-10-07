package servicio.conexion;

/**
 * Interfaz (contrato) que define las operaciones para la conexión con el servidor.
 * Desacopla la lógica de negocio de la interfaz de usuario.
 */
public interface IServicioConexion {
    /**
     * Intenta establecer una conexión con el servidor en la dirección IP proporcionada.
     * @param ip La dirección IP del servidor.
     * @return true si la conexión fue exitosa, false en caso contrario.
     */
    boolean conectar(String ip);
}
