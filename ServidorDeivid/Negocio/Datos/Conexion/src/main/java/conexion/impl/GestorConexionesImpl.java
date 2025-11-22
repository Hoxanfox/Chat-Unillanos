package conexion.impl;

import conexion.interfaces.IGestorConexiones;
import conexion.interfaces.IRouterMensajes;
import dto.p2p.DTOPeerDetails;
import transporte.p2p.interfaces.IMensajeListener;
import transporte.p2p.interfaces.ITransporteTcp;
import transporte.p2p.impl.NettyTransporteImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GestorConexionesImpl implements IGestorConexiones, IMensajeListener {

    private final ITransporteTcp transporte;
    private final Map<String, DTOPeerDetails> poolPeers;
    private IRouterMensajes routerMensajes;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructor por defecto (Recomendado).
     * Inicializa Netty automáticamente para que la capa superior no tenga que hacerlo.
     */
    public GestorConexionesImpl() {
        this.poolPeers = new ConcurrentHashMap<>();
        // Encapsulamos la creación del transporte aquí
        // Pasamos 'this' porque somos el Listener
        this.transporte = new NettyTransporteImpl(this);
    }

    /**
     * Constructor para inyección manual (Opcional, para tests).
     */
    public GestorConexionesImpl(ITransporteTcp transporte) {
        this.transporte = transporte;
        this.poolPeers = new ConcurrentHashMap<>();
        if (transporte instanceof NettyTransporteImpl) {
            ((NettyTransporteImpl) transporte).setListener(this);
        }
    }

    // --- Método de Inyección (Setter) para resolver el error ---
    public void setRouterMensajes(IRouterMensajes router) {
        this.routerMensajes = router;
    }

    @Override
    public void iniciarServidor(int puertoEscucha) {
        try {
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
        transporte.enviarMensaje(peerDto.getIp(), peerDto.getPuerto(), mensaje);
    }

    @Override
    public void broadcast(String mensaje) {
        poolPeers.values().forEach(peer -> {
            transporte.enviarMensaje(peer.getIp(), peer.getPuerto(), mensaje);
        });
    }

    @Override
    public void desconectar(DTOPeerDetails peerDto) {
        if (peerDto != null) poolPeers.remove(peerDto.getId());
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

    @Override
    public void onMensajeRecibido(String mensaje, String origen) {
        DTOPeerDetails peer = poolPeers.computeIfAbsent(origen, this::crearPeerDesdeOrigen);

        if (routerMensajes != null) {
            routerMensajes.procesarMensaje(mensaje, peer.getId());
        } else {
            System.out.println("[Conexion] WARN: Mensaje recibido sin Router configurado.");
        }
    }

    @Override
    public void onNuevaConexion(String origen) {
        DTOPeerDetails nuevoPeer = crearPeerDesdeOrigen(origen);
        poolPeers.put(nuevoPeer.getId(), nuevoPeer);
        System.out.println("[Conexion] Nueva conexión: " + origen);
    }

    private DTOPeerDetails crearPeerDesdeOrigen(String origen) {
        String[] parts = origen.split(":");
        return new DTOPeerDetails(origen, parts[0], Integer.parseInt(parts[1]), "ONLINE", LocalDateTime.now().format(FORMATTER));
    }
}