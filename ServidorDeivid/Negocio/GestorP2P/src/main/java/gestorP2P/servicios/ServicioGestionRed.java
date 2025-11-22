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
import java.util.stream.Collectors;

public class ServicioGestionRed implements IServicioP2P {

    // --- COLORES ANSI PARA LOGS CHEBRES ---
    private static final String RESET = "\u001B[0m";
    private static final String ROJO = "\u001B[31m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String AZUL = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String TAG = CYAN + "[GestionRed] " + RESET;

    private IGestorConexiones gestorConexiones;
    private final Configuracion config;
    private final PeerRepositorio repositorio;
    private final Gson gson;

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
        // 1. ROL SERVIDOR: ALGUIEN SE QUIERE UNIR A NOSOTROS
        // ================================================================
        router.registrarAccion("añadirPeer", (datosJson, origenId) -> {
            try {
                if (datosJson == null || !datosJson.isJsonObject()) return null;

                JsonObject obj = datosJson.getAsJsonObject();
                String ip = obj.get("ip").getAsString();
                int puerto = obj.get("puerto").getAsInt();
                String socketInfo = ip + ":" + puerto;

                System.out.println(TAG + "Solicitud de unión recibida de: " + AMARILLO + socketInfo + RESET);

                // A. Conectamos de vuelta (bidireccional)
                gestorConexiones.conectarAPeer(ip, puerto);

                // B. Guardamos en BD
                Peer nuevo = new Peer();
                nuevo.setIp(ip);
                nuevo.setEstado(Peer.Estado.ONLINE);
                // Mantener ID si ya existía
                Peer existente = repositorio.obtenerPorSocketInfo(socketInfo);
                if (existente != null) nuevo.setId(existente.getId());

                repositorio.guardarOActualizarPeer(nuevo, socketInfo);
                System.out.println(TAG + VERDE + "Nuevo nodo registrado y persistido: " + socketInfo + RESET);

                // C. Devolvemos NUESTRA lista de conocidos (Sincronización)
                List<PeerRepositorio.PeerInfo> peersDb = repositorio.listarPeersInfo();
                List<DTOPeerDetails> listaEnviar = peersDb.stream()
                        .map(p -> new DTOPeerDetails(p.id.toString(), p.ip, p.puerto, p.estado.name(), p.fechaCreacion.toString()))
                        .collect(Collectors.toList());

                System.out.println(TAG + "Enviando lista de sincronización con " + MAGENTA + listaEnviar.size() + " peers" + RESET + " a " + socketInfo);
                return new DTOResponse("añadirPeer", "success", "Bienvenido", gson.toJsonTree(listaEnviar));

            } catch (Exception e) {
                System.err.println(TAG + ROJO + "Error procesando solicitud de unión: " + e.getMessage() + RESET);
                e.printStackTrace();
                return new DTOResponse("añadirPeer", "error", e.getMessage(), null);
            }
        });

        // ================================================================
        // 2. ROL CLIENTE: LA SEMILLA NOS RESPONDIÓ (AQUÍ PROCESAS LA RESPUESTA)
        // ================================================================
        router.registrarManejadorRespuesta("añadirPeer", (response) -> {
            if (!response.fueExitoso()) {
                System.err.println(TAG + ROJO + "La semilla rechazó la conexión: " + response.getMessage() + RESET);
                return;
            }

            JsonElement data = response.getData();
            if (data != null && data.isJsonArray()) {
                int cantidad = data.getAsJsonArray().size();
                System.out.println(TAG + VERDE + "¡Conexión exitosa con Semilla! " + RESET + "Recibida lista de " + MAGENTA + cantidad + " peers" + RESET + ".");
                procesarListaPeersRecibida(data.getAsJsonArray());
            }
        });
    }

    private void procesarListaPeersRecibida(JsonArray listaJson) {
        String miHost = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();
        int nuevos = 0;

        for (JsonElement elem : listaJson) {
            try {
                JsonObject p = elem.getAsJsonObject();
                String ip = p.get("ip").getAsString();
                int puerto = p.get("puerto").getAsInt();

                // Ignorarnos a nosotros mismos
                if (ip.equals(miHost) && puerto == miPuerto) continue;

                String socketInfo = ip + ":" + puerto;
                String idStr = p.has("id") ? p.get("id").getAsString() : UUID.randomUUID().toString();

                // 1. Guardar en BD
                Peer peer = new Peer();
                peer.setId(UUID.fromString(idStr));
                peer.setIp(ip);
                peer.setEstado(Peer.Estado.ONLINE);

                boolean guardado = repositorio.guardarOActualizarPeer(peer, socketInfo);

                // 2. Conectar (El gestor filtra si ya está conectado)
                if (guardado) {
                    System.out.println(TAG + "Descubierto peer en la red: " + AMARILLO + socketInfo + RESET + ". Conectando...");
                    gestorConexiones.conectarAPeer(ip, puerto);
                    nuevos++;
                }

            } catch (Exception e) {
                System.err.println(TAG + ROJO + "Error procesando peer de lista: " + e.getMessage() + RESET);
            }
        }
        if (nuevos > 0) {
            System.out.println(TAG + VERDE + "Sincronización completada. " + nuevos + " nuevas conexiones lanzadas." + RESET);
        }
    }

    @Override
    public void iniciar() {
        System.out.println(TAG + "Iniciando secuencia de arranque...");

        String miIp = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();
        String miSocketInfo = miIp + ":" + miPuerto;

        // Lógica de Génesis vs Joiner
        Peer miPeer = repositorio.obtenerPorSocketInfo(miSocketInfo);
        String seedHost = config.getPeerInicialHost();
        int seedPort = config.getPeerInicialPuerto();
        boolean haySemilla = (seedHost != null && seedPort > 0);

        if (miPeer == null) {
            // CREACIÓN DE IDENTIDAD
            miPeer = new Peer();
            miPeer.setIp(miIp);
            miPeer.setEstado(Peer.Estado.ONLINE);
            repositorio.guardarOActualizarPeer(miPeer, miSocketInfo);

            if (!haySemilla) {
                System.out.println(TAG + MAGENTA + "=== MODO GÉNESIS ===" + RESET);
                System.out.println(TAG + "No hay identidad previa ni semilla. Se ha creado una nueva Red P2P.");
            } else {
                System.out.println(TAG + AZUL + "=== MODO JOINER (Nuevo Nodo) ===" + RESET);
                System.out.println(TAG + "Identidad creada. Preparando unión a la red existente...");
            }
        } else {
            System.out.println(TAG + VERDE + "=== REINICIO DE NODO ===" + RESET);
            System.out.println(TAG + "Identidad recuperada: " + AMARILLO + miPeer.getId() + RESET);
        }

        // Levantar servidor
        System.out.println(TAG + "Levantando servidor local en " + AMARILLO + miPuerto + RESET + "...");
        new Thread(() -> gestorConexiones.iniciarServidor(miPuerto)).start();

        // Conectar a semilla si existe
        if (haySemilla) {
            System.out.println(TAG + "Semilla configurada: " + AMARILLO + seedHost + ":" + seedPort + RESET);
            intentarUnirseALaRed(seedHost, seedPort, miIp, miPuerto);
        } else {
            System.out.println(TAG + VERDE + "Nodo listo y escuchando (Aislado/Génesis)." + RESET);
        }
    }

    private void intentarUnirseALaRed(String host, int port, String miIp, int miPuerto) {
        System.out.println(TAG + "Iniciando protocolo de unión (Handshake)...");
        try {
            Thread.sleep(500); // Pequeña pausa técnica
            gestorConexiones.conectarAPeer(host, port);

            JsonObject payload = new JsonObject();
            payload.addProperty("ip", miIp);
            payload.addProperty("puerto", miPuerto);

            DTORequest req = new DTORequest("añadirPeer", payload);

            String seedId = host + ":" + port;

            new Thread(() -> {
                try {
                    // Esperamos a que el socket TCP esté listo
                    Thread.sleep(1000);
                    gestorConexiones.obtenerDetallesPeers().stream()
                            .filter(p -> p.getId().equals(seedId))
                            .findFirst()
                            .ifPresentOrElse(
                                    p -> {
                                        System.out.println(TAG + "Enviando solicitud " + AZUL + "'añadirPeer'" + RESET + " a semilla " + AMARILLO + seedId + RESET);
                                        gestorConexiones.enviarMensaje(p, gson.toJson(req));
                                    },
                                    () -> System.err.println(TAG + ROJO + "No se pudo encontrar el peer semilla en el pool tras conectar." + RESET)
                            );
                } catch (InterruptedException e) {}
            }).start();

        } catch (Exception e) {
            System.err.println(TAG + ROJO + "Error crítico al intentar unirse: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }

    @Override
    public void detener() {}
}