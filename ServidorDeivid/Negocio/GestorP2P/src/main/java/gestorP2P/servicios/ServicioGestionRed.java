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

    private static final String TAG = "\u001B[36m[GestionRed] \u001B[0m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String RESET = "\u001B[0m";

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

        // =========================================================================
        // RUTA 1: AÑADIR PEER (Presentación Inicial)
        // =========================================================================

        // SERVIDOR: Recibe "Hola, quiero entrar". Guarda temporalmente y dice OK.
        router.registrarAccion("añadirPeer", (datosJson, origenId) -> {
            try {
                JsonObject obj = datosJson.getAsJsonObject();
                String ip = obj.get("ip").getAsString();
                int puerto = obj.get("puerto").getAsInt();

                System.out.println(TAG + "Solicitud 'añadirPeer' de " + ip + ":" + puerto);

                Peer temporal = new Peer();
                temporal.setIp(ip);
                temporal.setEstado(Peer.Estado.ONLINE);
                repositorio.guardarOActualizarPeer(temporal, ip + ":" + puerto);

                return new DTOResponse("añadirPeer", "success", "OK. Solicita sync ahora.", null);
            } catch (Exception e) {
                return new DTOResponse("añadirPeer", "error", e.getMessage(), null);
            }
        });

        // CLIENTE: Me aceptaron -> Pido la lista (Siguiente paso)
        router.registrarManejadorRespuesta("añadirPeer", (response) -> {
            if (response.fueExitoso()) {
                System.out.println(TAG + "Admitido en la red. Solicitando lista (sincronizar)...");
                // Enviamos petición de sincronización a la semilla (usando broadcast porque es la única conexión activa)
                DTORequest req = new DTORequest("sincronizar", null);
                gestorConexiones.broadcast(gson.toJson(req));
            }
        });


        // =========================================================================
        // RUTA 2: SINCRONIZAR (Intercambio de Datos)
        // =========================================================================

        // SERVIDOR: "Aquí tienes la lista de todos los que conozco"
        router.registrarAccion("sincronizar", (datosJson, origenId) -> {
            System.out.println(TAG + "Enviando lista de peers a " + origenId);
            List<PeerRepositorio.PeerInfo> peersDb = repositorio.listarPeersInfo();
            List<DTOPeerDetails> lista = peersDb.stream()
                    .map(p -> new DTOPeerDetails(p.id.toString(), p.ip, p.puerto, "ONLINE", ""))
                    .collect(Collectors.toList());

            return new DTOResponse("sincronizar", "success", "Lista entregada", gson.toJsonTree(lista));
        });

        // CLIENTE: Recibe lista -> Guarda -> SPLASH (Cuelga el teléfono) -> FASE 2
        router.registrarManejadorRespuesta("sincronizar", (response) -> {
            if (response.fueExitoso() && response.getData() != null) {
                System.out.println(TAG + "Lista recibida. Procesando...");

                // A. Guardar en BD Local
                JsonArray lista = response.getData().getAsJsonArray();
                guardarPeersEnBD(lista);

                // B. SPLASH (Desconexión Táctica)
                // Aquí cortamos la conexión con la semilla. Al hacerlo nosotros,
                // la semilla también recibe el cierre y ambos sockets mueren.
                System.out.println(TAG + AMARILLO + ">>> SPLASH! Cortando conexiones efímeras... <<<" + RESET);
                List<DTOPeerDetails> activos = gestorConexiones.obtenerDetallesPeers();
                for(DTOPeerDetails p : activos) {
                    gestorConexiones.desconectar(p);
                }

                // C. FASE 2: Iniciar Conexiones Reales (Heartbeats)
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (Exception e) {}
                    System.out.println(TAG + "Iniciando conexiones estables (Heartbeats)...");
                    enviarHeartbeatsATodos();
                }).start();
            }
        });


        // =========================================================================
        // RUTA 3: HEARTBEAT (Fase 2 - Vida Real)
        // =========================================================================
        router.registrarAccion("heartbeat", (datosJson, origenId) -> {
            try {
                JsonObject obj = datosJson.getAsJsonObject();
                String uuid = obj.get("uuid").getAsString();
                int puertoListen = obj.get("listenPort").getAsInt();

                // IP Real del socket físico
                String ipReal = origenId.split(":")[0];
                if(ipReal.startsWith("/")) ipReal = ipReal.substring(1);

                System.out.println(TAG + "❤ Heartbeat de " + uuid + " (" + ipReal + ":" + puertoListen + ")");

                // Guardar identidad definitiva
                Peer p = new Peer();
                p.setId(UUID.fromString(uuid));
                p.setIp(ipReal);
                p.setEstado(Peer.Estado.ONLINE);
                repositorio.guardarOActualizarPeer(p, ipReal + ":" + puertoListen);

                return new DTOResponse("heartbeat", "success", "ACK", null);
            } catch (Exception e) { return null; }
        });
    }

    private void guardarPeersEnBD(JsonArray listaJson) {
        String miHost = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();
        for (JsonElement elem : listaJson) {
            try {
                JsonObject p = elem.getAsJsonObject();
                String ip = p.get("ip").getAsString();
                int puerto = p.get("puerto").getAsInt();
                // No guardarnos a nosotros mismos
                if (ip.equals(miHost) && puerto == miPuerto) continue;

                String idStr = p.has("id") ? p.get("id").getAsString() : UUID.randomUUID().toString();
                Peer peer = new Peer();
                peer.setId(UUID.fromString(idStr));
                peer.setIp(ip);
                peer.setEstado(Peer.Estado.ONLINE);
                repositorio.guardarOActualizarPeer(peer, ip + ":" + puerto);
            } catch (Exception e) {}
        }
    }

    private void enviarHeartbeatsATodos() {
        String miIp = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();
        Peer yo = repositorio.obtenerPorSocketInfo(miIp + ":" + miPuerto);
        if (yo == null) return;

        JsonObject payload = new JsonObject();
        payload.addProperty("uuid", yo.getId().toString());
        payload.addProperty("listenPort", miPuerto);
        DTORequest req = new DTORequest("heartbeat", payload);
        String jsonReq = gson.toJson(req);

        List<PeerRepositorio.PeerInfo> conocidos = repositorio.listarPeersInfo();
        for (PeerRepositorio.PeerInfo destino : conocidos) {
            if (destino.ip.equals(miIp) && destino.puerto == miPuerto) continue;

            System.out.println(TAG + "Conectando Heartbeat -> " + destino.ip + ":" + destino.puerto);
            gestorConexiones.conectarAPeer(destino.ip, destino.puerto);

            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    String targetId = destino.ip + ":" + destino.puerto;
                    gestorConexiones.obtenerDetallesPeers().stream()
                            .filter(p -> p.getId().equals(targetId))
                            .findFirst()
                            .ifPresent(p -> gestorConexiones.enviarMensaje(p, jsonReq));
                } catch (Exception e) {}
            }).start();
        }
    }

    @Override
    public void iniciar() {
        System.out.println(TAG + "Iniciando secuencia de arranque...");
        String miIp = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();
        String miSocketInfo = miIp + ":" + miPuerto;

        // Gestión de Identidad Local
        if(repositorio.obtenerPorSocketInfo(miSocketInfo) == null) {
            Peer nuevo = new Peer(); nuevo.setIp(miIp); nuevo.setEstado(Peer.Estado.ONLINE);
            repositorio.guardarOActualizarPeer(nuevo, miSocketInfo);
            System.out.println(TAG + "Nueva identidad generada.");
        } else {
            System.out.println(TAG + "Identidad recuperada.");
        }

        new Thread(() -> gestorConexiones.iniciarServidor(miPuerto)).start();

        String seedHost = config.getPeerInicialHost();
        int seedPort = config.getPeerInicialPuerto();

        if (seedHost != null && seedPort > 0) {
            System.out.println(TAG + "Fase 1: Sync con Semilla " + seedHost + ":" + seedPort);
            solicitarIngreso(seedHost, seedPort, miIp, miPuerto);
        } else {
            System.out.println(TAG + "Modo Génesis (Esperando conexiones).");
        }
    }

    private void solicitarIngreso(String host, int port, String miIp, int miPuerto) {
        try {
            // 1. Conectar
            gestorConexiones.conectarAPeer(host, port);

            // 2. Payload de presentación
            JsonObject payload = new JsonObject();
            payload.addProperty("ip", miIp);
            payload.addProperty("puerto", miPuerto);
            DTORequest req = new DTORequest("añadirPeer", payload);

            String seedId = host + ":" + port;

            // 3. Enviar
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    gestorConexiones.obtenerDetallesPeers().stream()
                            .filter(p -> p.getId().equals(seedId))
                            .findFirst()
                            .ifPresent(p -> {
                                System.out.println(TAG + "Enviando solicitud PRESENTACIÓN (añadirPeer)...");
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