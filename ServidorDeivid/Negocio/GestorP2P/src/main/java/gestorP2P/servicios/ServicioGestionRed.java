package gestorP2P.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import conexion.interfaces.IRouterMensajes;
import conexion.interfaces.IGestorConexiones;
import configuracion.Configuracion;
import dominio.p2p.Peer;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.p2p.DTOPeerDetails;
import gestorP2P.interfaces.IServicioP2P;
import repositorio.p2p.PeerRepositorio;

import java.util.List;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class ServicioGestionRed implements IServicioP2P {

    // --- COLORES ANSI ---
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String ROJO = "\u001B[31m";
    private static final String CYAN = "\u001B[36m";
    private static final String TAG = CYAN + "[GestionRed] " + RESET;

    private IGestorConexiones gestorConexiones;
    private final Configuracion config;
    private final PeerRepositorio repositorio;
    private final Gson gson;
    private Timer timerHeartbeat;

    public ServicioGestionRed() {
        this.config = Configuracion.getInstance();
        this.repositorio = new PeerRepositorio();
        this.gson = new Gson();
    }

    @Override
    public String getNombre() { return "ServicioGestionRed"; }

    @Override
    public void inicializar(IGestorConexiones gestor, IRouterMensajes router) {
        this.gestorConexiones = gestor;

        // ================================================================
        // 1. ROL SERVIDOR: RECIBIR HEARTBEAT (IDENTIFICACIÓN REAL)
        // ================================================================
        router.registrarAccion("heartbeat", (datosJson, origenId) -> {
            try {
                JsonObject obj = datosJson.getAsJsonObject();
                String uuid = obj.get("uuid").getAsString();
                int puertoReal = obj.get("listenPort").getAsInt();

                // La IP la tomamos de la conexión física (origenId = ip:puerto_efimero)
                String ipReal = origenId.split(":")[0];
                if (ipReal.startsWith("/")) ipReal = ipReal.substring(1);

                String infoReal = uuid + " (" + ipReal + ":" + puertoReal + ")";
                System.out.println(TAG + "❤ Heartbeat recibido de: " + AMARILLO + infoReal + RESET);

                // ACTUALIZAMOS LA IDENTIDAD EN BASE DE DATOS
                // Esto corrige el problema de no saber el puerto real
                Peer peer = new Peer();
                peer.setId(UUID.fromString(uuid));
                peer.setIp(ipReal);
                peer.setEstado(Peer.Estado.ONLINE);

                // Guardamos usando el socketInfo REAL (ip:puerto_escucha) no el efímero
                repositorio.guardarOActualizarPeer(peer, ipReal + ":" + puertoReal);

                return new DTOResponse("heartbeat", "success", "Vivo y reconocido", null);
            } catch (Exception e) {
                return new DTOResponse("heartbeat", "error", "Datos corruptos", null);
            }
        });

        // ================================================================
        // 2. ROL SERVIDOR: PETICIÓN DE UNIÓN (AñadirPeer)
        // ================================================================
        router.registrarAccion("añadirPeer", (datosJson, origenId) -> {
            try {
                JsonObject obj = datosJson.getAsJsonObject();
                String ip = obj.get("ip").getAsString();
                int puerto = obj.get("puerto").getAsInt();

                System.out.println(TAG + "Petición de Sync (añadirPeer) de: " + ip + ":" + puerto);

                // Guardamos temporalmente (aunque sea con ID desconocido, se arreglará con el heartbeat)
                Peer temporal = new Peer();
                temporal.setIp(ip);
                temporal.setEstado(Peer.Estado.ONLINE);
                repositorio.guardarOActualizarPeer(temporal, ip + ":" + puerto);

                // Enviamos la lista
                List<PeerRepositorio.PeerInfo> peersDb = repositorio.listarPeersInfo();
                List<DTOPeerDetails> lista = peersDb.stream()
                        .map(p -> new DTOPeerDetails(p.id.toString(), p.ip, p.puerto, "ONLINE", ""))
                        .collect(Collectors.toList());

                return new DTOResponse("añadirPeer", "success", "Lista enviada", gson.toJsonTree(lista));
            } catch (Exception e) {
                return new DTOResponse("añadirPeer", "error", e.getMessage(), null);
            }
        });

        // ================================================================
        // 3. ROL CLIENTE: RESPUESTA DE AÑADIR PEER (Fase 1 Completada)
        // ================================================================
        router.registrarManejadorRespuesta("añadirPeer", (response) -> {
            if (response.fueExitoso() && response.getData() != null) {
                System.out.println(TAG + VERDE + "Lista recibida. Guardando y desconectando para Fase 2..." + RESET);

                // 1. Guardar peers recibidos
                procesarListaPeersRecibida(response.getData().getAsJsonArray());

                // 2. DESCONEXIÓN TÁCTICA
                // Esto fuerza a cerrar sockets efímeros y limpiar el estado antes de los heartbeats
                // Desconectamos de todos los que tenemos en memoria actualmente
                gestorConexiones.obtenerDetallesPeers().forEach(p -> gestorConexiones.desconectar(p));

                // 3. INICIAR FASE 2: Heartbeats masivos con identidad real
                System.out.println(TAG + "Iniciando difusión de Heartbeats (Identidad)...");
                enviarHeartbeatsATodos();
            }
        });
    }

    private void procesarListaPeersRecibida(JsonArray listaJson) {
        String miHost = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();

        for (JsonElement elem : listaJson) {
            try {
                JsonObject p = elem.getAsJsonObject();
                String ip = p.get("ip").getAsString();
                int puerto = p.get("puerto").getAsInt();

                if (ip.equals(miHost) && puerto == miPuerto) continue;

                String idStr = p.has("id") ? p.get("id").getAsString() : UUID.randomUUID().toString();

                Peer peer = new Peer();
                peer.setId(UUID.fromString(idStr));
                peer.setIp(ip);
                peer.setEstado(Peer.Estado.ONLINE);

                repositorio.guardarOActualizarPeer(peer, ip + ":" + puerto);
            } catch (Exception e) { /* ignorar errores de parseo individual */ }
        }
    }

    /**
     * FASE 2: Recorre la BD y envía "Hola, soy UUID X en Puerto Y" a todos.
     */
    private void enviarHeartbeatsATodos() {
        // Recuperamos mi propia identidad para enviarla
        String miIp = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();
        Peer yo = repositorio.obtenerPorSocketInfo(miIp + ":" + miPuerto);

        if (yo == null) return;

        JsonObject payload = new JsonObject();
        payload.addProperty("uuid", yo.getId().toString());
        payload.addProperty("listenPort", miPuerto);

        DTORequest req = new DTORequest("heartbeat", payload);
        String jsonReq = gson.toJson(req);

        // Listamos todos los peers conocidos de la BD
        List<PeerRepositorio.PeerInfo> conocidos = repositorio.listarPeersInfo();

        for (PeerRepositorio.PeerInfo destino : conocidos) {
            // No enviarnos a nosotros mismos
            if (destino.ip.equals(miIp) && destino.puerto == miPuerto) continue;

            System.out.println(TAG + "Conectando para Heartbeat -> " + destino.ip + ":" + destino.puerto);

            // Conectar
            gestorConexiones.conectarAPeer(destino.ip, destino.puerto);

            // Esperar un poco y enviar (en hilo separado para no bloquear el loop)
            new Thread(() -> {
                try {
                    Thread.sleep(1500); // Esperar handshake TCP
                    // Buscar el DTO de conexión activa (ID temporal host:port)
                    String tempId = destino.ip + ":" + destino.puerto;

                    gestorConexiones.obtenerDetallesPeers().stream()
                            .filter(p -> p.getId().equals(tempId))
                            .findFirst()
                            .ifPresent(p -> {
                                gestorConexiones.enviarMensaje(p, jsonReq);
                                System.out.println(TAG + "❤ Heartbeat enviado a " + tempId);
                            });
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        }
    }

    @Override
    public void iniciar() {
        // ... (Lógica de inicio e identidad igual) ...
        System.out.println(TAG + "Iniciando secuencia de arranque...");
        String miIp = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();
        String miSocketInfo = miIp + ":" + miPuerto;

        Peer miPeer = repositorio.obtenerPorSocketInfo(miSocketInfo);
        String seedHost = config.getPeerInicialHost();
        int seedPort = config.getPeerInicialPuerto();

        if (miPeer == null) {
            miPeer = new Peer();
            miPeer.setIp(miIp);
            miPeer.setEstado(Peer.Estado.ONLINE);
            repositorio.guardarOActualizarPeer(miPeer, miSocketInfo);
            System.out.println(TAG + "Identidad creada (" + (seedHost!=null?"Joiner":"Genesis") + "): " + miPeer.getId());
        } else {
            System.out.println(TAG + "Identidad recuperada: " + miPeer.getId());
        }

        new Thread(() -> gestorConexiones.iniciarServidor(miPuerto)).start();

        // FASE 1: SYNC CON SEMILLA
        if (seedHost != null && seedPort > 0) {
            System.out.println(TAG + "Iniciando Fase 1: Sincronización con Semilla...");
            iniciarFaseSync(seedHost, seedPort, miIp, miPuerto);
        }

        // Tarea periódica de Heartbeats (Mantenimiento)
        if (timerHeartbeat == null) {
            timerHeartbeat = new Timer();
            timerHeartbeat.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    // Solo si no estamos en medio del arranque inicial (opcional)
                    // enviarHeartbeatsATodos();
                }
            }, 60000, 60000); // Cada minuto
        }
    }

    private void iniciarFaseSync(String host, int port, String miIp, int miPuerto) {
        try {
            Thread.sleep(500);
            gestorConexiones.conectarAPeer(host, port);

            JsonObject payload = new JsonObject();
            payload.addProperty("ip", miIp);
            payload.addProperty("puerto", miPuerto);
            DTORequest req = new DTORequest("añadirPeer", payload);

            String seedId = host + ":" + port;

            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    gestorConexiones.obtenerDetallesPeers().stream()
                            .filter(p -> p.getId().equals(seedId))
                            .findFirst()
                            .ifPresent(p -> {
                                System.out.println(TAG + "Solicitando lista de peers (Sync)...");
                                gestorConexiones.enviarMensaje(p, gson.toJson(req));
                            });
                } catch (Exception e) {}
            }).start();

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void detener() {
        if (timerHeartbeat != null) timerHeartbeat.cancel();
    }
}