package transporte;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.transporte.DTOConexion;

/**
 * Contrato para las fábricas de transporte.
 * Su única responsabilidad es establecer una conexión y devolver los recursos
 * encapsulados en un DTOSesion.
 */
public interface ITransporte {

    /**
     * Intenta conectar al servidor y devuelve una sesión con los recursos de bajo nivel.
     *
     * @param datosConexion DTO con la información del host y puerto.
     * @return un DTOSesion si la conexión es exitosa, o null si falla.
     */
    DTOSesion conectar(DTOConexion datosConexion);
}

