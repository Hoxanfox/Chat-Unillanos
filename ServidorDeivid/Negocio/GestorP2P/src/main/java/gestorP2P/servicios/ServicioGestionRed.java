package gestorP2P.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import conexion.p2p.interfaces.IRouterMensajes;
import conexion.p2p.interfaces.IGestorConexiones;
import configuracion.Configuracion;
import dominio.p2p.Peer;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.p2p.DTOPeerDetails;
import gestorP2P.interfaces.IServicioP2P;
import gestorP2P.utils.GsonUtil;
import logger.LoggerCentral;
import observador.IObservador;
import observador.ISujeto;
import repositorio.p2p.PeerRepositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Collectors;

public class ServicioGestionRed implements IServicioP2P, ISujeto {

    private static final String TAG = "GestionRed";

    // --- COLORES ANSI PÚBLICOS (Paleta Global) ---
    public static final String RESET = "\u001B[0m";
    public static final String ROJO = "\u001B[31m";
    public static final String VERDE = "\u001B[32m";
    public static final String AMARILLO = "\u001B[33m";
    public static final String AZUL = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";;

    private IGestorConexiones gestorConexiones;
    private final Configuracion config;
    private final PeerRepositorio repositorio;
    private final Gson gson;
    private Timer timerMantenimiento;
    private final List<IObservador> observadores;

    public ServicioGestionRed() {
        this.config = Configuracion.getInstance();
        this.repositorio = new PeerRepositorio();
        this.gson = GsonUtil.crearGson();
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
            // Solo respondemos OK para que el cliente sepa que puede pedir la lista
            return new DTOResponse("añadirPeer", "success", "OK. Solicita sync.", null);
        });

        router.registrarManejadorRespuesta("añadirPeer", (response) -> {
            if (response.fueExitoso()) {
                LoggerCentral.info(TAG, "Admitido en la red. Solicitando sincronización...");
                notificarObservadores("ESTADO", "Admitido. Sincronizando...");

                // Enviamos petición 'sincronizar' a la semilla
                DTORequest req = new DTORequest("sincronizar", null);
                gestorConexiones.broadcast(gson.toJson(req));
            }
        });

        // =========================================================================
        // RUTA 2: SINCRONIZAR (Fase 1.b - Intercambio y Desconexión Bilateral)
        // =========================================================================

        // --- SERVIDOR (La Semilla) ---
        router.registrarAccion("sincronizar", (datosJson, origenId) -> {
            LoggerCentral.debug(TAG, "Petición 'sincronizar' de " + origenId);

            // Guardar IP temporal del solicitante si viene
            try {
                if (datosJson != null && datosJson.isJsonObject()) {
                    JsonObject obj = datosJson.getAsJsonObject();
                    String ip = obj.get("ip").getAsString();
                    int puerto = obj.get("puerto").getAsInt();
                    Peer p = new Peer(); p.setIp(ip); p.setEstado(Peer.Estado.ONLINE);
                    repositorio.guardarOActualizarPeer(p, ip + ":" + puerto);
                }
            } catch (Exception e) {}

            // Preparar Lista
            List<PeerRepositorio.PeerInfo> peersDb = repositorio.listarPeersInfo();
            List<DTOPeerDetails> lista = peersDb.stream()
                    .map(p -> new DTOPeerDetails(p.id.toString(), p.ip, p.puerto, "ONLINE", ""))
                    .collect(Collectors.toList());

            // AUTODESTRUCCIÓN (Servidor cierra socket tras responder)
            new Thread(() -> {
                try { Thread.sleep(500); } catch (Exception e) {}
                LoggerCentral.warn(TAG, "Cerrando socket efímero cliente: " + origenId);
                gestorConexiones.obtenerDetallesPeers().stream()
                        .filter(p -> p.getId().equals(origenId))
                        .findFirst().ifPresent(p -> gestorConexiones.desconectar(p));
            }).start();

            return new DTOResponse("sincronizar", "success", "Lista entregada.", gson.toJsonTree(lista));
        });

        // --- CLIENTE (El Nuevo) ---
        router.registrarManejadorRespuesta("sincronizar", (response) -> {
            if (response.fueExitoso() && response.getData() != null) {
                LoggerCentral.info(TAG, VERDE + "Sync exitoso." + RESET + " Procesando lista...");
                notificarObservadores("SYNC_COMPLETO", "Lista recibida");

                JsonArray lista = response.getData().getAsJsonArray();
                guardarPeersEnBD(lista);

                LoggerCentral.warn(TAG, ">>> SPLASH! Cortando conexión... <<<");
                List<DTOPeerDetails> activos = gestorConexiones.obtenerDetallesPeers();
                for(DTOPeerDetails p : activos) gestorConexiones.desconectar(p);

                // FASE 2: Iniciar Mantenimiento (Ping-Pong)
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (Exception e) {}
                    LoggerCentral.info(TAG, "Iniciando Fase 2: Verificación de estado...");
                    verificarEstadoPeers();
                }).start();
            }
        });

        // =========================================================================
        // RUTA 3: PING (Solicitud de estado activo)
        // =========================================================================
        router.registrarAccion("ping", (datosJson, origenId) -> {
            try {
                if (datosJson == null) return null;
                JsonObject obj = datosJson.getAsJsonObject();

                String ipRemota = origenId.split(":")[0];
                if(ipRemota.startsWith("/")) ipRemota = ipRemota.substring(1);

                // Actualizar datos del remitente
                if (obj.has("listenPort") && obj.has("uuid")) {
                    int portRemoto = obj.get("listenPort").getAsInt();
                    String uuidRemoto = obj.get("uuid").getAsString();

                    Peer p = new Peer();
                    p.setId(UUID.fromString(uuidRemoto));
                    p.setIp(ipRemota);
                    p.setEstado(Peer.Estado.ONLINE);
                    repositorio.guardarOActualizarPeer(p, ipRemota + ":" + portRemoto);

                    // Actualizar puerto visual en memoria
                    gestorConexiones.actualizarPuertoServidor(origenId, portRemoto);

                    LoggerCentral.info(TAG, "PING recibido de " + AMARILLO + uuidRemoto + RESET + " @ " + ipRemota + ":" + portRemoto);
                }

                // RESPONDER PONG con mis datos
                String miIp = config.getPeerHost();
                int miPuerto = config.getPeerPuerto();
                Peer yo = repositorio.obtenerPorSocketInfo(miIp + ":" + miPuerto);
                String miUuid = (yo != null) ? yo.getId().toString() : UUID.randomUUID().toString();

                JsonObject pongData = new JsonObject();
                pongData.addProperty("uuid", miUuid);
                pongData.addProperty("listenPort", miPuerto);
                pongData.addProperty("mensaje", "pong");

                return new DTOResponse("ping", "success", "PONG", pongData);

            } catch (Exception e) {
                return new DTOResponse("ping", "error", "Datos corruptos", null);
            }
        });

        // =========================================================================
        // RUTA 4: PONG (Respuesta al Ping)
        // =========================================================================
        router.registrarManejadorRespuesta("ping", (response) -> {
            if (response.fueExitoso() && response.getData() != null) {
                try {
                    JsonObject data = response.getData().getAsJsonObject();
                    String uuid = data.get("uuid").getAsString();
                    // int puertoListen = data.get("listenPort").getAsInt();

                    LoggerCentral.info(TAG, VERDE + "¡PONG! Peer " + uuid + " está ONLINE." + RESET);
                    notificarObservadores("PEER_ONLINE", uuid);

                    // Aquí podríamos actualizar BD a ONLINE si tuviéramos la IP segura,
                    // pero la lógica de 'verificarEstadoPeers' ya asume éxito si no falló la conexión.
                } catch (Exception e) {
                    LoggerCentral.error(TAG, "Error procesando PONG: " + e.getMessage());
                }
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
                // Asumimos offline hasta verificar con Ping
                peer.setEstado(Peer.Estado.OFFLINE);
                if(repositorio.guardarOActualizarPeer(peer, ip + ":" + puerto)) nuevos++;
            } catch (Exception e) {}
        }
        if(nuevos > 0) notificarObservadores("PEERS_NUEVOS", nuevos);
    }

    /**
     * Tarea Mantenimiento: Verificar quién está vivo mediante Ping-Pong.
     */
    private void verificarEstadoPeers() {
        String miIp = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();

        Peer yo = repositorio.obtenerPorSocketInfo(miIp + ":" + miPuerto);
        if (yo == null) return;

        JsonObject payload = new JsonObject();
        payload.addProperty("uuid", yo.getId().toString());
        payload.addProperty("listenPort", miPuerto);
        DTORequest reqPing = new DTORequest("ping", payload);
        String jsonPing = gson.toJson(reqPing);

        List<PeerRepositorio.PeerInfo> conocidos = repositorio.listarPeersInfo();
        LoggerCentral.debug(TAG, "Ejecutando ronda de PING a " + conocidos.size() + " peers conocidos.");

        for (PeerRepositorio.PeerInfo destino : conocidos) {
            if (destino.ip.equals(miIp) && destino.puerto == miPuerto) continue;

            // Conectamos proactivamente
            gestorConexiones.conectarAPeer(destino.ip, destino.puerto);

            new Thread(() -> {
                try {
                    Thread.sleep(1500); // Esperar handshake TCP
                    String targetId = destino.ip + ":" + destino.puerto;

                    DTOPeerDetails conexionActiva = gestorConexiones.obtenerDetallesPeers().stream()
                            .filter(p -> p.getId().equals(targetId))
                            .findFirst().orElse(null);

                    if (conexionActiva != null) {
                        // Conexión OK -> Enviar Ping
                        // LoggerCentral.debug(TAG, "Enviando PING a " + targetId);
                        gestorConexiones.enviarMensaje(conexionActiva, jsonPing);

                        // Actualizar BD a ONLINE (provisional, el Pong confirma la lógica)
                        Peer pOnline = new Peer();
                        pOnline.setId(destino.id);
                        pOnline.setIp(destino.ip);
                        pOnline.setEstado(Peer.Estado.ONLINE);
                        repositorio.guardarOActualizarPeer(pOnline, targetId);

                    } else {
                        // Conexión Fallida -> Marcar OFFLINE
                        LoggerCentral.warn(TAG, "Fallo conexión con " + targetId + " -> " + ROJO + "OFFLINE" + RESET);
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

    // --- CORRECCIÓN CRÍTICA EN INICIAR ---
    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Iniciando secuencia de arranque...");
        String miIp = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();
        String miSocketInfo = miIp + ":" + miPuerto;

        String seedHost = config.getPeerInicialHost();
        int seedPort = config.getPeerInicialPuerto();
        boolean haySemilla = (seedHost != null && seedPort > 0);

        // 1. VERIFICACIÓN DE IDENTIDAD LOCAL
        Peer miPeer = repositorio.obtenerPorSocketInfo(miSocketInfo);

        if (miPeer != null) {
            // CASO: REINICIO (Ya existía identidad)
            System.out.println(TAG + VERDE + ">>> Identidad Recuperada <<<" + RESET);
            System.out.println(TAG + "Soy: " + AZUL + miPeer.getId() + RESET + " en " + miSocketInfo);

            // Importante: Actualizar estado a ONLINE
            miPeer.setEstado(Peer.Estado.ONLINE);
            repositorio.guardarOActualizarPeer(miPeer, miSocketInfo);
        } else {
            // CASO: NUEVA INSTALACIÓN
            miPeer = new Peer();
            miPeer.setIp(miIp);
            miPeer.setEstado(Peer.Estado.ONLINE);
            repositorio.guardarOActualizarPeer(miPeer, miSocketInfo);

            if (haySemilla) {
                System.out.println(TAG + AZUL + ">>> MODO JOINER (Nuevo Nodo) <<<" + RESET);
            } else {
                System.out.println(TAG + MAGENTA + ">>> MODO GÉNESIS ACTIVADO <<<" + RESET);
            }
        }

        // 2. LEVANTAR SERVIDOR
        new Thread(() -> gestorConexiones.iniciarServidor(miPuerto)).start();
        notificarObservadores("RED_INICIADA", "Puerto " + miPuerto);

        // 2.5. NOTIFICAR QUE EL PEER LOCAL ESTÁ ACTIVO
        // Esto permite que ServicioSincronizacionDatos reconstruya sus árboles Merkle
        LoggerCentral.info(TAG, VERDE + "Peer local registrado. Notificando sistema..." + RESET);
        notificarObservadores("PEER_CONECTADO", miPeer.getId().toString());

        // 3. BOOTSTRAPPING (Lógica unificada)
        // Si hay semilla configurada, SIEMPRE intentamos conectar,
        // sin importar si somos nuevos o reiniciados.
        if (haySemilla) {
            LoggerCentral.info(TAG, "Conectando a semilla maestra: " + AMARILLO + seedHost + ":" + seedPort + RESET);
            solicitarSync(seedHost, seedPort, miIp, miPuerto);
        } else {
            LoggerCentral.info(TAG, VERDE + "Nodo Maestro listo. Esperando conexiones." + RESET);
        }

        // 4. MANTENIMIENTO
        if (timerMantenimiento == null) {
            timerMantenimiento = new Timer();
            timerMantenimiento.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() { verificarEstadoPeers(); }
            }, 30000, 30000);
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