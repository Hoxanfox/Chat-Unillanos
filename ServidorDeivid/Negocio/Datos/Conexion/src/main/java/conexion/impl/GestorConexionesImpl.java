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

    // --- COLORES ANSI PARA DEBUGGING CHEBRE ---
    private static final String RESET = "\u001B[0m";
    private static final String ROJO = "\u001B[31m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String AZUL = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";

    private static final String TAG = MAGENTA + "[GestorConexiones] " + RESET;

    private final ITransporteTcp transporte;
    private final Map<String, DTOPeerDetails> poolPeers;
    private IRouterMensajes routerMensajes;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public GestorConexionesImpl() {
        System.out.println(TAG + "Inicializando Gestor de Conexiones...");
        this.poolPeers = new ConcurrentHashMap<>();
        // Nos pasamos a nosotros mismos como listener
        this.transporte = new NettyTransporteImpl(this);
    }

    public void setRouterMensajes(IRouterMensajes router) {
        this.routerMensajes = router;
        System.out.println(TAG + "Router de Mensajes vinculado correctamente.");
    }

    @Override
    public void iniciarServidor(int puertoEscucha) {
        try {
            System.out.println(TAG + "Solicitando inicio de servidor en puerto " + AZUL + puertoEscucha + RESET);
            transporte.iniciarEscucha(puertoEscucha);
        } catch (InterruptedException e) {
            System.err.println(TAG + ROJO + "Interrupción al iniciar servidor" + RESET);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void conectarAPeer(String host, int puerto) {
        System.out.println(TAG + "Solicitando conexión saliente a -> " + AMARILLO + host + ":" + puerto + RESET);
        transporte.conectarA(host, puerto);
    }

    @Override
    public void enviarMensaje(DTOPeerDetails peerDto, String mensaje) {
        // 1. Validación de entrada
        if (peerDto == null || peerDto.getId() == null) {
            System.err.println(TAG + ROJO + "Error: Se intentó enviar mensaje a un Peer NULO o sin ID." + RESET);
            return;
        }

        String targetId = peerDto.getId();
        String ipDestino = null;
        int puertoDestino = 0;
        String fuenteDatos = "DESCONOCIDO";

        // 2. Intentar obtener datos reales del Pool (Memoria)
        DTOPeerDetails peerEnMemoria = poolPeers.get(targetId);

        if (peerEnMemoria != null && peerEnMemoria.getIp() != null) {
            ipDestino = peerEnMemoria.getIp();
            puertoDestino = peerEnMemoria.getPuerto();
            fuenteDatos = "POOL (Memoria)";
        }
        // 3. Si no está en memoria, usar los datos del DTO entrante
        else if (peerDto.getIp() != null) {
            ipDestino = peerDto.getIp();
            puertoDestino = peerDto.getPuerto();
            fuenteDatos = "DTO (Entrante)";
        }
        // 4. FALLBACK CRÍTICO: Extraer IP del ID (formato "ip:puerto")
        else {
            try {
                String[] parts = targetId.split(":");
                if (parts.length >= 2) {
                    ipDestino = parts[0];
                    if (ipDestino.startsWith("/")) ipDestino = ipDestino.substring(1);
                    puertoDestino = Integer.parseInt(parts[1]);
                    fuenteDatos = "ID PARSE (Fallback)";
                }
            } catch (Exception e) { /* Falló el parsing */ }
        }

        // 5. Ejecutar envío solo si tenemos datos válidos
        if (ipDestino != null && !ipDestino.equals("null") && puertoDestino > 0) {
            // Log de depuración detallado para rastrear el origen de los datos
            System.out.println(TAG + "Enviando a " + CYAN + targetId + RESET +
                    " usando IP: " + VERDE + ipDestino + ":" + puertoDestino + RESET +
                    " [Fuente: " + AMARILLO + fuenteDatos + RESET + "]");

            transporte.enviarMensaje(ipDestino, puertoDestino, mensaje);
        } else {
            System.err.println(TAG + ROJO + "ERROR FATAL: No se pudo resolver IP para ID: " + targetId +
                    ". Datos disponibles -> DTO_IP: " + peerDto.getIp() + ", EnPool: " + (peerEnMemoria!=null) + RESET);
        }
    }

    @Override
    public void broadcast(String mensaje) {
        System.out.println(TAG + "Iniciando BROADCAST a " + poolPeers.size() + " peers...");
        poolPeers.values().forEach(peer -> {
            if(peer.getIp() != null) {
                transporte.enviarMensaje(peer.getIp(), peer.getPuerto(), mensaje);
            }
        });
    }

    @Override
    public void desconectar(DTOPeerDetails peerDto) {
        if (peerDto != null) {
            DTOPeerDetails removed = poolPeers.remove(peerDto.getId());
            if (removed != null) {
                System.out.println(TAG + AMARILLO + "Peer desconectado y removido del pool: " + peerDto.getId() + RESET);
            }
        }
    }

    @Override
    public List<DTOPeerDetails> obtenerDetallesPeers() {
        return new ArrayList<>(poolPeers.values());
    }

    @Override
    public void apagar() {
        System.out.println(TAG + "Apagando sistema de conexiones...");
        transporte.detener();
        poolPeers.clear();
    }

    // --- Eventos de Netty ---

    @Override
    public void onMensajeRecibido(String mensaje, String origen) {
        // 'origen' es la llave ip:puerto_efimero
        // System.out.println(TAG + "RAW recibido de " + origen + ": " + mensaje); // Debug muy verboso

        DTOPeerDetails peer = poolPeers.computeIfAbsent(origen, this::crearPeerDesdeOrigen);

        if (routerMensajes != null) {
            routerMensajes.procesarMensaje(mensaje, peer.getId());
        } else {
            System.err.println(TAG + ROJO + "ALERTA: Mensaje recibido pero NO hay Router configurado para procesarlo." + RESET);
        }
    }

    @Override
    public void onNuevaConexion(String origen) {
        DTOPeerDetails nuevoPeer = crearPeerDesdeOrigen(origen);
        if (nuevoPeer.getIp() != null) {
            poolPeers.put(nuevoPeer.getId(), nuevoPeer);
            System.out.println(TAG + VERDE + "Nueva conexión registrada en Pool: " + origen + RESET);
        } else {
            System.err.println(TAG + ROJO + "Error al registrar conexión: IP nula para origen " + origen + RESET);
        }
    }

    private DTOPeerDetails crearPeerDesdeOrigen(String origen) {
        try {
            String[] parts = origen.split(":");
            if (parts.length < 2) {
                System.err.println(TAG + ROJO + "Formato de origen inválido: " + origen + RESET);
                return new DTOPeerDetails(origen, "127.0.0.1", 0, "ERROR", "");
            }

            String ip = parts[0];
            if (ip.startsWith("/")) ip = ip.substring(1);

            return new DTOPeerDetails(origen, ip, Integer.parseInt(parts[1]), "ONLINE", LocalDateTime.now().format(FORMATTER));
        } catch (Exception e) {
            System.err.println(TAG + ROJO + "Excepción creando peer desde origen (" + origen + "): " + e.getMessage() + RESET);
            return new DTOPeerDetails(origen, "127.0.0.1", 0, "ERROR", "");
        }
    }
}