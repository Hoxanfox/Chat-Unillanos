package controlador.p2p;

import dto.p2p.DTOPeerDetails;
import logger.LoggerCentral;
import observador.IObservador;
import servicio.p2p.ServicioP2P;
import servicio.p2p.IServicioP2PControl;

import java.util.List;
import java.util.function.Consumer;

public class ControladorP2P implements IObservador {

    private static final String TAG = "ControladorP2P";
    private final IServicioP2PControl servicio;

    // NUEVO: Callbacks para notificar a la vista
    private Consumer<List<DTOPeerDetails>> onListaPeersActualizada;
    private Consumer<DTOPeerDetails> onPeerConectado;
    private Consumer<DTOPeerDetails> onPeerDesconectado;

    public ControladorP2P() {
        LoggerCentral.debug(TAG, "Creando instancia de ControladorP2P...");
        this.servicio = new ServicioP2P();
        LoggerCentral.info(TAG, "ControladorP2P inicializado correctamente.");
    }

    public ControladorP2P(IServicioP2PControl servicio) {
        LoggerCentral.debug(TAG, "Creando ControladorP2P con servicio inyectado...");
        this.servicio = servicio;
        LoggerCentral.info(TAG, "ControladorP2P inicializado con inyección de dependencias.");
    }

    // --- MÉTODOS DE CONTROL ---

    public void iniciarRed() {
        LoggerCentral.info(TAG, "Iniciando red P2P...");
        servicio.iniciarRed();
        LoggerCentral.info(TAG, "Red P2P iniciada.");
    }

    public void detenerRed() {
        LoggerCentral.info(TAG, "Deteniendo red P2P...");
        servicio.detenerRed();
        LoggerCentral.info(TAG, "Red P2P detenida.");
    }

    // NUEVO: Método puente para la sincronización
    public void sincronizarManual() {
        LoggerCentral.info(TAG, "Solicitando sincronización manual...");
        servicio.sincronizarManual();
        LoggerCentral.debug(TAG, "Solicitud de sincronización enviada al servicio.");
    }

    public void enviarMensajeChat(String mensaje) {
        LoggerCentral.info(TAG, "Enviando mensaje de chat global: [" + mensaje + "]");
        servicio.enviarMensajeGlobal("UsuarioLocal", mensaje);
        LoggerCentral.debug(TAG, "Mensaje de chat enviado.");
    }

    public void enviarMensajePrivado(String idPeer, String mensaje) {
        LoggerCentral.info(TAG, "Enviando mensaje privado a [" + idPeer + "]: [" + mensaje + "]");
        servicio.enviarMensajePrivado(idPeer, "UsuarioLocal", mensaje);
        LoggerCentral.debug(TAG, "Mensaje privado enviado.");
    }

    public List<DTOPeerDetails> obtenerListaPeers() {
        LoggerCentral.debug(TAG, "Obteniendo lista de peers del servicio...");
        List<DTOPeerDetails> peers = servicio.obtenerListaPeers();
        LoggerCentral.debug(TAG, "Lista obtenida: " + peers.size() + " peers");
        return peers;
    }

    public void conectarManual(String ip, int puerto) {
        LoggerCentral.info(TAG, "Solicitando conexión manual a " + ip + ":" + puerto);
        servicio.conectarManual(ip, puerto);
        LoggerCentral.debug(TAG, "Solicitud de conexión enviada.");
    }

    public boolean isRedIniciada() {
        boolean estado = servicio.estaCorriendo();
        LoggerCentral.debug(TAG, "Estado de la red consultado: " + (estado ? "CORRIENDO" : "DETENIDA"));
        return estado;
    }

    // === NUEVO: MÉTODOS PARA SUSCRIPCIÓN DE OBSERVADORES ===

    /**
     * Registra este controlador como observador del GestorConexiones
     * para recibir notificaciones automáticas de cambios en los peers.
     * DEBE llamarse DESPUÉS de iniciarRed().
     */
    public void suscribirseAEventosConexion() {
        LoggerCentral.info(TAG, "Suscribiendo ControladorP2P a eventos de conexión...");
        if (servicio instanceof ServicioP2P) {
            ((ServicioP2P) servicio).registrarObservadorConexiones(this);
            LoggerCentral.info(TAG, "✅ ControladorP2P suscrito a eventos de GestorConexiones.");
        } else {
            LoggerCentral.error(TAG, "❌ No se pudo suscribir: servicio no es instancia de ServicioP2P.");
        }
    }

    /**
     * Permite que la vista se suscriba para recibir actualizaciones de la lista de peers.
     */
    public void suscribirActualizacionLista(Consumer<List<DTOPeerDetails>> callback) {
        LoggerCentral.debug(TAG, "Vista suscrita a actualizaciones de lista de peers.");
        this.onListaPeersActualizada = callback;
    }

    /**
     * Permite que la vista se suscriba para recibir notificaciones de nuevas conexiones.
     */
    public void suscribirConexionPeer(Consumer<DTOPeerDetails> callback) {
        LoggerCentral.debug(TAG, "Vista suscrita a eventos de conexión de peers.");
        this.onPeerConectado = callback;
    }

    /**
     * Permite que la vista se suscriba para recibir notificaciones de desconexiones.
     */
    public void suscribirDesconexionPeer(Consumer<DTOPeerDetails> callback) {
        LoggerCentral.debug(TAG, "Vista suscrita a eventos de desconexión de peers.");
        this.onPeerDesconectado = callback;
    }

    // === IMPLEMENTACIÓN DEL PATRÓN OBSERVER ===

    @Override
    @SuppressWarnings("unchecked")
    public void actualizar(String tipoDeDato, Object datos) {
        LoggerCentral.debug(TAG, "Evento recibido del patrón Observer: [" + tipoDeDato + "]");

        switch (tipoDeDato) {
            case "LISTA_PEERS":
                if (datos instanceof List && onListaPeersActualizada != null) {
                    List<DTOPeerDetails> peers = (List<DTOPeerDetails>) datos;
                    LoggerCentral.info(TAG, "Notificando actualización de lista: " + peers.size() + " peers");
                    onListaPeersActualizada.accept(peers);
                } else {
                    LoggerCentral.warn(TAG, "Evento LISTA_PEERS sin callback o datos inválidos.");
                }
                break;

            case "PEER_CONECTADO":
                if (datos instanceof DTOPeerDetails && onPeerConectado != null) {
                    DTOPeerDetails peer = (DTOPeerDetails) datos;
                    LoggerCentral.info(TAG, "Notificando conexión de peer: " + peer.getId());
                    onPeerConectado.accept(peer);
                } else {
                    LoggerCentral.warn(TAG, "Evento PEER_CONECTADO sin callback o datos inválidos.");
                }
                break;

            case "PEER_DESCONECTADO":
                if (datos instanceof DTOPeerDetails && onPeerDesconectado != null) {
                    DTOPeerDetails peer = (DTOPeerDetails) datos;
                    LoggerCentral.warn(TAG, "Notificando desconexión de peer: " + peer.getId());
                    onPeerDesconectado.accept(peer);
                } else {
                    LoggerCentral.warn(TAG, "Evento PEER_DESCONECTADO sin callback o datos inválidos.");
                }
                break;

            default:
                LoggerCentral.warn(TAG, "Evento no manejado: [" + tipoDeDato + "]");
        }
    }
}