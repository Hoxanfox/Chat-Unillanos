package servicio.p2p;

import dto.p2p.DTOPeerDetails;
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

    void enviarMensajeGlobal(String usuario, String mensaje);

    void enviarMensajePrivado(String idPeerDestino, String usuario, String mensaje);

    void conectarManual(String ip, int puerto);

    boolean estaCorriendo();
}