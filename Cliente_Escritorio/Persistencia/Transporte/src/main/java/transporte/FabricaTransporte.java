package transporte;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.transporte.DTOConexion;

/**
 * Fábrica dedicada a la creación de sesiones de transporte.
 */
public class FabricaTransporte {

    public DTOSesion iniciarConexion(DTOConexion datosConexion) {
        // La fábrica utiliza una implementación de ITransporte para crear la sesión.
        ITransporte transporte = new NettyClienteTransporte();
        return transporte.conectar(datosConexion);
    }
}

