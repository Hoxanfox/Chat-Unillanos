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

    // --- COLORES LOGS CHEBRES ---
    private static final String TAG = "\u001B[36m[GestionRed] \u001B[0m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String VERDE = "\u001B[32m";
    private static final String ROJO = "\u001B[31m";
    private static final String AZUL = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
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
            return new DTOResponse("añadirPeer", "success", "OK. Solicita sync.", null);
        });

        router.registrarManejadorRespuesta("añadirPeer", (response) -> {
            if (response.fueExitoso()) {
                System.out.println(TAG + "Admitido en la red. Solicitando sincronización...");
                notificarObservadores("ESTADO", "Sincronizando tablas...");
                DTORequest req = new DTORequest("sincronizar", null);
                gestorConexiones.broadcast(gson.toJson(req));
            }
        });

        // =========================================================================
        // RUTA 2: SINCRONIZAR (Fase 1.b - Intercambio y Desconexión)
        // =========================================================================

        // --- SERVIDOR ---
        router.registrarAccion("sincronizar", (datosJson, origenId) -> {
            System.out.println(TAG + "Petición SYNC de " + origenId);

            // Guardar IP temporal del visitante si viene en el body
            try {
                if (datosJson != null && datosJson.isJsonObject()) {
                    JsonObject obj = datosJson.getAsJsonObject();
                    if(obj.has("ip") && obj.has("puerto")){
                        String ip = obj.get("ip").getAsString();
                        int puerto = obj.get("puerto").getAsInt();
                        Peer p = new Peer(); p.setIp(ip); p.setEstado(Peer.Estado.ONLINE);
                        repositorio.guardarOActualizarPeer(p, ip + ":" + puerto);
                    }
                }
            } catch (Exception e) {}

            List<PeerRepositorio.PeerInfo> peersDb = repositorio.listarPeersInfo();
            List<DTOPeerDetails> lista = peersDb.stream()
                    .map(p -> new DTOPeerDetails(p.id.toString(), p.ip, p.puerto, "ONLINE", ""))
                    .collect(Collectors.toList());

            // AUTODESTRUCCIÓN SERVIDOR
            new Thread(() -> {
                try { Thread.sleep(500); } catch (Exception e) {}
                System.out.println(TAG + AMARILLO + "Cerrando socket SYNC cliente: " + origenId + RESET);
                gestorConexiones.obtenerDetallesPeers().stream()
                        .filter(p -> p.getId().equals(origenId))
                        .findFirst().ifPresent(p -> gestorConexiones.desconectar(p));
            }).start();

            return new DTOResponse("sincronizar", "success", "Lista entregada.", gson.toJsonTree(lista));
        });

        // --- CLIENTE ---
        router.registrarManejadorRespuesta("sincronizar", (response) -> {
            if (response.fueExitoso() && response.getData() != null) {
                System.out.println(TAG + VERDE + "Sync exitoso. Guardando datos..." + RESET);
                notificarObservadores("SYNC_COMPLETO", "Datos recibidos");

                JsonArray lista = response.getData().getAsJsonArray();
                guardarPeersEnBD(lista);

                // SPLASH (Desconexión Cliente)
                System.out.println(TAG + AMARILLO + ">>> SPLASH! Finalizando Fase 1 (Corte) <<<" + RESET);
                List<DTOPeerDetails> activos = gestorConexiones.obtenerDetallesPeers();
                for(DTOPeerDetails p : activos) gestorConexiones.desconectar(p);

                // FASE 2: Heartbeats
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (Exception e) {}
                    System.out.println(TAG + "Iniciando Fase 2: Conexiones Estables...");
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

                String ipReal = origenId.split(":")[0];
                if(ipReal.startsWith("/")) ipReal = ipReal.substring(1);

                System.out.println(TAG + MAGENTA + "❤ Heartbeat: " + uuid + " @ " + ipReal + ":" + puertoListen + RESET);

                Peer p = new Peer();
                p.setId(UUID.fromString(uuid));
                p.setIp(ipReal);
                p.setEstado(Peer.Estado.ONLINE);
                repositorio.guardarOActualizarPeer(p, ipReal + ":" + puertoListen);

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

            // Evitamos conectar si ya existe conexión activa (aunque sea con puerto efímero)
            // Verificamos solo por IP para simplificar
            boolean yaConectado = gestorConexiones.obtenerDetallesPeers().stream()
                    .anyMatch(p -> p.getIp().equals(destino.ip));

            if (!yaConectado) {
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
    }

    // --- AQUÍ ESTÁ LA LÓGICA DE INICIO QUE PEDISTE ---
    @Override
    public void iniciar() {
        System.out.println(TAG + "Iniciando comprobaciones de arranque...");

        String miIp = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();
        String miSocketInfo = miIp + ":" + miPuerto;

        String seedHost = config.getPeerInicialHost();
        int seedPort = config.getPeerInicialPuerto();
        boolean haySemilla = (seedHost != null && seedPort > 0);

        // 1. VERIFICACIÓN DE IDENTIDAD LOCAL (CRÍTICO)
        Peer miPeer = repositorio.obtenerPorSocketInfo(miSocketInfo);   

        if (miPeer != null) {
            // YA EXISTO: Solo me despierto
            System.out.println(TAG + VERDE + ">>> Identidad Recuperada <<<" + RESET);
            System.out.println(TAG + "Soy: " + AZUL + miPeer.getId() + RESET + " en " + miSocketInfo);
            miPeer.setEstado(Peer.Estado.ONLINE);
            repositorio.guardarOActualizarPeer(miPeer, miSocketInfo);
        } else {
            // NO EXISTO: Debo crearme
            miPeer = new Peer();
            miPeer.setIp(miIp);
            miPeer.setEstado(Peer.Estado.ONLINE);

            if (!haySemilla) {
                // NO HAY SEMILLA -> SOY EL PRIMERO (GÉNESIS)
                System.out.println(TAG + MAGENTA + ">>> MODO GÉNESIS ACTIVADO <<<" + RESET);
                System.out.println(TAG + "Creando nueva red P2P. UUID Generado: " + miPeer.getId());
            } else {
                // HAY SEMILLA -> SOY NUEVO (JOINER)
                System.out.println(TAG + AZUL + ">>> MODO JOINER (Nuevo Nodo) <<<" + RESET);
                System.out.println(TAG + "Generando identidad para unirse a la red. UUID: " + miPeer.getId());
            }
            // Persistir la nueva identidad
            repositorio.guardarOActualizarPeer(miPeer, miSocketInfo);
        }

        // 2. LEVANTAR SERVIDOR
        System.out.println(TAG + "Levantando servidor local en " + miPuerto + "...");
        new Thread(() -> gestorConexiones.iniciarServidor(miPuerto)).start();
        notificarObservadores("RED_INICIADA", "Puerto: " + miPuerto);

        // 3. BOOTSTRAPPING (Solo si hay semilla)
        if (haySemilla) {
            System.out.println(TAG + "Conectando a semilla maestra: " + AMARILLO + seedHost + ":" + seedPort + RESET);
            solicitarSync(seedHost, seedPort, miIp, miPuerto);
        } else {
            System.out.println(TAG + VERDE + "Nodo Maestro listo. Esperando súbditos." + RESET);
        }

        // 4. MANTENIMIENTO
        iniciarTareaMantenimiento();
    }

    private void iniciarTareaMantenimiento() {
        if (timerHeartbeat != null) return;
        timerHeartbeat = new Timer();
        timerHeartbeat.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // System.out.println(TAG + "Ejecutando ciclo de mantenimiento (Heartbeats)...");
                enviarHeartbeatsATodos();
            }
        }, 60000, 60000);
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

    // --- OBSERVER ---
    @Override
    public void registrarObservador(IObservador observador) { observadores.add(observador); }
    @Override
    public void removerObservador(IObservador observador) { observadores.remove(observador); }
    @Override
    public void notificarObservadores(String tipo, Object datos) {
        for (IObservador obs : observadores) obs.actualizar(tipo, datos);
    }
}