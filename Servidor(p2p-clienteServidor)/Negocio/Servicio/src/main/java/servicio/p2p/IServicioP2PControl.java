package servicio.p2p;

import dto.p2p.DTOPeerDetails;
import dto.p2p.DTOPeerConClientes;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import observador.IObservador;

import java.util.List;

/**
 * Interfaz de Alto Nivel para la aplicación.
 * Define las operaciones de negocio disponibles para la UI.
 */
public interface IServicioP2PControl {

    /**
     * Configura y arranca la red P2P.
     */
    void iniciarRed();

    /**
     * Detiene todos los servicios y cierra conexiones.
     */
    void detenerRed();

    /**
     * Fuerza un proceso de sincronización de datos (Merkle Tree) con los peers conectados.
     * Útil si se sospecha desincronización o tras reconexiones.
     */
    void sincronizarManual();

    /**
     * Obtiene la lista actual de nodos conectados.
     */
    List<DTOPeerDetails> obtenerListaPeers();

    /**
     * ✅ NUEVO: Obtiene la lista de peers con información de sus clientes conectados.
     * Útil para mostrar la topología completa de la red en la interfaz.
     */
    List<DTOPeerConClientes> obtenerPeersConClientes();

    void enviarMensajeGlobal(String usuario, String mensaje);

    void enviarMensajePrivado(String idPeerDestino, String usuario, String mensaje);

    void conectarManual(String ip, int puerto);

    boolean estaCorriendo();

    /**
     * ✅ NUEVO: Expone el servicio de sincronización P2P.
     */
    ServicioSincronizacionDatos getServicioSincronizacion();

    /**
     * Permite registrar un observador desde el controlador
     * para monitorear conexiones/desconexiones de peers.
     */
    void registrarObservadorConexiones(IObservador obs);
}