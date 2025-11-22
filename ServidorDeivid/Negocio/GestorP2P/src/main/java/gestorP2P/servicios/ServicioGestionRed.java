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

    private static final String TAG = "\u001B[36m[GestionRed] \u001B[0m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String VERDE = "\u001B[32m";
    private static final String ROJO = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    private IGestorConexiones gestorConexiones;
    private final Configuracion config;
    private final PeerRepositorio repositorio;
    private final Gson gson;
    private Timer timerMantenimiento;

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

        // -------------------------------------------------------------------------
        // RUTA 1: PING (Solicitud de estado)
        // El otro peer nos pregunta "¿Estás vivo?" y nos da su info de retorno.
        // -------------------------------------------------------------------------
        router.registrarAccion("ping", (datosJson, origenId) -> {
            try {
                if (datosJson == null) return null;
                JsonObject obj = datosJson.getAsJsonObject();

                // Info de quien nos hace ping (para saber cómo devolverle la llamada si hiciera falta)
                String ipRemota = origenId.split(":")[0];
                if(ipRemota.startsWith("/")) ipRemota = ipRemota.substring(1);

                // Si el ping trae info de escucha del remitente, actualizamos nuestra BD
                if (obj.has("listenPort") && obj.has("uuid")) {
                    int portRemoto = obj.get("listenPort").getAsInt();
                    String uuidRemoto = obj.get("uuid").getAsString();

                    Peer p = new Peer();
                    p.setId(UUID.fromString(uuidRemoto));
                    p.setIp(ipRemota);
                    p.setEstado(Peer.Estado.ONLINE);
                    repositorio.guardarOActualizarPeer(p, ipRemota + ":" + portRemoto);

                    // Actualizamos visualmente el puerto
                    gestorConexiones.actualizarPuertoServidor(origenId, portRemoto);
                }

                // PREPARAR RESPUESTA (PONG)
                String miIp = config.getPeerHost();
                int miPuerto = config.getPeerPuerto();
                Peer yo = repositorio.obtenerPorSocketInfo(miIp + ":" + miPuerto);
                String miUuid = (yo != null) ? yo.getId().toString() : UUID.randomUUID().toString();

                JsonObject pongData = new JsonObject();
                pongData.addProperty("uuid", miUuid);
                pongData.addProperty("listenPort", miPuerto);
                pongData.addProperty("mensaje", "pong");

                // System.out.println(TAG + "Respondiendo PONG a " + origenId);
                return new DTOResponse("ping", "success", "PONG", pongData);

            } catch (Exception e) {
                return new DTOResponse("ping", "error", "Datos corruptos", null);
            }
        });

        // -------------------------------------------------------------------------
        // RUTA 2: PONG (Respuesta recibida)
        // El peer nos respondió "Sí, estoy vivo".
        // -------------------------------------------------------------------------
        router.registrarManejadorRespuesta("ping", (response) -> {
            if (response.fueExitoso() && response.getData() != null) {
                try {
                    JsonObject data = response.getData().getAsJsonObject();
                    String uuid = data.get("uuid").getAsString();
                    int puertoListen = data.get("listenPort").getAsInt();

                    // No tenemos la IP en el body de respuesta usualmente, pero podemos inferirla
                    // del contexto si tuviéramos acceso al origenId aquí.
                    // Como limitante del router actual, asumimos que actualizamos basado en lo que sabemos.
                    // OJO: Para ser precisos, necesitamos la IP.
                    // Truco: El router procesa mensajes de una conexión.
                    // Si no tenemos la IP aquí, solo podemos actualizar por UUID si ya existe,
                    // o confiar en que la IP no cambió.

                    // Asumiremos actualización de estado "VIVO"
                    System.out.println(TAG + VERDE + "¡PONG Recibido! Peer " + uuid + " está ONLINE." + RESET);

                    // Notificar UI
                    notificarObservadores("PEER_ONLINE", "UUID: " + uuid);

                    // Actualizar en BD a ONLINE (Búsqueda por UUID sería ideal, pero usamos socketInfo conocido)
                    // Como no tenemos la IP aquí fácil (limitación de interfaz), notificamos que "alguien" respondió.

                    // MEJORA: Si podemos, actualizamos el peer en la BD
                    // (Requiere buscar por UUID en la BD)
                    // repositorio.marcarComoOnline(UUID.fromString(uuid));

                } catch (Exception e) {
                    System.err.println(TAG + ROJO + "Error procesando PONG: " + e.getMessage() + RESET);
                }
            }
        });

        // --- RUTAS DE INICIO (Añadir/Sync) SE MANTIENEN IGUAL ---
        router.registrarAccion("añadirPeer", (datosJson, origenId) -> {
            return new DTOResponse("añadirPeer", "success", "OK", null);
        });

        router.registrarManejadorRespuesta("añadirPeer", (response) -> {
            if (response.fueExitoso()) {
                DTORequest req = new DTORequest("sincronizar", null);
                gestorConexiones.broadcast(gson.toJson(req));
            }
        });

        router.registrarAccion("sincronizar", (datosJson, origenId) -> {
            try {
                if(datosJson!=null && datosJson.isJsonObject()){
                    JsonObject o = datosJson.getAsJsonObject();
                    repositorio.guardarOActualizarPeer(new Peer(null, o.get("ip").getAsString(), null, Peer.Estado.ONLINE, null),
                            o.get("ip").getAsString() + ":" + o.get("puerto").getAsInt());
                }
            } catch(Exception e){}

            List<DTOPeerDetails> lista = repositorio.listarPeersInfo().stream()
                    .map(p -> new DTOPeerDetails(p.id.toString(), p.ip, p.puerto, "ONLINE", ""))
                    .collect(Collectors.toList());

            // Auto-disconnect server side
            new Thread(() -> {
                try { Thread.sleep(500); } catch(Exception e){}
                gestorConexiones.obtenerDetallesPeers().stream()
                        .filter(p -> p.getId().equals(origenId))
                        .findFirst().ifPresent(p -> gestorConexiones.desconectar(p));
            }).start();

            return new DTOResponse("sincronizar", "success", "Lista", gson.toJsonTree(lista));
        });

        router.registrarManejadorRespuesta("sincronizar", (response) -> {
            if (response.fueExitoso() && response.getData() != null) {
                JsonArray lista = response.getData().getAsJsonArray();
                guardarPeersEnBD(lista);

                // Desconectar todo para iniciar limpio
                gestorConexiones.obtenerDetallesPeers().forEach(p -> gestorConexiones.desconectar(p));

                // Iniciar mantenimiento
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (Exception e) {}
                    verificarEstadoPeers(); // Primer chequeo
                }).start();
            }
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
                // Al recibir la lista, no sabemos si están online AHORA, asumimos offline hasta el ping
                peer.setEstado(Peer.Estado.OFFLINE);
                if(repositorio.guardarOActualizarPeer(peer, ip + ":" + puerto)) nuevos++;
            } catch (Exception e) {}
        }
        if(nuevos > 0) notificarObservadores("PEERS_NUEVOS", nuevos);
    }

    /**
     * Tarea: Verificar quién está vivo.
     * 1. Marcar todos como sospechosos (o usar timeout).
     * 2. Enviar Ping.
     * 3. Si responden (PONG), se marcan ONLINE en el handler.
     */
    private void verificarEstadoPeers() {
        String miIp = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();

        // Datos propios para el Ping
        Peer yo = repositorio.obtenerPorSocketInfo(miIp + ":" + miPuerto);
        if (yo == null) return;

        JsonObject payload = new JsonObject();
        payload.addProperty("uuid", yo.getId().toString());
        payload.addProperty("listenPort", miPuerto);
        DTORequest reqPing = new DTORequest("ping", payload);
        String jsonPing = gson.toJson(reqPing);

        List<PeerRepositorio.PeerInfo> conocidos = repositorio.listarPeersInfo();

        System.out.println(TAG + "Ejecutando ronda de PING a " + conocidos.size() + " candidatos...");

        for (PeerRepositorio.PeerInfo destino : conocidos) {
            if (destino.ip.equals(miIp) && destino.puerto == miPuerto) continue;

            // Intentamos conectar y pingear
            // Si falla la conexión, nunca recibiremos el Pong, y se quedará en su estado actual (o podemos marcar offline aqui)

            gestorConexiones.conectarAPeer(destino.ip, destino.puerto);

            new Thread(() -> {
                try {
                    Thread.sleep(1500); // Esperar conexión
                    String targetId = destino.ip + ":" + destino.puerto;

                    // Buscar si logramos conectar
                    DTOPeerDetails conexionActiva = gestorConexiones.obtenerDetallesPeers().stream()
                            .filter(p -> p.getId().equals(targetId))
                            .findFirst().orElse(null);

                    if (conexionActiva != null) {
                        System.out.println(TAG + "Enviando PING a " + targetId);
                        gestorConexiones.enviarMensaje(conexionActiva, jsonPing);

                        // Opcional: Si queremos ser estrictos, marcamos OFFLINE aquí y esperamos que el PONG lo pase a ONLINE
                        // Pero puede causar parpadeo en la UI.
                    } else {
                        // No pudimos conectar -> Definitivamente OFFLINE
                        System.out.println(TAG + ROJO + "Fallo conexión con " + targetId + " -> OFFLINE" + RESET);
                        Peer pOffline = new Peer();
                        pOffline.setId(destino.id);
                        pOffline.setIp(destino.ip);
                        pOffline.setEstado(Peer.Estado.OFFLINE);
                        repositorio.guardarOActualizarPeer(pOffline, targetId);
                        notificarObservadores("PEER_OFFLINE", targetId);
                    }
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
        }

        new Thread(() -> gestorConexiones.iniciarServidor(miPuerto)).start();
        notificarObservadores("RED_INICIADA", "Puerto " + miPuerto);

        String seedHost = config.getPeerInicialHost();
        int seedPort = config.getPeerInicialPuerto();

        if (seedHost != null && seedPort > 0) {
            solicitarSync(seedHost, seedPort, miIp, miPuerto);
        }

        // Tarea periódica de verificación
        if (timerMantenimiento == null) {
            timerMantenimiento = new Timer();
            timerMantenimiento.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() { verificarEstadoPeers(); }
            }, 30000, 30000); // Cada 30s
        }
    }

    private void solicitarSync(String host, int port, String miIp, int miPuerto) {
        try {
            gestorConexiones.conectarAPeer(host, port);
            JsonObject pl = new JsonObject(); pl.addProperty("ip", miIp); pl.addProperty("puerto", miPuerto);
            DTORequest req = new DTORequest("añadirPeer", pl);

            String seedId = host + ":" + port;
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    gestorConexiones.obtenerDetallesPeers().stream()
                            .filter(p -> p.getId().equals(seedId))
                            .findFirst().ifPresent(p -> gestorConexiones.enviarMensaje(p, gson.toJson(req)));
                } catch (Exception e) {}
            }).start();
        } catch (Exception e) {}
    }

    @Override
    public void detener() {
        if (timerMantenimiento != null) timerMantenimiento.cancel();
    }

    // --- OBSERVER ---
    @Override public void registrarObservador(IObservador o) { observadores.add(o); }
    @Override public void removerObservador(IObservador o) { observadores.remove(o); }
    @Override public void notificarObservadores(String t, Object d) { for (IObservador o : observadores) o.actualizar(t, d); }
}