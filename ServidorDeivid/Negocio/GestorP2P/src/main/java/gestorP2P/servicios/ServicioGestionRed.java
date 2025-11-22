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
import observador.IObservador;
import observador.ISujeto;
import repositorio.p2p.PeerRepositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class ServicioGestionRed implements IServicioP2P, ISujeto {

    // --- COLORES LOGS ---
    private static final String TAG = "\u001B[36m[GestionRed] \u001B[0m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String VERDE = "\u001B[32m";
    private static final String RESET = "\u001B[0m";

    private IGestorConexiones gestorConexiones;
    private final Configuracion config;
    private final PeerRepositorio repositorio;
    private final Gson gson;
    private Timer timerHeartbeat;

    // --- OBSERVADOR ---
    private final List<IObservador> observadores;

    public ServicioGestionRed() {
        this.config = Configuracion.getInstance();
        this.repositorio = new PeerRepositorio();
        this.gson = new Gson();
        this.observadores = new ArrayList<>();
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
            // Solo respondemos OK. El cliente debe pedir 'sincronizar' después.
            return new DTOResponse("añadirPeer", "success", "OK. Solicita sync.", null);
        });

        // CLIENTE: Si me aceptaron, pido sincronizar la lista
        router.registrarManejadorRespuesta("añadirPeer", (response) -> {
            if (response.fueExitoso()) {
                System.out.println(TAG + "Admitido. Solicitando sincronización...");
                notificarObservadores("ESTADO", "Admitido en la red. Sincronizando...");

                // Enviamos petición 'sincronizar' a la semilla (broadcast porque es la única conexión)
                DTORequest req = new DTORequest("sincronizar", null);
                gestorConexiones.broadcast(gson.toJson(req));
            }
        });


        // =========================================================================
        // RUTA 2: SINCRONIZAR (Fase 1.b - Intercambio y Desconexión Bilateral)
        // =========================================================================

        // --- ROL SERVIDOR (La Semilla) ---
        router.registrarAccion("sincronizar", (datosJson, origenId) -> {
            System.out.println(TAG + "Petición 'sincronizar' de " + origenId);

            // 1. Guardar IP temporal del solicitante
            try {
                if (datosJson != null && datosJson.isJsonObject()) {
                    JsonObject obj = datosJson.getAsJsonObject();
                    String ip = obj.get("ip").getAsString();
                    int puerto = obj.get("puerto").getAsInt();
                    Peer p = new Peer(); p.setIp(ip); p.setEstado(Peer.Estado.ONLINE);
                    repositorio.guardarOActualizarPeer(p, ip + ":" + puerto);
                }
            } catch (Exception e) {}

            // 2. Preparar Lista
            List<PeerRepositorio.PeerInfo> peersDb = repositorio.listarPeersInfo();
            List<DTOPeerDetails> lista = peersDb.stream()
                    .map(p -> new DTOPeerDetails(p.id.toString(), p.ip, p.puerto, "ONLINE", ""))
                    .collect(Collectors.toList());

            // 3. AUTODESTRUCCIÓN (Servidor cierra socket tras responder)
            new Thread(() -> {
                try { Thread.sleep(500); } catch (Exception e) {}
                System.out.println(TAG + AMARILLO + "Cerrando socket efímero cliente: " + origenId + RESET);
                gestorConexiones.obtenerDetallesPeers().stream()
                        .filter(p -> p.getId().equals(origenId))
                        .findFirst().ifPresent(p -> gestorConexiones.desconectar(p));
            }).start();

            return new DTOResponse("sincronizar", "success", "Lista entregada.", gson.toJsonTree(lista));
        });

        // --- ROL CLIENTE (El Nuevo) ---
        router.registrarManejadorRespuesta("sincronizar", (response) -> {
            if (response.fueExitoso() && response.getData() != null) {
                System.out.println(TAG + VERDE + "Sync exitoso. Procesando lista..." + RESET);
                notificarObservadores("SYNC_COMPLETO", "Lista de peers recibida.");

                // A. Guardar
                JsonArray lista = response.getData().getAsJsonArray();
                guardarPeersEnBD(lista);

                // B. SPLASH (Cliente cierra socket con semilla)
                System.out.println(TAG + AMARILLO + ">>> SPLASH! Cortando conexión... <<<" + RESET);
                List<DTOPeerDetails> activos = gestorConexiones.obtenerDetallesPeers();
                for(DTOPeerDetails p : activos) gestorConexiones.desconectar(p);

                // C. FASE 2: Heartbeats
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (Exception e) {}
                    System.out.println(TAG + "Iniciando Fase 2: Heartbeats...");
                    enviarHeartbeatsATodos();
                }).start();
            }
        });


        // =========================================================================
        // RUTA 3: HEARTBEAT (Fase 2 - Identidad Real)
        // =========================================================================
        router.registrarAccion("heartbeat", (datosJson, origenId) -> {
            try {
                if (datosJson == null) return null;
                JsonObject obj = datosJson.getAsJsonObject();
                String uuid = obj.get("uuid").getAsString();
                int puertoListen = obj.get("listenPort").getAsInt();

                // IP Real del socket físico
                String ipReal = origenId.split(":")[0];
                if(ipReal.startsWith("/")) ipReal = ipReal.substring(1);

                System.out.println(TAG + "❤ Heartbeat de " + uuid + " (" + ipReal + ":" + puertoListen + ")");

                Peer p = new Peer();
                p.setId(UUID.fromString(uuid));
                p.setIp(ipReal);
                p.setEstado(Peer.Estado.ONLINE);
                repositorio.guardarOActualizarPeer(p, ipReal + ":" + puertoListen);

                // Actualizar la vista pública del puerto
                gestorConexiones.actualizarPuertoServidor(origenId, puertoListen);

                notificarObservadores("PEER_CONECTADO", ipReal + ":" + puertoListen);

                return new DTOResponse("heartbeat", "success", "ACK", null);
            } catch (Exception e) { return null; }
        });
    }

    private void guardarPeersEnBD(JsonArray listaJson) {
        String miHost = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();
        int nuevos = 0;
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
                if(repositorio.guardarOActualizarPeer(peer, ip + ":" + puerto)) nuevos++;
            } catch (Exception e) {}
        }
        if (nuevos > 0) notificarObservadores("PEERS_NUEVOS", nuevos);
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
        System.out.println(TAG + "Iniciando...");
        String miIp = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();
        String miSocketInfo = miIp + ":" + miPuerto;

        if(repositorio.obtenerPorSocketInfo(miSocketInfo) == null) {
            Peer nuevo = new Peer(); nuevo.setIp(miIp); nuevo.setEstado(Peer.Estado.ONLINE);
            repositorio.guardarOActualizarPeer(nuevo, miSocketInfo);
            System.out.println(TAG + "Identidad local creada.");
        }

        new Thread(() -> gestorConexiones.iniciarServidor(miPuerto)).start();
        notificarObservadores("RED_INICIADA", "Puerto: " + miPuerto);

        String seedHost = config.getPeerInicialHost();
        int seedPort = config.getPeerInicialPuerto();

        if (seedHost != null && seedPort > 0) {
            System.out.println(TAG + "Fase 1: Sync con Semilla " + seedHost + ":" + seedPort);
            solicitarSync(seedHost, seedPort, miIp, miPuerto);
        } else {
            System.out.println(TAG + "Modo Génesis.");
        }

        // Tarea Mantenimiento
        if (timerHeartbeat == null) {
            timerHeartbeat = new Timer();
            timerHeartbeat.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() { enviarHeartbeatsATodos(); }
            }, 60000, 60000);
        }
    }

    private void solicitarSync(String host, int port, String miIp, int miPuerto) {
        try {
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
                            .ifPresent(p -> gestorConexiones.enviarMensaje(p, gson.toJson(req)));
                } catch (Exception e) {}
            }).start();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void detener() {
        if (timerHeartbeat != null) timerHeartbeat.cancel();
    }

    // --- IMPLEMENTACIÓN DEL SUJETO (OBSERVER) ---

    @Override
    public void registrarObservador(IObservador observador) {
        observadores.add(observador);
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador obs : observadores) {
            obs.actualizar(tipoDeDato, datos);
        }
    }
}