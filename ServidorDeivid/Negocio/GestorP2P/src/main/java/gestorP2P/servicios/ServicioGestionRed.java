package gestorP2P.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import comunicacion.IRouterMensajes;
import configuracion.Configuracion;
import conexion.IGestorConexiones;
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

                System.out.println("[GestionRed] Solicitud de unión recibida de: " + socketInfo);

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

                // C. Devolvemos NUESTRA lista de conocidos (Sincronización)
                List<PeerRepositorio.PeerInfo> peersDb = repositorio.listarPeersInfo();
                List<DTOPeerDetails> listaEnviar = peersDb.stream()
                        .map(p -> new DTOPeerDetails(p.id.toString(), p.ip, p.puerto, p.estado.name(), p.fechaCreacion.toString()))
                        .collect(Collectors.toList());

                return new DTOResponse("añadirPeer", "success", "Bienvenido", gson.toJsonTree(listaEnviar));

            } catch (Exception e) {
                e.printStackTrace();
                return new DTOResponse("añadirPeer", "error", e.getMessage(), null);
            }
        });

        // ================================================================
        // 2. ROL CLIENTE: LA SEMILLA NOS RESPONDIÓ (AQUÍ PROCESAS LA RESPUESTA)
        // ================================================================
        router.registrarManejadorRespuesta("añadirPeer", (response) -> {
            if (!response.fueExitoso()) {
                System.err.println("[GestionRed] La semilla rechazó la conexión: " + response.getMessage());
                return;
            }

            JsonElement data = response.getData();
            if (data != null && data.isJsonArray()) {
                System.out.println("[GestionRed] ¡Lista de peers recibida! Sincronizando...");
                procesarListaPeersRecibida(data.getAsJsonArray());
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

                // Ignorarnos a nosotros mismos
                if (ip.equals(miHost) && puerto == miPuerto) continue;

                String socketInfo = ip + ":" + puerto;
                String idStr = p.has("id") ? p.get("id").getAsString() : UUID.randomUUID().toString();

                // 1. Guardar en BD
                Peer peer = new Peer();
                peer.setId(UUID.fromString(idStr));
                peer.setIp(ip);
                peer.setEstado(Peer.Estado.ONLINE); // Asumimos online si nos lo acaban de pasar
                repositorio.guardarOActualizarPeer(peer, socketInfo);

                // 2. Conectar (El gestor filtra si ya está conectado)
                gestorConexiones.conectarAPeer(ip, puerto);

            } catch (Exception e) {
                System.err.println("[GestionRed] Error procesando peer de lista: " + e.getMessage());
            }
        }
    }

    @Override
    public void iniciar() {
        System.out.println("[ServicioGestionRed] Verificando identidad...");

        String miIp = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();
        String miSocketInfo = miIp + ":" + miPuerto;

        // Lógica de Génesis vs Joiner (Tu lógica original)
        Peer miPeer = repositorio.obtenerPorSocketInfo(miSocketInfo);
        String seedHost = config.getPeerInicialHost();
        int seedPort = config.getPeerInicialPuerto();
        boolean haySemilla = (seedHost != null && seedPort > 0);

        if (miPeer == null) {
            miPeer = new Peer();
            miPeer.setIp(miIp);
            miPeer.setEstado(Peer.Estado.ONLINE);
            repositorio.guardarOActualizarPeer(miPeer, miSocketInfo);
            System.out.println(haySemilla ? "[GestionRed] Identidad creada (Joiner)." : "[GestionRed] Identidad creada (Génesis).");
        } else {
            System.out.println("[GestionRed] Identidad recuperada: " + miPeer.getId());
        }

        // Levantar servidor
        new Thread(() -> gestorConexiones.iniciarServidor(miPuerto)).start();

        // Conectar a semilla si existe
        if (haySemilla) {
            intentarUnirseALaRed(seedHost, seedPort, miIp, miPuerto);
        }
    }

    private void intentarUnirseALaRed(String host, int port, String miIp, int miPuerto) {
        try {
            Thread.sleep(500);
            gestorConexiones.conectarAPeer(host, port);

            JsonObject payload = new JsonObject();
            payload.addProperty("ip", miIp);
            payload.addProperty("puerto", miPuerto);

            DTORequest req = new DTORequest("añadirPeer", payload);

            // IMPORTANTE: Necesitamos el DTO del destino para enviar
            // Como acabamos de llamar a conectar, el gestor ya debería tenerlo (o estar creándolo)
            // Hacemos un pequeño reintento simple para encontrarlo
            String seedId = host + ":" + port;

            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Esperar a que el handshake TCP termine
                    gestorConexiones.obtenerDetallesPeers().stream()
                            .filter(p -> p.getId().equals(seedId))
                            .findFirst()
                            .ifPresent(p -> {
                                System.out.println("[GestionRed] Enviando handshake a semilla...");
                                gestorConexiones.enviarMensaje(p, gson.toJson(req));
                            });
                } catch (InterruptedException e) {}
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void detener() {}
}