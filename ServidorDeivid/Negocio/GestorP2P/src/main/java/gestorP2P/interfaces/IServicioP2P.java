package gestorP2P.interfaces;

// Imports corregidos
import conexion.interfaces.IGestorConexiones;
import conexion.interfaces.IRouterMensajes;

public interface IServicioP2P {
    void inicializar(IGestorConexiones gestor, IRouterMensajes router);
    void iniciar();
    void detener();
    String getNombre();
}