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
import gestorP2P.config.FileConfigReader;
import gestorP2P.config.IConfigReader;
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

    // Config reader para obtener ip/puerto local
    private final IConfigReader config;

    // Observadores
    private final List<IObservador> observadores = new ArrayList<>();

    public GestorP2PImpl() {
        LoggerCentral.debug("GestorP2PImpl: constructor - inicio inicialización");
        this.enviador = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.peerRegistrar = new PeerRegistrarImpl();
        // Inicializar gson antes de usarlo en los manejadores
        this.gson = new Gson();
        // Inicializar config reader para conocer la ip/puerto local
        this.config = new FileConfigReader();
        // Registrarse como observador del PeerRegistrar para reenviar eventos a la red
        try {
            this.peerRegistrar.registrarObservador(this);
            LoggerCentral.debug("GestorP2PImpl: registrado como observador del PeerRegistrar");
        } catch (Exception e) {
            LoggerCentral.warn("No se pudo registrar observador del PeerRegistrar: " + e.getMessage());
        }
        // Registrar publisher de push para notificar a otros peers cuando se registren o actualice la lista
        try {
            PeerPushPublisherImpl pushPublisher = new PeerPushPublisherImpl();
            this.peerRegistrar.registrarObservador(pushPublisher);
            LoggerCentral.debug("GestorP2PImpl: PeerPushPublisher registrado como observador del PeerRegistrar");
        } catch (Exception e) {
            LoggerCentral.warn("No se pudo registrar PeerPushPublisher como observador: " + e.getMessage());
        }
        // Registrar manejadores para notificaciones entrantes de peers (push/update)
        try {
            LoggerCentral.debug("GestorP2PImpl: registrando manejador para PEER_PUSH");
            // Cuando recibimos una notificación de nuevo peer, solicitamos la lista al remitente
            gestorRespuesta.registrarManejador(AccionesComunicacion.PEER_PUSH, (DTOResponse resp) -> {
                try {
                    if (resp == null) {
                        LoggerCentral.warn("Manejador PEER_PUSH: response nulo recibido");
                        return;
                    }
                    LoggerCentral.debug("Manejador PEER_PUSH: recibido response=" + resp.toString());
                    Object data = resp.getData();
                    String json = gson.toJson(data);
                    DTOPeerPush push = gson.fromJson(json, DTOPeerPush.class);
                    if (push != null) {
                        String ip = push.getIp();
                        int port = push.getPort();
                        LoggerCentral.debug("Manejador PEER_PUSH: push parseado ip=" + ip + " port=" + port + " socketInfo=" + push.getSocketInfo());
                        if (ip != null && port > 0) {
                            // no bloquear el hilo del manejador
                            LoggerCentral.info("PEER_PUSH recibido de " + ip + ":" + port + " - solicitando lista");
                            solicitarListaPeers(ip, port).exceptionally(ex -> { LoggerCentral.error("Error solicitando lista tras PEER_PUSH: " + (ex!=null?ex.getMessage():"<null>"), ex); return null; });
                        } else if (push.getSocketInfo() != null) {
                            String[] parts = push.getSocketInfo().split(":");
                            if (parts.length >= 2) {
                                try { int p = Integer.parseInt(parts[1]); LoggerCentral.debug("Manejador PEER_PUSH: obteniendo puerto desde socketInfo=" + push.getSocketInfo()); solicitarListaPeers(parts[0], p).exceptionally(ex -> { LoggerCentral.warn("Error solicitando lista desde socketInfo: " + (ex!=null?ex.getMessage():"<null>")); return null; }); } catch (Exception e) { LoggerCentral.warn("Manejador PEER_PUSH: puerto en socketInfo no válido='" + push.getSocketInfo() + "'"); }
                            }
                        }
                    } else {
                        LoggerCentral.warn("Manejador PEER_PUSH: push parseado es null para json=" + json);
                    }
                } catch (Exception e) { LoggerCentral.error("Manejador PEER_PUSH: excepción procesando response: " + (e!=null?e.getMessage():"<null>"), e); }
            });

            LoggerCentral.debug("GestorP2PImpl: registrando manejador para PEER_UPDATE");
            // Cuando recibimos una actualización completa de peers, registrarla localmente
            gestorRespuesta.registrarManejador(AccionesComunicacion.PEER_UPDATE, (DTOResponse resp) -> {
                try {
                    if (resp == null) {
                        LoggerCentral.warn("Manejador PEER_UPDATE: response nulo recibido");
                        return;
                    }
                    LoggerCentral.debug("Manejador PEER_UPDATE: recibido response=" + resp.toString());
                    Object data = resp.getData();
                    String json = gson.toJson(data);
                    DTOPeerListResponse lista = gson.fromJson(json, DTOPeerListResponse.class);
                    if (lista != null && lista.getPeers() != null) {
                        LoggerCentral.info("Manejador PEER_UPDATE: lista recibida con count=" + lista.getPeers().size());
                        peerRegistrar.registrarListaDesdeDTO(lista.getPeers());
                    } else {
                        LoggerCentral.warn("Manejador PEER_UPDATE: lista parseada es nula o vacía. Intentando fallback parsear como lista directa");
                        // fallback: intentar como lista directa
                        try {
                            Type listType = new TypeToken<List<DTOPeer>>(){}.getType();
                            List<DTOPeer> dtos = gson.fromJson(json, listType);
                            if (dtos != null) {
                                LoggerCentral.debug("Manejador PEER_UPDATE: fallback parseó lista directa tamaño=" + dtos.size());
                                peerRegistrar.registrarListaDesdeDTO(dtos);
                            }
                        } catch (Exception e) { LoggerCentral.warn("Manejador PEER_UPDATE: fallback falló -> " + e.getMessage()); }
                    }
                } catch (Exception e) { LoggerCentral.error("Manejador PEER_UPDATE: excepción procesando response: " + (e!=null?e.getMessage():"<null>"), e); }
            });
        } catch (Exception e) { LoggerCentral.error("GestorP2PImpl: excepción registrando manejadores iniciales: " + (e!=null?e.getMessage():"<null>"), e); }
        // Starter P2P (por defecto)
        this.starter = new DefaultP2PStarter(this, this.peerRegistrar);
        LoggerCentral.debug("GestorP2PImpl: constructor - fin inicialización");
    }

    @Override
    public CompletableFuture<UUID> unirseRed(String ip, int puerto) {
        LoggerCentral.debug("unirseRed: iniciando petición de join a " + ip + ":" + puerto);
        CompletableFuture<UUID> futuro = new CompletableFuture<>();
        final String ACCION = AccionesComunicacion.PEER_JOIN;

        String requestId = UUID.randomUUID().toString();

        Map<String, Object> payload = new HashMap<>();

        // Corregido: enviar la IP/PUERTO locales en el payload para que el bootstrap conozca nuestra dirección
        String localHost = config.getString("peer.host", "localhost");
        int localPort = config.getInt("peer.puerto", 9000);
        payload.put("ip", localHost);
        payload.put("port", localPort);
        payload.put("socketInfo", localHost + ":" + localPort);

        // Mantener requestId y además opcionalmente incluir destino para claridad
        payload.put("requestId", requestId);
        payload.put("targetSocketInfo", ip + ":" + puerto);

        DTORequest request = new DTORequest(ACCION, payload);

        // Notificar inicio
        notificarObservadores("P2P_JOIN_INICIADA", payload);

        String claveManejador = ACCION + ":" + requestId;

        java.util.function.Consumer<DTOResponse> manejador = (DTOResponse response) -> {
            try {
                LoggerCentral.debug("unirseRed.manejador: recibido response para clave " + claveManejador + " -> " + (response==null?"<null>":response.toString()));
                if (response == null) {
                    String msg = "Respuesta nula al intentar unirse a la red";
                    // Enviar detalles estructurados
                    Map<String, Object> err = new HashMap<>();
                    err.put("requestId", requestId);
                    err.put("targetSocketInfo", ip + ":" + puerto);
                    err.put("message", msg);
                    notificarObservadores("P2P_JOIN_ERROR", err);
                    futuro.completeExceptionally(new RuntimeException(msg));
                    gestorRespuesta.removerManejador(claveManejador);
                    return;
                }

                if (!response.fueExitoso()) {
                    String msg = response.getMessage() != null ? response.getMessage() : "Error al unir peer";
                    Map<String, Object> err = new HashMap<>();
                    err.put("requestId", requestId);
                    err.put("targetSocketInfo", ip + ":" + puerto);
                    err.put("message", msg);
                    err.put("response", response);
                    notificarObservadores("P2P_JOIN_ERROR", err);
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
                        LoggerCentral.debug("unirseRed.manejador: DTOJoinResponse parseado idStr=" + idStr + " respRequestId=" + respRequestId);
                    }
                } catch (Exception pe) {
                    LoggerCentral.warn("unirseRed.manejador: no se pudo parsear DTOJoinResponse -> " + pe.getMessage());
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
                        LoggerCentral.debug("unirseRed.manejador: fallback map parsed idStr=" + idStr + " respRequestId=" + respRequestId);
                    }
                }

                if (respRequestId != null && !requestId.equals(respRequestId)) {
                    // Este handler no corresponde a este request (seguridad extra)
                    Map<String, Object> err = new HashMap<>();
                    err.put("requestId", requestId);
                    err.put("targetSocketInfo", ip + ":" + puerto);
                    err.put("message", "requestId mismatch en la respuesta");
                    err.put("respRequestId", respRequestId);
                    notificarObservadores("P2P_JOIN_ERROR", err);
                    gestorRespuesta.removerManejador(claveManejador);
                    futuro.completeExceptionally(new RuntimeException("requestId mismatch en la respuesta"));
                    return;
                }

                if (idStr == null || idStr.isEmpty() || "null".equalsIgnoreCase(idStr)) {
                    String msg = "UUID no recibido en la respuesta";
                    Map<String, Object> err = new HashMap<>();
                    err.put("requestId", requestId);
                    err.put("targetSocketInfo", ip + ":" + puerto);
                    err.put("message", msg);
                    notificarObservadores("P2P_JOIN_ERROR", err);
                    futuro.completeExceptionally(new RuntimeException(msg));
                    gestorRespuesta.removerManejador(claveManejador);
                    return;
                }

                UUID uuid = UUID.fromString(idStr);

                // Construir Peer y delegar persistencia al repositorio
                Peer peer = new Peer(uuid, ip, null, Peer.Estado.ONLINE, Instant.now());
                boolean guardado = peerRegistrar.registrarPeer(peer, ip + ":" + puerto);

                if (guardado) {
                    LoggerCentral.info("unirseRed.manejador: join exitoso y peer guardado id=" + uuid + " ip=" + ip + " puerto=" + puerto);
                    // No notificar aquí: el PeerRegistrar ya notificará "PEER_REGISTRADO" (con socketInfo) y el gestor la reenvará
                    futuro.complete(uuid);
                } else {
                    String msg = "No se pudo guardar el peer en la persistencia";
                    Map<String, Object> err = new HashMap<>();
                    err.put("requestId", requestId);
                    err.put("targetSocketInfo", ip + ":" + puerto);
                    err.put("message", msg);
                    notificarObservadores("P2P_JOIN_ERROR", err);
                    futuro.completeExceptionally(new RuntimeException(msg));
                }

                // Remover manejador al terminar
                gestorRespuesta.removerManejador(claveManejador);

            } catch (Exception e) {
                gestorRespuesta.removerManejador(claveManejador);
                LoggerCentral.error("Excepción en manejador de unirseRed", e);
                Map<String, Object> err = new HashMap<>();
                err.put("requestId", requestId);
                err.put("targetSocketInfo", ip + ":" + puerto);
                err.put("message", e.getMessage());
                err.put("exception", e);
                notificarObservadores("P2P_JOIN_ERROR", err);
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
            Map<String, Object> err = new HashMap<>();
            err.put("requestId", requestId);
            err.put("targetSocketInfo", ip + ":" + puerto);
            err.put("message", e.getMessage());
            err.put("exception", e);
            notificarObservadores("P2P_JOIN_ERROR", err);
            gestorRespuesta.removerManejador(claveManejador);
            futuro.completeExceptionally(e);
            return futuro;
        }

        // Timeout para el join
        scheduler.schedule(() -> {
            if (!futuro.isDone()) {
                gestorRespuesta.removerManejador(claveManejador);
                String msg = "Timeout esperando respuesta PEER_JOIN";
                LoggerCentral.warn(msg + " requestId=" + requestId);
                Map<String, Object> err = new HashMap<>();
                err.put("requestId", requestId);
                err.put("targetSocketInfo", ip + ":" + puerto);
                err.put("message", msg);
                notificarObservadores("P2P_JOIN_ERROR", err);
                futuro.completeExceptionally(new RuntimeException(msg));
            }
        }, 6, TimeUnit.SECONDS);

        return futuro;
    }

    public CompletableFuture<List<Peer>> solicitarListaPeers(String ip, int puerto) {
        CompletableFuture<List<Peer>> futuro = new CompletableFuture<>();
        final String ACCION = AccionesComunicacion.PEER_LIST;
        String requestId = UUID.randomUUID().toString();

        Map<String, Object> payload = new HashMap<>();

        // Corregido: enviar la IP/PUERTO locales en el payload
        String localHost = config.getString("peer.host", "localhost");
        int localPort = config.getInt("peer.puerto", 9000);
        payload.put("ip", localHost);
        payload.put("port", localPort);
        payload.put("socketInfo", localHost + ":" + localPort);
        payload.put("requestId", requestId);
        payload.put("targetSocketInfo", ip + ":" + puerto);

        DTORequest request = new DTORequest(ACCION, payload);

        String claveManejador = ACCION + ":" + requestId;

        java.util.function.Consumer<DTOResponse> manejador = (DTOResponse response) -> {
            try {
                LoggerCentral.debug("solicitarListaPeers.manejador: recibido response para clave " + claveManejador + " -> " + (response==null?"<null>":response.toString()));
                if (response == null) {
                    String msg = "Respuesta nula al solicitar lista de peers";
                    Map<String, Object> err = new HashMap<>();
                    err.put("requestId", requestId);
                    err.put("targetSocketInfo", ip + ":" + puerto);
                    err.put("message", msg);
                    notificarObservadores("P2P_PEER_LIST_ERROR", err);
                    futuro.completeExceptionally(new RuntimeException(msg));
                    gestorRespuesta.removerManejador(claveManejador);
                    return;
                }

                if (!response.fueExitoso()) {
                    String msg = response.getMessage() != null ? response.getMessage() : "Error al obtener lista de peers";
                    Map<String, Object> err = new HashMap<>();
                    err.put("requestId", requestId);
                    err.put("targetSocketInfo", ip + ":" + puerto);
                    err.put("message", msg);
                    err.put("response", response);
                    notificarObservadores("P2P_PEER_LIST_ERROR", err);
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
                    LoggerCentral.debug("solicitarListaPeers.manejador: intento parseo a DTOPeerListResponse -> " + (listaResp!=null?"ok":"null"));
                } catch (Exception pe) {
                    LoggerCentral.warn("solicitarListaPeers.manejador: no se pudo parsear DTOPeerListResponse -> " + pe.getMessage());
                    // fallback: intentar deserializar como lista directa de DTOPeer
                }

                List<Peer> procesados = new ArrayList<>();
                if (listaResp != null && listaResp.getPeers() != null) {
                    // Delegar al registrador para mapear y persistir
                    List<DTOPeer> dtos = listaResp.getPeers();
                    procesados = peerRegistrar.registrarListaDesdeDTO(dtos);
                    LoggerCentral.info("solicitarListaPeers.manejador: registrados " + procesados.size() + " peers desde DTOPeerListResponse");
                } else {
                    // Fallback: intentar deserializar como lista directa de DTOPeer
                    try {
                        Type listType = new TypeToken<List<DTOPeer>>(){}.getType();
                        List<DTOPeer> dtos = gson.fromJson(jsonData, listType);
                        if (dtos != null) {
                            procesados = peerRegistrar.registrarListaDesdeDTO(dtos);
                            LoggerCentral.info("solicitarListaPeers.manejador: registrados " + procesados.size() + " peers desde lista directa");
                        }
                    } catch (Exception e) {
                        LoggerCentral.warn("solicitarListaPeers.manejador: fallback lista directa falló -> " + e.getMessage());
                        // si falla, procesados queda vacío
                    }
                }

                // El PeerRegistrar ya notifica la lista original (DTOPeer) y el gestor la reenvía.
                futuro.complete(procesados);
                gestorRespuesta.removerManejador(claveManejador);

             } catch (Exception e) {
                 gestorRespuesta.removerManejador(claveManejador);
                 LoggerCentral.error("Excepción en manejador de solicitarListaPeers", e);
                 Map<String, Object> err = new HashMap<>();
                 err.put("requestId", requestId);
                 err.put("targetSocketInfo", ip + ":" + puerto);
                 err.put("message", e.getMessage());
                 err.put("exception", e);
                 notificarObservadores("P2P_PEER_LIST_ERROR", err);
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
            Map<String, Object> err = new HashMap<>();
            err.put("requestId", requestId);
            err.put("targetSocketInfo", ip + ":" + puerto);
            err.put("message", e.getMessage());
            err.put("exception", e);
            notificarObservadores("P2P_PEER_LIST_ERROR", err);
            gestorRespuesta.removerManejador(claveManejador);
            futuro.completeExceptionally(e);
            return futuro;
        }

        // Timeout para la petición de lista
        scheduler.schedule(() -> {
            if (!futuro.isDone()) {
                gestorRespuesta.removerManejador(claveManejador);
                String msg = "Timeout esperando lista de peers";
                LoggerCentral.warn(msg + " requestId=" + requestId);
                Map<String, Object> err = new HashMap<>();
                err.put("requestId", requestId);
                err.put("targetSocketInfo", ip + ":" + puerto);
                err.put("message", msg);
                notificarObservadores("P2P_PEER_LIST_ERROR", err);
                futuro.completeExceptionally(new RuntimeException(msg));
            }
        }, 6, TimeUnit.SECONDS);

        return futuro;
    }

    // Implementaciones de ISujeto (registro/gestión de observadores)
    @Override
    public void registrarObservador(IObservador observador) {
        if (observador == null) return;
        synchronized (observadores) {
            if (!observadores.contains(observador)) {
                observadores.add(observador);
            }
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
        List<IObservador> snapshot;
        synchronized (observadores) {
            snapshot = new ArrayList<>(observadores);
        }
        for (IObservador obs : snapshot) {
            try {
                obs.actualizar(tipoDeDato, datos);
            } catch (Exception e) {
                LoggerCentral.warn("Error notificando observador: " + e.getMessage());
            }
        }
    }

    // Implementación de IObservador (reenvía eventos al propio sujeto para notificar a sus observadores)
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        try {
            // reenvía tal cual a los observadores registrados en este gestor
            notificarObservadores(tipoDeDato, datos);
        } catch (Exception e) {
            LoggerCentral.warn("GestorP2PImpl.actualizar: error reenviando actualización -> " + e.getMessage());
        }
    }

    // Implementación del inicio de la red (bootstrap) delegando al starter configurado
    @Override
    public CompletableFuture<Void> iniciarRed() {
        if (this.starter == null) {
            CompletableFuture<Void> err = new CompletableFuture<>();
            err.completeExceptionally(new IllegalStateException("Starter P2P no configurado"));
            return err;
        }
        try {
            return this.starter.iniciar();
        } catch (Exception e) {
            CompletableFuture<Void> err = new CompletableFuture<>();
            err.completeExceptionally(e);
            return err;
        }
    }

}
