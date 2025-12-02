package gestorP2P.interfaces;

// Imports corregidos
import conexion.p2p.interfaces.IGestorConexiones;
import conexion.p2p.interfaces.IRouterMensajes;

public interface IServicioP2P {
    void inicializar(IGestorConexiones gestor, IRouterMensajes router);
    void iniciar();
    void detener();
    String getNombre();
}