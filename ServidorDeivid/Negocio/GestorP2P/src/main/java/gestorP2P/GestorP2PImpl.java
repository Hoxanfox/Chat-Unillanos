package gestorP2P;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import comunicacion.peticionesPull.AccionesComunicacion;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import conexion.TipoPool;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.p2p.DTOJoinResponse;
import dto.p2p.DTOPeer;
import dto.p2p.DTOPeerListResponse;
import dto.p2p.DTOPeerPush;
import dominio.p2p.Peer;
import gestorP2P.actualizacion.PeerPushPublisherImpl;
import gestorP2P.inicio.DefaultP2PStarter;
import gestorP2P.inicio.IStarterP2P;
import gestorP2P.registroP2P.IPeerRegistrar;
import gestorP2P.registroP2P.PeerRegistrarImpl;
import observador.IObservador;
import logger.LoggerCentral;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementación del gestor P2P que solicita unirse a la red mediante otra instancia peer.
 * Actúa como sujeto observador para notificar eventos a la UI u otros componentes.
 */
public class GestorP2PImpl implements IGestorP2P, IObservador {

    private final EnviadorPeticiones enviador;
    private final GestorRespuesta gestorRespuesta;
    private final IPeerRegistrar peerRegistrar;
    private final Gson gson;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final IStarterP2P starter;

    // Observadores
    private final List<IObservador> observadores = new ArrayList<>();

    public GestorP2PImpl() {
        this.enviador = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.peerRegistrar = new PeerRegistrarImpl();
        // Inicializar gson antes de usarlo en los manejadores
        this.gson = new Gson();
        // Registrarse como observador del PeerRegistrar para reenviar eventos
        try {
            this.peerRegistrar.registrarObservador(this);
        } catch (Exception e) {
            LoggerCentral.warn("No se pudo registrar observador del PeerRegistrar: " + e.getMessage());
        }
        // Registrar publisher de push para notificar a otros peers cuando se registren o actualice la lista
        try {
            PeerPushPublisherImpl pushPublisher = new PeerPushPublisherImpl();
            this.peerRegistrar.registrarObservador(pushPublisher);
        } catch (Exception e) {
            LoggerCentral.warn("No se pudo registrar PeerPushPublisher como observador: " + e.getMessage());
        }
        // Registrar manejadores para notificaciones entrantes de peers (push/update)
        try {
            // Cuando recibimos una notificación de nuevo peer, solicitamos la lista al remitente
            gestorRespuesta.registrarManejador(AccionesComunicacion.PEER_PUSH, (DTOResponse resp) -> {
                try {
                    Object data = resp.getData();
                    String json = gson.toJson(data);
                    DTOPeerPush push = gson.fromJson(json, DTOPeerPush.class);
                    if (push != null) {
                        String ip = push.getIp();
                        int port = push.getPort();
                        if (ip != null && port > 0) {
                            // no bloquear el hilo del manejador
                            LoggerCentral.info("PEER_PUSH recibido de " + ip + ":" + port + " - solicitando lista");
                            solicitarListaPeers(ip, port).exceptionally(ex -> { LoggerCentral.error("Error solicitando lista tras PEER_PUSH: " + ex.getMessage(), ex); return null; });
                        } else if (push.getSocketInfo() != null) {
                            String[] parts = push.getSocketInfo().split(":");
                            if (parts.length >= 2) {
                                try { int p = Integer.parseInt(parts[1]); solicitarListaPeers(parts[0], p).exceptionally(ex -> null); } catch (Exception ignored) {}
                            }
                        }
                    }
                } catch (Exception ignored) {}
            });

            // Cuando recibimos una actualización completa de peers, registrarla localmente
            gestorRespuesta.registrarManejador(AccionesComunicacion.PEER_UPDATE, (DTOResponse resp) -> {
                try {
                    Object data = resp.getData();
                    String json = gson.toJson(data);
                    DTOPeerListResponse lista = gson.fromJson(json, DTOPeerListResponse.class);
                    if (lista != null && lista.getPeers() != null) {
                        peerRegistrar.registrarListaDesdeDTO(lista.getPeers());
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
        // Starter P2P (por defecto)
        this.starter = new DefaultP2PStarter(this, this.peerRegistrar);
    }

    @Override
    public CompletableFuture<UUID> unirseRed(String ip, int puerto) {
        CompletableFuture<UUID> futuro = new CompletableFuture<>();
        final String ACCION = AccionesComunicacion.PEER_JOIN;

        String requestId = UUID.randomUUID().toString();

        Map<String, Object> payload = new HashMap<>();
        payload.put("ip", ip);
        payload.put("port", puerto);
        payload.put("socketInfo", ip + ":" + puerto);
        payload.put("requestId", requestId);

        DTORequest request = new DTORequest(ACCION, payload);

        // Notificar inicio
        notificarObservadores("P2P_JOIN_INICIADA", payload);

        String claveManejador = ACCION + ":" + requestId;

        java.util.function.Consumer<DTOResponse> manejador = (DTOResponse response) -> {
            try {
                if (response == null) {
                    String msg = "Respuesta nula al intentar unirse a la red";
                    notificarObservadores("P2P_JOIN_ERROR", msg);
                    futuro.completeExceptionally(new RuntimeException(msg));
                    gestorRespuesta.removerManejador(claveManejador);
                    return;
                }

                if (!response.fueExitoso()) {
                    String msg = response.getMessage() != null ? response.getMessage() : "Error al unir peer";
                    notificarObservadores("P2P_JOIN_ERROR", msg);
                    futuro.completeExceptionally(new RuntimeException(msg));
                    gestorRespuesta.removerManejador(claveManejador);
                    return;
                }

                // Intentar deserializar data a DTOJoinResponse
                String idStr = null;
                String respRequestId = null;
                try {
                    DTOJoinResponse joinResp = gson.fromJson(gson.toJson(response.getData()), DTOJoinResponse.class);
                    if (joinResp != null) {
                        if (joinResp.getId() != null && !joinResp.getId().isEmpty()) idStr = joinResp.getId();
                        else if (joinResp.getUuid() != null && !joinResp.getUuid().isEmpty()) idStr = joinResp.getUuid();
                        respRequestId = joinResp.getRequestId();
                    }
                } catch (Exception ignored) {
                    // fallback a map parsing
                }

                if (idStr == null) {
                    Object data = response.getData();
                    String jsonData = gson.toJson(data);
                    Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> map = gson.fromJson(jsonData, mapType);
                    if (map != null) {
                        if (map.containsKey("id")) idStr = String.valueOf(map.get("id"));
                        else if (map.containsKey("uuid")) idStr = String.valueOf(map.get("uuid"));
                        if (respRequestId == null && map.containsKey("requestId")) respRequestId = String.valueOf(map.get("requestId"));
                    }
                }

                if (respRequestId != null && !requestId.equals(respRequestId)) {
                    // Este handler no corresponde a este request (seguridad extra)
                    gestorRespuesta.removerManejador(claveManejador);
                    futuro.completeExceptionally(new RuntimeException("requestId mismatch en la respuesta"));
                    return;
                }

                if (idStr == null || idStr.isEmpty() || "null".equalsIgnoreCase(idStr)) {
                    String msg = "UUID no recibido en la respuesta";
                    notificarObservadores("P2P_JOIN_ERROR", msg);
                    futuro.completeExceptionally(new RuntimeException(msg));
                    gestorRespuesta.removerManejador(claveManejador);
                    return;
                }

                UUID uuid = UUID.fromString(idStr);

                // Construir Peer y delegar persistencia al repositorio
                Peer peer = new Peer(uuid, ip, null, Peer.Estado.ONLINE, Instant.now());
                boolean guardado = peerRegistrar.registrarPeer(peer, ip + ":" + puerto);

                if (guardado) {
                    notificarObservadores("P2P_JOIN_EXITOSA", peer);
                    futuro.complete(uuid);
                } else {
                    String msg = "No se pudo guardar el peer en la persistencia";
                    notificarObservadores("P2P_JOIN_ERROR", msg);
                    futuro.completeExceptionally(new RuntimeException(msg));
                }

                // Remover manejador al terminar
                gestorRespuesta.removerManejador(claveManejador);

            } catch (Exception e) {
                gestorRespuesta.removerManejador(claveManejador);
                LoggerCentral.error("Excepción en manejador de unirseRed", e);
                notificarObservadores("P2P_JOIN_ERROR", e.getMessage());
                futuro.completeExceptionally(e);
            }
        };

        // Registrar manejador específico y enviar por pool PEERS
        try {
            gestorRespuesta.registrarManejador(claveManejador, manejador);
            LoggerCentral.debug("Manejador registrado para " + claveManejador);
        } catch (Exception e) {
            LoggerCentral.error("No se pudo registrar manejador para join: " + claveManejador, e);
            futuro.completeExceptionally(e);
            return futuro;
        }

        try {
            enviador.enviar(request, TipoPool.PEERS);
            LoggerCentral.info("Enviada solicitud PEER_JOIN con requestId=" + requestId + " hacia " + ip + ":" + puerto);
        } catch (Exception e) {
            LoggerCentral.error("Error enviando solicitud PEER_JOIN: " + e.getMessage(), e);
            gestorRespuesta.removerManejador(claveManejador);
            futuro.completeExceptionally(e);
            return futuro;
        }

        // Timeout defensivo: si expira, remover manejador y notificar
        scheduler.schedule(() -> {
            if (!futuro.isDone()) {
                gestorRespuesta.removerManejador(claveManejador);
                String msg = "Timeout esperando respuesta de join";
                LoggerCentral.warn(msg + " requestId=" + requestId);
                notificarObservadores("P2P_JOIN_ERROR", msg);
                futuro.completeExceptionally(new RuntimeException(msg));
            }
        }, 6, TimeUnit.SECONDS);

        return futuro;
    }

    @Override
    public CompletableFuture<List<Peer>> solicitarListaPeers(String ip, int puerto) {
        CompletableFuture<List<Peer>> futuro = new CompletableFuture<>();
        final String ACCION = AccionesComunicacion.PEER_LIST;

        String requestId = UUID.randomUUID().toString();

        Map<String, Object> payload = new HashMap<>();
        payload.put("ip", ip);
        payload.put("port", puerto);
        payload.put("socketInfo", ip + ":" + puerto);
        payload.put("requestId", requestId);

        DTORequest request = new DTORequest(ACCION, payload);

        // Notificar inicio
        notificarObservadores("P2P_PEER_LIST_SOLICITADA", payload);

        String claveManejador = ACCION + ":" + requestId;

        java.util.function.Consumer<DTOResponse> manejador = (DTOResponse response) -> {
            try {
                if (response == null) {
                    String msg = "Respuesta nula al solicitar lista de peers";
                    notificarObservadores("P2P_PEER_LIST_ERROR", msg);
                    futuro.completeExceptionally(new RuntimeException(msg));
                    gestorRespuesta.removerManejador(claveManejador);
                    return;
                }

                if (!response.fueExitoso()) {
                    String msg = response.getMessage() != null ? response.getMessage() : "Error al obtener lista de peers";
                    notificarObservadores("P2P_PEER_LIST_ERROR", msg);
                    futuro.completeExceptionally(new RuntimeException(msg));
                    gestorRespuesta.removerManejador(claveManejador);
                    return;
                }

                // Deserializar a DTOPeerListResponse para usar DTOs y delegar persistencia al PeerRegistrar
                Object data = response.getData();
                String jsonData = gson.toJson(data);
                DTOPeerListResponse listaResp = null;
                try {
                    listaResp = gson.fromJson(jsonData, DTOPeerListResponse.class);
                } catch (Exception ignored) {
                    // fallback: intentar deserializar como lista directa de DTOPeer
                }

                List<Peer> procesados = new ArrayList<>();
                if (listaResp != null && listaResp.getPeers() != null) {
                    // Delegar al registrador para mapear y persistir
                    List<DTOPeer> dtos = listaResp.getPeers();
                    procesados = peerRegistrar.registrarListaDesdeDTO(dtos);
                } else {
                    // Fallback: intentar deserializar como lista directa de DTOPeer
                    try {
                        Type listType = new TypeToken<List<DTOPeer>>(){}.getType();
                        List<DTOPeer> dtos = gson.fromJson(jsonData, listType);
                        if (dtos != null) {
                            procesados = peerRegistrar.registrarListaDesdeDTO(dtos);
                        }
                    } catch (Exception ignored) {
                        // si falla, procesados queda vacío
                    }
                }

                Map<String, Object> paquete = new HashMap<>();
                paquete.put("peers", procesados);
                paquete.put("count", procesados.size());
                paquete.put("timestamp", java.time.Instant.now());

                notificarObservadores("P2P_PEER_LIST_RECIBIDA", paquete);
                // También emitir paquete de actualización global del gestorP2P
                notificarObservadores("P2P_ACTUALIZACION", paquete);

                futuro.complete(procesados);
                gestorRespuesta.removerManejador(claveManejador);

            } catch (Exception e) {
                gestorRespuesta.removerManejador(claveManejador);
                LoggerCentral.error("Excepción en manejador de solicitarListaPeers", e);
                notificarObservadores("P2P_PEER_LIST_ERROR", e.getMessage());
                futuro.completeExceptionally(e);
            }
        };

        // Registrar manejador específico y enviar por pool PEERS
        try {
            gestorRespuesta.registrarManejador(claveManejador, manejador);
            LoggerCentral.debug("Manejador registrado para " + claveManejador);
        } catch (Exception e) {
            LoggerCentral.error("No se pudo registrar manejador para peer list: " + claveManejador, e);
            futuro.completeExceptionally(e);
            return futuro;
        }

        try {
            enviador.enviar(request, TipoPool.PEERS);
            LoggerCentral.info("Enviada solicitud PEER_LIST con requestId=" + requestId + " hacia " + ip + ":" + puerto);
        } catch (Exception e) {
            LoggerCentral.error("Error enviando solicitud PEER_LIST: " + e.getMessage(), e);
            gestorRespuesta.removerManejador(claveManejador);
            futuro.completeExceptionally(e);
            return futuro;
        }

        scheduler.schedule(() -> {
            if (!futuro.isDone()) {
                gestorRespuesta.removerManejador(claveManejador);
                String msg = "Timeout esperando lista de peers";
                LoggerCentral.warn(msg + " requestId=" + requestId);
                notificarObservadores("P2P_PEER_LIST_ERROR", msg);
                futuro.completeExceptionally(new RuntimeException(msg));
            }
        }, 6, TimeUnit.SECONDS);

        return futuro;
    }

    @Override
    public CompletableFuture<Void> iniciarRed() {
        return starter.iniciar();
    }

    // Implementación del patrón Observador (ISujeto)
    @Override
    public void registrarObservador(IObservador observador) {
        if (observador == null) return;
        synchronized (observadores) {
            if (!observadores.contains(observador)) observadores.add(observador);
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        if (observador == null) return;
        synchronized (observadores) {
            observadores.remove(observador);
        }
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        List<IObservador> copia;
        synchronized (observadores) {
            copia = new ArrayList<>(observadores);
        }
        for (IObservador obs : copia) {
            try {
                obs.actualizar(tipoDeDato, datos);
            } catch (Exception ignored) {
                // no interrumpir notificaciones si un observador falla
            }
        }
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        // Reenviar eventos del PeerRegistrar a los observadores del GestorP2P
        try {
            notificarObservadores(tipoDeDato, datos);
        } catch (Exception ignored) {
            // no detener el flujo si un observador falla
        }
    }
}
