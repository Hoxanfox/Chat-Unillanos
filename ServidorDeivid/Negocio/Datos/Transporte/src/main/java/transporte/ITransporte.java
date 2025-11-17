package transporte;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.transporte.DTOConexion;

/**
 * Contrato para cualquier componente que pueda crear una sesión de transporte.
 * Su única responsabilidad es establecer la conexión y devolver los recursos.
 */
public interface ITransporte {

    /**
     * Intenta establecer una conexión y devuelve una sesión con los recursos.
     * @param datosConexion Los datos para la conexión (IP y puerto).
     * @return un DTOSesion con los recursos si la conexión es exitosa, o null si falla.
     */
    DTOSesion conectar(DTOConexion datosConexion);
}

