package comunicacion.peticionesPull;

/**
 * Constantes públicas para acciones de comunicación.
 */
public final class AccionesComunicacion {
    private AccionesComunicacion() {}

    public static final String PEER_JOIN = "registrarNuevoPeer";
    public static final String PEER_LIST = "listarPeers";
    // Nuevas acciones para notificaciones push entre peers
    public static final String PEER_PUSH = "peerPushNotification"; // notifica nuevo peer
    public static final String PEER_UPDATE = "peerListUpdate"; // notifica actualización de lista de peers
    // Añadir otras acciones aquí según se necesite
}
