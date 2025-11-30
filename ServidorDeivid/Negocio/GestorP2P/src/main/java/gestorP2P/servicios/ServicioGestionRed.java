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
import java.util.concurrent.ConcurrentHashMap;
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

    // --- TRACKING DE PINGs PENDIENTES ---
    private final ConcurrentHashMap<String, Long> pingsPendientes = new ConcurrentHashMap<>();
    private static final long TIMEOUT_PING_MS = 5000; // 5 segundos

    public ServicioGestionRed() {
        this.config = Configuracion.getInstance();
        this.repositorio = new PeerRepositorio();
        this.gson = GsonUtil.crearGson();
        this.observadores = new ArrayList<>();
    }

    @Override
    public String getNombre() { return "ServicioGestionRed"; }

    /**
     * NUEVO: Método público para notificar cuando un peer se desconecta.
     * Llamado por el callback de GestorConexiones.
     */
    public void onPeerDesconectado(String peerId) {
        LoggerCentral.warn(TAG, ROJO + "🔴 Peer desconectado: " + peerId + " -> Actualizando BD a OFFLINE" + RESET);

        // Actualizar estado en BD a OFFLINE
        repositorio.actualizarEstado(peerId, Peer.Estado.OFFLINE);

        // Notificar a observadores (por si ServicioSync quiere reaccionar)
        notificarObservadores("PEER_OFFLINE", peerId);
    }

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

                    // Notificar que el peer está conectado
                    notificarObservadores("PEER_CONECTADO", uuidRemoto);
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
                    int puertoListen = data.get("listenPort").getAsInt();

                    LoggerCentral.info(TAG, VERDE + "✓ PONG recibido de " + uuid + RESET);

                    // Buscar en la BD por UUID para obtener la IP
                    Peer peerRemoto = repositorio.obtenerPorId(UUID.fromString(uuid));
                    if (peerRemoto != null) {
                        String targetId = peerRemoto.getIp() + ":" + puertoListen;

                        // MARCAR PONG RECIBIDO (remover de pendientes)
                        pingsPendientes.remove(targetId);

                        // Actualizar estado a ONLINE
                        repositorio.actualizarEstado(uuid, Peer.Estado.ONLINE);

                        LoggerCentral.debug(TAG, VERDE + "Peer " + uuid + " marcado como ONLINE" + RESET);
                        notificarObservadores("PEER_CONECTADO", uuid);
                    }

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
     * NUEVA VERSIÓN: Con tracking real de respuestas y timeout.
     */
    private void verificarEstadoPeers() {
        String miIp = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();

        Peer yo = repositorio.obtenerPorSocketInfo(miIp + ":" + miPuerto);
        if (yo == null) {
            LoggerCentral.warn(TAG, ROJO + "No se pudo verificar peers: Identidad local no encontrada" + RESET);
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("uuid", yo.getId().toString());
        payload.addProperty("listenPort", miPuerto);
        DTORequest reqPing = new DTORequest("ping", payload);
        String jsonPing = gson.toJson(reqPing);

        List<PeerRepositorio.PeerInfo> conocidos = repositorio.listarPeersInfo();
        LoggerCentral.debug(TAG, "Ejecutando ronda de PING a " + conocidos.size() + " peers conocidos.");

        for (PeerRepositorio.PeerInfo destino : conocidos) {
            // Saltar si soy yo mismo
            if (destino.ip.equals(miIp) && destino.puerto == miPuerto) continue;

            String targetId = destino.ip + ":" + destino.puerto;

            // Verificar si ya está conectado en el pool de conexiones
            DTOPeerDetails conexionExistente = gestorConexiones.obtenerDetallesPeers().stream()
                    .filter(p -> {
                        String pId = p.getId();
                        // Comparar con socket_info o con IP:puerto
                        return pId.equals(targetId) ||
                               (p.getIp() != null && p.getIp().equals(destino.ip) && p.getPuerto() == destino.puerto);
                    })
                    .findFirst().orElse(null);

            if (conexionExistente != null) {
                // Ya está conectado, enviar PING y registrar timestamp
                LoggerCentral.debug(TAG, "Enviando PING a peer conectado: " + targetId);

                // Registrar PING pendiente con timestamp actual
                pingsPendientes.put(targetId, System.currentTimeMillis());

                gestorConexiones.enviarMensaje(conexionExistente, jsonPing);

            } else {
                // No está conectado, intentar reconectar
                LoggerCentral.debug(TAG, AMARILLO + "Reconectando con " + targetId + RESET);
                gestorConexiones.conectarAPeer(destino.ip, destino.puerto);

                new Thread(() -> {
                    try {
                        Thread.sleep(2000); // Esperar handshake TCP

                        DTOPeerDetails conexionNueva = gestorConexiones.obtenerDetallesPeers().stream()
                                .filter(p -> p.getId().equals(targetId) ||
                                        (p.getIp() != null && p.getIp().equals(destino.ip) && p.getPuerto() == destino.puerto))
                                .findFirst().orElse(null);

                        if (conexionNueva != null) {
                            // Conexión exitosa -> Enviar PING
                            LoggerCentral.debug(TAG, VERDE + "Reconectado con " + targetId + " -> Enviando PING" + RESET);

                            // Registrar PING pendiente
                            pingsPendientes.put(targetId, System.currentTimeMillis());

                            gestorConexiones.enviarMensaje(conexionNueva, jsonPing);
                        } else {
                            // Conexión falló -> OFFLINE
                            LoggerCentral.warn(TAG, ROJO + "✗ Fallo conexión con " + targetId + " -> OFFLINE" + RESET);

                            // CORREGIDO: Actualizar por socketInfo (IP:puerto)
                            boolean actualizado = repositorio.actualizarEstado(targetId, Peer.Estado.OFFLINE);
                            if (actualizado) {
                                LoggerCentral.info(TAG, "Estado actualizado en BD: " + targetId + " -> OFFLINE");
                            } else {
                                LoggerCentral.error(TAG, "No se pudo actualizar estado de: " + targetId);
                            }

                            notificarObservadores("PEER_OFFLINE", destino.id.toString());
                        }
                    } catch (Exception e) {
                        LoggerCentral.error(TAG, "Error verificando peer: " + e.getMessage());
                    }
                }).start();
            }
        }

        // VERIFICACIÓN DE TIMEOUTS (después de enviar todos los PINGs)
        new Thread(() -> {
            try {
                Thread.sleep(TIMEOUT_PING_MS); // Esperar 5 segundos

                // Revisar cuáles PINGs siguen pendientes (no recibieron PONG)
                long ahora = System.currentTimeMillis();
                List<String> timedOut = new ArrayList<>();

                for (String targetId : pingsPendientes.keySet()) {
                    long timestampEnvio = pingsPendientes.get(targetId);
                    if (ahora - timestampEnvio >= TIMEOUT_PING_MS) {
                        timedOut.add(targetId);
                    }
                }

                // Marcar como OFFLINE los que no respondieron
                for (String targetId : timedOut) {
                    pingsPendientes.remove(targetId);

                    LoggerCentral.warn(TAG, ROJO + "✗ TIMEOUT: " + targetId + " no respondió PONG -> OFFLINE" + RESET);

                    // CORREGIDO: Actualizar por socketInfo (IP:puerto)
                    boolean actualizado = repositorio.actualizarEstado(targetId, Peer.Estado.OFFLINE);
                    if (actualizado) {
                        LoggerCentral.info(TAG, "Estado actualizado en BD: " + targetId + " -> OFFLINE");

                        // Buscar UUID para notificar a observadores
                        Peer peerOffline = repositorio.obtenerPorSocketInfo(targetId);
                        if (peerOffline != null) {
                            notificarObservadores("PEER_OFFLINE", peerOffline.getId().toString());
                        }
                    } else {
                        LoggerCentral.error(TAG, "No se pudo actualizar estado de: " + targetId);
                    }
                }

                if (timedOut.isEmpty()) {
                    LoggerCentral.info(TAG, VERDE + "✓ Todos los peers respondieron PONG" + RESET);
                }

            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error en verificación de timeouts: " + e.getMessage());
            }
        }).start();
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

        // NUEVO: Crear DTO del peer local para notificación visual
        DTOPeerDetails peerLocalDto = new DTOPeerDetails(
            miPeer.getId().toString(),
            miIp,
            miPuerto,
            "ONLINE",
            miSocketInfo
        );
        peerLocalDto.setPuertoServidor(miPuerto); // Marcar puerto real

        // Notificar con UUID (para ServicioSincronizacionDatos)
        notificarObservadores("PEER_CONECTADO", miPeer.getId().toString());

        // NUEVO: También notificar visualmente a la consola (GestorConexiones maneja esto)
        System.out.println(TAG + VERDE + "✓ Peer local ACTIVO: " + miPeer.getId() + " en " + miSocketInfo + RESET);

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
