package conexion;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.transporte.DTOConexion;
import observador.IObservador;

import java.io.IOException;

/**
 * Interfaz que define la funcionalidad pública del gestor de conexiones.
 * Permite desacoplar el uso del gestor de su implementación concreta.
 */
public interface IGestorConexion {

    // Sesión legacy
    void setSesion(DTOSesion sesion);
    DTOSesion getSesion();
    void cerrarSesion();

    // Pools
    void agregarSesionCliente(DTOSesion sesion);
    void agregarSesionPeer(DTOSesion sesion);
    DTOSesion obtenerSesionCliente(long timeoutMs);
    DTOSesion obtenerSesionPeer(long timeoutMs);
    DTOSesion obtenerSesionPorDireccion(String ip, int port, long timeoutMs, boolean buscarEnPeers);
    void liberarSesionCliente(DTOSesion sesion);
    void liberarSesionPeer(DTOSesion sesion);

    // Ciclo de vida
    void cerrarTodo();

    // Listeners
    void iniciarEscuchaClientes(String host, int puerto) throws IOException;
    void iniciarEscuchaPeers(String host, int puerto) throws IOException;
    void detenerEscuchaClientes();
    void detenerEscuchaPeers();
    void inicializarServidor(String host, int puertoClientes, int puertoPeers) throws IOException;

    // Cliente
    DTOSesion conectarComoCliente(String host, int puerto, boolean comoPeer);
    DTOSesion conectarComoCliente(DTOConexion datosConexion, boolean comoPeer);
    DTOSesion inicializarComoCliente(String host, int puerto, boolean comoPeer);

    // Observers
    void registrarObservador(IObservador observador);
    void removerObservador(IObservador observador);
}

