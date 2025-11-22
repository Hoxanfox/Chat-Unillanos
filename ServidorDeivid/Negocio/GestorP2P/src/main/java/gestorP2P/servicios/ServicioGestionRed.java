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
    private static final String VERDE = "\u001B[32m";
    private static final String ROJO = "\u001B[31m";
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
        // RUTA 1: AÑADIR PEER (Fase 1.a - Presentación)
        // =========================================================================
        router.registrarAccion("añadirPeer", (datosJson, origenId) -> {
            // Solo respondemos OK para que el cliente sepa que puede pedir la lista
            return new DTOResponse("añadirPeer", "success", "OK. Solicita sync ahora.", null);
        });

        // CLIENTE: Si me aceptaron, pido sincronizar la lista
        router.registrarManejadorRespuesta("añadirPeer", (response) -> {
            if (response.fueExitoso()) {
                System.out.println(TAG + "Admitido. Solicitando sincronización...");
                // Enviamos petición de sincronización a la semilla (payload null es válido aquí)
                DTORequest req = new DTORequest("sincronizar", null);
                gestorConexiones.broadcast(gson.toJson(req));
            }
        });


        // =========================================================================
        // RUTA 2: SINCRONIZAR (Fase 1.b - Intercambio y Desconexión)
        // =========================================================================

        // --- ROL SERVIDOR (La Semilla) ---
        router.registrarAccion("sincronizar", (datosJson, origenId) -> {
            System.out.println(TAG + "Petición 'sincronizar' recibida de " + origenId);

            // CORRECCIÓN: No intentamos leer datosJson si es null (evita el error en logs)
            if (datosJson != null && datosJson.isJsonObject()) {
                try {
                    JsonObject obj = datosJson.getAsJsonObject();
                    if (obj.has("ip") && obj.has("puerto")) {
                        String ipReal = obj.get("ip").getAsString();
                        int puertoReal = obj.get("puerto").getAsInt();
                        Peer temporal = new Peer();
                        temporal.setIp(ipReal);
                        temporal.setEstado(Peer.Estado.ONLINE);
                        repositorio.guardarOActualizarPeer(temporal, ipReal + ":" + puertoReal);
                    }
                } catch (Exception e) { /* Ignorar datos malformados */ }
            }

            // Preparar Lista de la Red
            List<PeerRepositorio.PeerInfo> peersDb = repositorio.listarPeersInfo();
            List<DTOPeerDetails> lista = peersDb.stream()
                    .map(p -> new DTOPeerDetails(p.id.toString(), p.ip, p.puerto, "ONLINE", ""))
                    .collect(Collectors.toList());

            // AUTODESTRUCCIÓN (Lado Servidor): Cortar la llamada tras responder
            new Thread(() -> {
                try { Thread.sleep(500); } catch (Exception e) {} // Esperar a que salga el mensaje
                System.out.println(TAG + AMARILLO + "Cerrando socket efímero del cliente: " + origenId + RESET);

                // Buscamos la conexión activa por su ID de origen y la cerramos
                gestorConexiones.obtenerDetallesPeers().stream()
                        .filter(p -> p.getId().equals(origenId))
                        .findFirst()
                        .ifPresent(p -> gestorConexiones.desconectar(p));
            }).start();

            return new DTOResponse("sincronizar", "success", "Lista entregada. Bye.", gson.toJsonTree(lista));
        });

        // --- ROL CLIENTE (El Nuevo) ---
        router.registrarManejadorRespuesta("sincronizar", (response) -> {
            if (response.fueExitoso() && response.getData() != null) {
                System.out.println(TAG + VERDE + "Sync exitoso. Procesando lista..." + RESET);

                JsonArray lista = response.getData().getAsJsonArray();
                guardarPeersEnBD(lista);

                // SPLASH (Desconexión Lado Cliente)
                System.out.println(TAG + AMARILLO + ">>> SPLASH! Cortando conexión con semilla... <<<" + RESET);
                List<DTOPeerDetails> activos = gestorConexiones.obtenerDetallesPeers();
                for(DTOPeerDetails p : activos) {
                    gestorConexiones.desconectar(p);
                }

                // FASE 2: Iniciar Conexiones Reales
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (Exception e) {}
                    System.out.println(TAG + "Iniciando Fase 2: Heartbeats a puertos reales...");
                    enviarHeartbeatsATodos();
                }).start();
            }
        });


        // =========================================================================
        // RUTA 3: HEARTBEAT (Fase 2 - Vida Real)
        // =========================================================================
        router.registrarAccion("heartbeat", (datosJson, origenId) -> {
            try {
                if (datosJson == null) return null;
                JsonObject obj = datosJson.getAsJsonObject();
                String uuid = obj.get("uuid").getAsString();
                int puertoListen = obj.get("listenPort").getAsInt();

                // Obtener IP real del socket
                String ipReal = origenId.split(":")[0];
                if(ipReal.startsWith("/")) ipReal = ipReal.substring(1);

                System.out.println(TAG + "❤ Heartbeat de " + uuid + " (" + ipReal + ":" + puertoListen + ")");

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
        if (yo == null) return; // Seguridad

        JsonObject payload = new JsonObject();
        payload.addProperty("uuid", yo.getId().toString());
        payload.addProperty("listenPort", miPuerto);
        DTORequest req = new DTORequest("heartbeat", payload);
        String jsonReq = gson.toJson(req);

        // Usamos la BD para saber a quién conectar (Puertos Reales)
        List<PeerRepositorio.PeerInfo> conocidos = repositorio.listarPeersInfo();

        for (PeerRepositorio.PeerInfo destino : conocidos) {
            if (destino.ip.equals(miIp) && destino.puerto == miPuerto) continue;

            System.out.println(TAG + "Conectando Heartbeat -> " + destino.ip + ":" + destino.puerto);
            gestorConexiones.conectarAPeer(destino.ip, destino.puerto);

            // Enviar en hilo aparte para no bloquear si hay muchos
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Esperar handshake TCP
                    // El ID temporal en el gestor es "ip:puerto"
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

        if(repositorio.obtenerPorSocketInfo(miSocketInfo) == null) {
            Peer nuevo = new Peer(); nuevo.setIp(miIp); nuevo.setEstado(Peer.Estado.ONLINE);
            repositorio.guardarOActualizarPeer(nuevo, miSocketInfo);
            System.out.println(TAG + "Identidad local creada.");
        }

        new Thread(() -> gestorConexiones.iniciarServidor(miPuerto)).start();

        String seedHost = config.getPeerInicialHost();
        int seedPort = config.getPeerInicialPuerto();

        if (seedHost != null && seedPort > 0) {
            System.out.println(TAG + "Fase 1: Sync con Semilla " + seedHost + ":" + seedPort);
            solicitarSync(seedHost, seedPort, miIp, miPuerto);
        } else {
            System.out.println(TAG + "Modo Génesis. Esperando conexiones...");
        }
    }

    private void solicitarSync(String host, int port, String miIp, int miPuerto) {
        try {
            gestorConexiones.conectarAPeer(host, port);

            // Solo enviamos IP/Puerto en añadirPeer para presentarnos, sincronizar va vacío
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
                                System.out.println(TAG + "Enviando solicitud PRESENTACIÓN...");
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