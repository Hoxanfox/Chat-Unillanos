package conexion;

import conexion.IGestorConexiones;
import comunicacion.IRouterMensajes;
import dto.p2p.DTOPeerDetails;
import transporte.p2p.interfaces.IMensajeListener;
import transporte.p2p.interfaces.ITransporteTcp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GestorConexionesImpl implements IGestorConexiones, IMensajeListener {

    private final ITransporteTcp transporte;
    private final Map<String, DTOPeerDetails> poolPeers;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Referencia al router (inyección circular)
    private IRouterMensajes routerMensajes;

    public GestorConexionesImpl(ITransporteTcp transporte) {
        this.transporte = transporte;
        this.poolPeers = new ConcurrentHashMap<>();

        // Si es Netty, nos inyectamos como listener
        if (transporte instanceof transporte.p2p.impl.NettyTransporteImpl) {
            ((transporte.p2p.impl.NettyTransporteImpl) transporte).setListener(this);
        }
    }

    // --- IMPORTANTE: Este es el método que te faltaba o no se reconocía ---
    public void setRouterMensajes(IRouterMensajes router) {
        this.routerMensajes = router;
    }

    @Override
    public void iniciarServidor(int puertoEscucha) {
        try {
            System.out.println("[Conexion] Iniciando servidor en puerto " + puertoEscucha);
            transporte.iniciarEscucha(puertoEscucha);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void conectarAPeer(String host, int puerto) {
        transporte.conectarA(host, puerto);
    }

    @Override
    public void enviarMensaje(DTOPeerDetails peerDto, String mensaje) {
        if (peerDto == null) return;
        DTOPeerDetails peerInterno = poolPeers.get(peerDto.getId());
        // Solo enviamos si existe y está ONLINE (o si asumimos que al estar en el pool está conectado)
        if (peerInterno != null) {
            transporte.enviarMensaje(peerInterno.getIp(), peerInterno.getPuerto(), mensaje);
        } else {
            System.err.println("[Conexion] Error envío. Peer no encontrado: " + peerDto.getId());
        }
    }

    @Override
    public void broadcast(String mensaje) {
        poolPeers.values().stream()
                .filter(p -> "ONLINE".equals(p.getEstado()))
                .forEach(p -> transporte.enviarMensaje(p.getIp(), p.getPuerto(), mensaje));
    }

    @Override
    public void desconectar(DTOPeerDetails peerDto) {
        if (peerDto == null) return;
        DTOPeerDetails eliminado = poolPeers.remove(peerDto.getId());
        if (eliminado != null) {
            System.out.println("[Conexion] Peer eliminado: " + eliminado.getId());
        }
    }

    @Override
    public List<DTOPeerDetails> obtenerDetallesPeers() {
        return new ArrayList<>(poolPeers.values());
    }

    @Override
    public void apagar() {
        transporte.detener();
        poolPeers.clear();
    }

    // --- EVENTOS DE TRANSPORTE ---

    @Override
    public void onMensajeRecibido(String mensaje, String origen) {
        // 1. Asegurar que el peer existe en nuestro registro
        DTOPeerDetails peer = poolPeers.computeIfAbsent(origen, this::crearPeerDesdeOrigen);

        // 2. Delegar al Router si está configurado
        if (routerMensajes != null) {
            routerMensajes.procesarMensaje(mensaje, peer.getId());
        } else {
            System.out.println("[Conexion] (Sin Router) MSG de " + peer.getId() + ": " + mensaje);
        }
    }

    @Override
    public void onNuevaConexion(String origen) {
        DTOPeerDetails nuevoPeer = crearPeerDesdeOrigen(origen);
        poolPeers.put(nuevoPeer.getId(), nuevoPeer);
        System.out.println("[Conexion] Conexión establecida con: " + nuevoPeer.getId());
    }

    private DTOPeerDetails crearPeerDesdeOrigen(String origen) {
        String[] parts = origen.split(":");
        // Asumimos host:puerto
        return new DTOPeerDetails(origen, parts[0], Integer.parseInt(parts[1]), "ONLINE", LocalDateTime.now().format(FORMATTER));
    }
}