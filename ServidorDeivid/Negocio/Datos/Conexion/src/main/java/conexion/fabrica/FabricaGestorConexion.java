package conexion.fabrica;

import conexion.GestorConexion;
import conexion.IGestorConexion;

/**
 * Fábrica sencilla para obtener la instancia de IGestorConexion.
 * Permite desacoplar código que dependa de la interfaz y facilita pruebas.
 */
public final class FabricaGestorConexion {

    private FabricaGestorConexion() {}

    public static IGestorConexion crearGestor() {
        return GestorConexion.getInstancia();
    }
}

