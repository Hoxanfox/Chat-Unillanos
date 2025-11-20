package comunicacion.p2p;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import comunicacion.IRouterMensajes;
import configuracion.Configuracion;
import conexion.IGestorConexiones;
import dominio.p2p.Peer;
import dto.comunicacion.DTOResponse;
import dto.p2p.DTOPeerDetails;
import repositorio.p2p.PeerRepositorio;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AccionesP2P {

    private final IGestorConexiones gestorConexiones;
    private final PeerRepositorio repositorio;
    private final Gson gson;
    private final Configuracion config;

    public AccionesP2P(IGestorConexiones gestorConexiones) {
        this.gestorConexiones = gestorConexiones;
        this.repositorio = new PeerRepositorio();
        this.gson = new Gson();
        this.config = Configuracion.getInstance();
    }

    public void registrarAcciones(IRouterMensajes router) {

        // ================================================================
        //  1. ROL SERVIDOR: Procesar petición de unión ("añadirPeer")
        // ================================================================
        router.registrarAccion("añadirPeer", (datosJson, origenId) -> {
            try {
                // 1. Validaciones básicas
                if (datosJson == null || !datosJson.isJsonObject()) {
                    return new DTOResponse("añadirPeer", "error", "Datos vacíos o inválidos", null);
                }

                JsonObject obj = datosJson.getAsJsonObject();
                // Validar campos obligatorios
                if (!obj.has("ip") || !obj.has("puerto")) {
                    return new DTOResponse("añadirPeer", "error", "Faltan ip o puerto", null);
                }

                String ipRemota = obj.get("ip").getAsString();
                int puertoRemoto = obj.get("puerto").getAsInt();
                String socketInfo = ipRemota + ":" + puertoRemoto;

                System.out.println("[P2P-Server] Solicitud de unión recibida de: " + socketInfo);

                // 2. Conectar de vuelta (Bidireccionalidad)
                // Si ya estamos conectados, el gestor lo maneja internamente.
                gestorConexiones.conectarAPeer(ipRemota, puertoRemoto);

                // 3. Persistir el nuevo nodo en la Base de Datos Local
                Peer nuevoPeer = new Peer();
                nuevoPeer.setIp(ipRemota);
                nuevoPeer.setEstado(Peer.Estado.ONLINE);

                // Verificar si ya existía en BD para mantener su ID y Fecha original
                Peer existente = repositorio.obtenerPorSocketInfo(socketInfo);
                if (existente != null) {
                    nuevoPeer.setId(existente.getId());
                    nuevoPeer.setFechaCreacion(existente.getFechaCreacion());
                }

                // Guardamos en BD
                repositorio.guardarOActualizarPeer(nuevoPeer, socketInfo);

                // 4. Obtener lista completa de la BD para enviarla al nuevo nodo (Sincronización)
                List<PeerRepositorio.PeerInfo> peersDb = repositorio.listarPeersInfo();

                // Convertimos la info de BD a DTOs ligeros para el transporte
                List<DTOPeerDetails> listaParaEnviar = peersDb.stream()
                        .map(p -> new DTOPeerDetails(
                                p.id.toString(),
                                p.ip,
                                p.puerto,
                                p.estado.name(),
                                p.fechaCreacion.toString()))
                        .collect(Collectors.toList());

                JsonElement dataResponse = gson.toJsonTree(listaParaEnviar);

                System.out.println("[P2P-Server] Respondiendo a " + socketInfo + " con " + listaParaEnviar.size() + " peers.");
                return new DTOResponse("añadirPeer", "success", "Bienvenido a la red. Lista adjunta.", dataResponse);

            } catch (Exception e) {
                e.printStackTrace();
                return new DTOResponse("añadirPeer", "error", "Error interno server: " + e.getMessage(), null);
            }
        });

        // ================================================================
        //  2. ROL CLIENTE: Procesar respuesta de la unión (La lista que nos devuelven)
        // ================================================================
        router.registrarManejadorRespuesta("añadirPeer", (response) -> {
            if (!response.fueExitoso()) {
                System.err.println("[P2P-Client] Error al unirse a la red: " + response.getMessage());
                return;
            }

            JsonElement data = response.getData();
            if (data != null && data.isJsonArray()) {
                System.out.println("[P2P-Client] ¡Lista de peers recibida de la semilla! Procesando...");
                procesarListaPeers(data.getAsJsonArray());
            } else {
                System.out.println("[P2P-Client] Unido exitosamente, pero la lista de peers vino vacía.");
            }
        });
    }

    /**
     * Lógica auxiliar para recorrer la lista JSON, guardar en BD y conectar.
     */
    private void procesarListaPeers(JsonArray listaJson) {
        String miHost = config.getPeerHost();
        int miPuerto = config.getPeerPuerto();

        int nuevos = 0;
        for (JsonElement elem : listaJson) {
            try {
                JsonObject pObj = elem.getAsJsonObject();

                // Extracción segura de datos
                String ip = pObj.get("ip").getAsString();
                int puerto = pObj.get("puerto").getAsInt();

                // IMPORTANTE: Ignorarnos a nosotros mismos si venimos en la lista
                if (ip.equals(miHost) && puerto == miPuerto) continue;

                // Recuperar ID si viene, sino generar uno temporal (se corregirá al conectar)
                String idStr = pObj.has("id") ? pObj.get("id").getAsString() : UUID.randomUUID().toString();
                String estadoStr = pObj.has("estado") ? pObj.get("estado").getAsString() : "OFFLINE";

                String socketInfo = ip + ":" + puerto;

                // 1. Guardar en Base de Datos
                Peer peerDb = new Peer();
                try { peerDb.setId(UUID.fromString(idStr)); } catch (Exception e) { peerDb.setId(UUID.randomUUID()); }
                peerDb.setIp(ip);
                try { peerDb.setEstado(Peer.Estado.valueOf(estadoStr)); } catch (Exception e) { peerDb.setEstado(Peer.Estado.OFFLINE); }

                boolean guardado = repositorio.guardarOActualizarPeer(peerDb, socketInfo);

                // 2. Conectar al nuevo peer descubierto
                if (guardado) {
                    // Verificamos si ya estamos conectados en memoria antes de intentar (para no saturar)
                    boolean yaConectadoEnMemoria = gestorConexiones.obtenerDetallesPeers().stream()
                            .anyMatch(d -> d.getId().equals(socketInfo));

                    if (!yaConectadoEnMemoria) {
                        //System.out.println("[P2P-Client] Descubierto nuevo peer: " + socketInfo + ". Conectando...");
                        gestorConexiones.conectarAPeer(ip, puerto);
                        nuevos++;
                    }
                }

            } catch (Exception e) {
                System.err.println("[P2P-Client] Error procesando peer individual de la lista: " + e.getMessage());
            }
        }
        if (nuevos > 0) {
            System.out.println("[P2P-Client] Sincronización completada. Intentando conectar con " + nuevos + " nuevos peers.");
        }
    }
}