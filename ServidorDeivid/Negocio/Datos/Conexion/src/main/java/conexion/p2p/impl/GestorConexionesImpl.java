package conexion.p2p.impl;

import conexion.p2p.interfaces.IGestorConexiones;
import conexion.p2p.interfaces.IRouterMensajes;
import dto.p2p.DTOPeerDetails;
import observador.IObservador;
import observador.ISujeto;
import transporte.p2p.interfaces.IMensajeListener;
import transporte.p2p.interfaces.ITransporteTcp;
import transporte.p2p.impl.NettyTransporteImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class GestorConexionesImpl implements IGestorConexiones, IMensajeListener, ISujeto {

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

    // NUEVO: Callback para notificar desconexiones
    private Consumer<String> onPeerDisconnectedCallback;

    // NUEVO: Lista de observadores (thread-safe)
    private final List<IObservador> observadores;

    public GestorConexionesImpl() {
        System.out.println(TAG + "Inicializando Gestor de Conexiones...");
        this.poolPeers = new ConcurrentHashMap<>();
        this.observadores = new CopyOnWriteArrayList<>();
        // Nos pasamos a nosotros mismos como listener
        this.transporte = new NettyTransporteImpl(this);
    }

    /**
     * Registra un callback que será llamado cuando un peer se desconecta.
     * @param callback Función que recibe el ID del peer desconectado (formato "ip:puerto")
     */
    public void setOnPeerDisconnectedCallback(Consumer<String> callback) {
        this.onPeerDisconnectedCallback = callback;
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

    // --- NUEVO: Actualiza la "Cara Pública" del peer cuando recibimos Heartbeat ---
    @Override
    public void actualizarPuertoServidor(String connectionId, int puertoReal) {
        DTOPeerDetails peer = poolPeers.get(connectionId);
        if (peer != null) {
            System.out.println(TAG + "Actualizando identidad visual: " + connectionId + " es servidor en puerto " + VERDE + puertoReal + RESET);
            peer.setPuertoServidor(puertoReal);
        }
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
            // Log de depuración detallado
            // System.out.println(TAG + "Enviando a " + CYAN + targetId + RESET +
            //                    " usando IP: " + VERDE + ipDestino + ":" + puertoDestino + RESET +
            //                    " [Fuente: " + AMARILLO + fuenteDatos + RESET + "]");

            transporte.enviarMensaje(ipDestino, puertoDestino, mensaje);
        } else {
            System.err.println(TAG + ROJO + "ERROR FATAL: No se pudo resolver IP para ID: " + targetId +
                    ". Datos disponibles -> DTO_IP: " + peerDto.getIp() + ", EnPool: " + (peerEnMemoria != null) + RESET);
        }
    }

    @Override
    public void broadcast(String mensaje) {
        System.out.println(TAG + "Iniciando BROADCAST a " + poolPeers.size() + " peers...");
        poolPeers.values().forEach(peer -> {
            if (peer.getIp() != null) {
                transporte.enviarMensaje(peer.getIp(), peer.getPuerto(), mensaje);
            }
        });
    }

    @Override
    public void desconectar(DTOPeerDetails peerDto) {
        if (peerDto != null) {
            DTOPeerDetails removed = poolPeers.remove(peerDto.getId());
            if (removed != null) {
                System.out.println(TAG + AMARILLO + "Desconectando peer: " + removed.getId() + RESET);

                // --- ACTUALIZACIÓN CRÍTICA PARA SYNC & SPLASH ---
                // Cortamos la conexión física en Netty
                if (removed.getIp() != null && removed.getPuerto() > 0) {
                    transporte.desconectar(removed.getIp(), removed.getPuerto());
                }
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
        DTOPeerDetails peer = poolPeers.computeIfAbsent(origen, this::crearPeerDesdeOrigen);

        if (routerMensajes != null) {
            routerMensajes.procesarMensaje(mensaje, peer.getId());
        } else {
            System.err.println(TAG + ROJO + "ALERTA: Mensaje recibido pero NO hay Router configurado." + RESET);
        }
    }

    @Override
    public void onNuevaConexion(String origen) {
        DTOPeerDetails nuevoPeer = crearPeerDesdeOrigen(origen);
        if (nuevoPeer.getIp() != null) {
            poolPeers.put(nuevoPeer.getId(), nuevoPeer);
            System.out.println(TAG + VERDE + "Nueva conexión registrada en Pool: " + origen + RESET);

            // NUEVO: Notificar a los observadores sobre la nueva conexión
            notificarObservadores("PEER_CONECTADO", nuevoPeer);
            notificarObservadores("LISTA_PEERS", obtenerDetallesPeers());
        } else {
            System.err.println(TAG + ROJO + "Error al registrar conexión: IP nula para origen " + origen + RESET);
        }
    }

    // --- NUEVO: Manejo de Desconexión Automática (Callback de Netty) ---
    @Override
    public void onDesconexion(String origen) {
        if (poolPeers.containsKey(origen)) {
            DTOPeerDetails peerDesconectado = poolPeers.get(origen);
            System.out.println(TAG + AMARILLO + "Detectada caída/cierre de canal: " + origen + ". Eliminando del pool." + RESET);
            poolPeers.remove(origen);

            // NUEVO: Llamar al callback de desconexión si está presente
            if (onPeerDisconnectedCallback != null) {
                onPeerDisconnectedCallback.accept(origen);
            }

            // NUEVO: Notificar a los observadores sobre la desconexión
            notificarObservadores("PEER_DESCONECTADO", peerDesconectado);
            notificarObservadores("LISTA_PEERS", obtenerDetallesPeers());
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
            System.err.println(TAG + ROJO + "Excepción creando peer: " + e.getMessage() + RESET);
            return new DTOPeerDetails(origen, "127.0.0.1", 0, "ERROR", "");
        }
    }

    // === IMPLEMENTACIÓN DEL PATRÓN OBSERVER ===

    @Override
    public void registrarObservador(IObservador observador) {
        if (observador != null && !observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println(TAG + CYAN + "Observador registrado. Total: " + observadores.size() + RESET);
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        if (observadores.remove(observador)) {
            System.out.println(TAG + AMARILLO + "Observador removido. Total: " + observadores.size() + RESET);
        }
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador observador : observadores) {
            try {
                observador.actualizar(tipoDeDato, datos);
            } catch (Exception e) {
                System.err.println(TAG + ROJO + "Error al notificar observador: " + e.getMessage() + RESET);
            }
        }
    }
}
