package servicio.p2p;

import dto.p2p.DTOPeerDetails;
import java.util.List;

/**
 * Interfaz de Alto Nivel para la aplicación.
 * Define las operaciones de negocio que la UI o el Main pueden solicitar.
 * Oculta la complejidad de la Fachada y los Servicios internos.
 */
public interface IServicioP2PControl {

    /**
     * Configura y arranca la red P2P.
     * Incluye levantar el servidor, buscar semillas y sincronizar.
     */
    void iniciarRed();

    /**
     * Detiene todos los servicios y cierra conexiones.
     */
    void detenerRed();

    /**
     * Obtiene la lista actual de nodos conectados.
     */
    List<DTOPeerDetails> obtenerListaPeers();

    /**
     * Envía un mensaje de chat a toda la red (Broadcast).
     */
    void enviarMensajeGlobal(String usuario, String mensaje);

    /**
     * Envía un mensaje privado a un peer específico.
     */
    void enviarMensajePrivado(String idPeerDestino, String usuario, String mensaje);

    /**
     * Conecta manualmente a una IP/Puerto (útil para debug o redes privadas).
     */
    void conectarManual(String ip, int puerto);

    /**
     * Verifica si el servicio está corriendo.
     */
    boolean estaCorriendo();
}