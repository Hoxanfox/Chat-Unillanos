package com.arquitectura.transporte;

import com.arquitectura.controlador.IPeerHandler;
import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.domain.Peer;
import com.arquitectura.domain.enums.EstadoPeer;
import com.arquitectura.events.*;
import com.arquitectura.persistence.repository.PeerRepository;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Gestor del pool de conexiones P2P
 * Maneja conexiones entrantes y salientes con otros servidores de la red
 */
@PropertySource(value = "file:./config/server.properties", ignoreResourceNotFound = true)
@Component
public class PeerConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(PeerConnectionManager.class);

    @Value("${peer.server.port:22200}")
    private int peerPort;

    @Value("${peer.max.connections:50}")
    private int maxPeerConnections;

    @Value("${peer.heartbeat.interval.ms:30000}")
    private long heartbeatIntervalMs;

    @Value("${peer.heartbeat.timeout.seconds:60}")
    private long heartbeatTimeoutSeconds;

    @Value("${peer.reconnect.attempts:3}")
    private int reconnectAttempts;

    @Value("${peer.reconnect.delay.ms:5000}")
    private long reconnectDelayMs;

    @Value("${server.port:22100}")
    private int clientPort;

    // ==================================================================
    // 1. AÑADIDA LA LECTURA DE server.host
    // ==================================================================
    @Value("${server.host:0.0.0.0}")
    private String serverHost;

    @Value("${peer.bootstrap.nodes:}")
    private String bootstrapNodes;

    private final Gson gson;
    private final PeerRepository peerRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Pools y Mapas
    private ExecutorService peerPool;
    private ScheduledExecutorService maintenancePool;
    private final Map<UUID, IPeerHandler> activePeerConnections = new ConcurrentHashMap<>();
    private final Map<UUID, PeerOutgoingConnection> outgoingConnections = new ConcurrentHashMap<>();
    private UUID localPeerId;
    private volatile boolean running = false;

    @Autowired
    public PeerConnectionManager(Gson gson, PeerRepository peerRepository,
                                 ApplicationEventPublisher eventPublisher) {
        this.gson = gson;
        this.peerRepository = peerRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void init() {
        this.peerPool = Executors.newFixedThreadPool(maxPeerConnections);
        this.maintenancePool = Executors.newScheduledThreadPool(4);

        // ==================================================================
        // 2. PASAMOS LA IP DEL HOST AL MÉTODO DE INICIALIZACIÓN
        // ==================================================================
        initializeLocalPeerId(this.serverHost);

        log.info("PeerConnectionManager inicializado. Local Peer ID: {}", localPeerId);
        log.info("Puerto P2P: {}, Max conexiones: {}", peerPort, maxPeerConnections);

        scheduleMaintenanceTasks();

        if (bootstrapNodes != null && !bootstrapNodes.trim().isEmpty()) {
            maintenancePool.schedule(this::autoRegisterWithBootstrapPeers, 5, TimeUnit.SECONDS);
        }
    }

    // REEMPLAZA ESTE MÉTODO COMPLETO
    private void autoRegisterWithBootstrapPeers() {
        log.info("Iniciando auto-registro con bootstrap peers: {}", bootstrapNodes);

        String[] peers = bootstrapNodes.split(",");
        for (String peerAddress : peers) {
            try {
                String[] parts = peerAddress.trim().split(":");
                if (parts.length != 2) {
                    log.warn("Formato inválido de peer bootstrap: {}", peerAddress);
                    continue;
                }

                String ip = parts[0].trim();
                int puerto = Integer.parseInt(parts[1].trim());

                Optional<Peer> existingPeer = peerRepository.findByIpAndPuerto(ip, puerto);
                if (existingPeer.isPresent()) {
                    log.info("Peer bootstrap {} ya está registrado con ID: {}",
                            peerAddress, existingPeer.get().getPeerId());
                    connectToPeer(existingPeer.get().getPeerId(), ip, puerto); // Conectar si ya existe
                    continue;
                }

                // ==================================================================
                // CORRECCIÓN #1 (Arregla el bug de la BD y el de compilación)
                // ==================================================================
                // Usamos el constructor (String, int).
                // Éste pone el estado en DESCONOCIDO por defecto, lo cual es correcto.
                Peer newPeer = new Peer(ip, puerto);
                Peer savedPeer = peerRepository.save(newPeer);

                log.info("✓ Peer bootstrap registrado exitosamente: {} (ID: {})",
                        peerAddress, savedPeer.getPeerId());

                connectToPeer(savedPeer.getPeerId(), ip, puerto);

            } catch (NumberFormatException e) {
                log.error("Puerto inválido en peer bootstrap: {}", peerAddress);
            } catch (Exception e) {
                log.error("Error al registrar peer bootstrap {}: {}", peerAddress, e.getMessage());
            }
        }
    }

    // REEMPLAZA ESTE MÉTODO COMPLETO
    private void initializeLocalPeerId(String hostIp) {
        String ipParaRegistrar = hostIp;

        if ("0.0.0.0".equals(ipParaRegistrar)) {
            // Si la IP es 0.0.0.0, usamos la IP real que detecta NetworkUtils
            // Si eso también falla, usamos "localhost" como último recurso.
            try {
                // Asumiendo que tienes un bean NetworkUtils inyectado (como en PeerServiceImpl)
                // Si no lo tienes, puedes dejar "localhost"
                // ipParaRegistrar = networkUtils.getServerIPAddress();
                ipParaRegistrar = "localhost"; // Temporal hasta que inyectes NetworkUtils
                log.warn("server.host es 0.0.0.0, registrando peer local como '{}'.", ipParaRegistrar);
            } catch (Exception e) {
                ipParaRegistrar = "localhost";
                log.warn("server.host es 0.0.0.0 y NetworkUtils falló. Registrando peer local como 'localhost'.");
            }
        }

        Optional<Peer> localPeer = peerRepository.findByIpAndPuerto(ipParaRegistrar, peerPort);

        if (localPeer.isPresent()) {
            this.localPeerId = localPeer.get().getPeerId();
            log.info("Local Peer ID recuperado de BD: {}", localPeerId);
        } else {
            // ==================================================================
            // CORRECCIÓN #2 (Arregla el bug de "nombre_servidor: ONLINE")
            // ==================================================================
            Peer newLocalPeer = new Peer(ipParaRegistrar, peerPort); // Usar el constructor (String, int)
            newLocalPeer.setConectado(EstadoPeer.ONLINE); // Asignar estado ONLINE (este SÍ existe)
            Peer saved = peerRepository.save(newLocalPeer);
            this.localPeerId = saved.getPeerId();
            log.info("Nuevo Local Peer ID creado: {} (Registrado como {}:{})",
                    localPeerId, ipParaRegistrar, peerPort);
        }
    }

    /**
     * Inicia el servidor P2P para aceptar conexiones entrantes
     */
    public void startPeerServer() {
        running = true;

        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(peerPort)) {
                log.info("Servidor P2P iniciado en puerto {} (Cliente en puerto {})", peerPort, clientPort);

                while (running) {
                    try {
                        Socket peerSocket = serverSocket.accept();

                        if (activePeerConnections.size() >= maxPeerConnections) {
                            log.warn("Conexión P2P rechazada de {}. Límite de {} peers alcanzado.",
                                    peerSocket.getInetAddress().getHostAddress(), maxPeerConnections);

                            try (PrintWriter out = new PrintWriter(peerSocket.getOutputStream(), true)) {
                                DTOResponse errResponse = new DTOResponse(
                                        "connect", "error",
                                        "El servidor ha alcanzado su capacidad máxima de peers.", null
                                );
                                out.println(gson.toJson(errResponse));
                            }
                            peerSocket.close();
                            continue;
                        }

                        log.info("Nueva conexión P2P entrante desde: {}",
                                peerSocket.getInetAddress().getHostAddress());

                        PeerHandler peerHandler = new PeerHandler(
                                peerSocket, gson, this, this::removePeerConnection
                        );
                        peerPool.submit(peerHandler);

                    } catch (IOException e) {
                        if (running) {
                            log.error("Error aceptando conexión P2P: {}", e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Error fatal al iniciar servidor P2P: {}", e.getMessage(), e);
            }
        }, "PeerServerListener").start();
    }

    /**
     * Conecta activamente a otro peer de la red
     */
    public void connectToPeer(UUID peerId, String ip, int port) {
        if (peerId.equals(localPeerId)) {
            log.debug("Ignorando conexión a sí mismo");
            return;
        }

        if (outgoingConnections.containsKey(peerId)) {
            PeerOutgoingConnection outgoing = outgoingConnections.get(peerId);
            if (outgoing != null && outgoing.isConnected()) {
                log.debug("Ya existe una conexión saliente activa con peer {}", peerId);
                return;
            }
        }

        log.info("Iniciando conexión saliente a peer {} ({}:{})", peerId, ip, port);

        PeerOutgoingConnection outgoing = new PeerOutgoingConnection(
                peerId, ip, port, gson, this, reconnectAttempts, reconnectDelayMs
        );
        outgoingConnections.put(peerId, outgoing);
        peerPool.submit(outgoing);
    }

    /**
     * Conecta a todos los peers conocidos en la base de datos
     */
    public void connectToAllKnownPeers() {
        List<Peer> peers = peerRepository.findAll();

        log.info("Conectando a {} peers conocidos en BD...", peers.size());

        if (peers.isEmpty() || (peers.size() == 1 && peers.get(0).getPeerId().equals(localPeerId))) {
            log.warn("⚠️ No hay *otros* peers registrados en la base de datos.");
            log.info("Intentando conectar a peers de arranque (bootstrap)...");
            connectToBootstrapPeers();
            return;
        }

        int connectedCount = 0;
        int invalidCount = 0;

        for (Peer peer : peers) {
            if (peer.getPeerId().equals(localPeerId)) {
                continue; // No conectarse a sí mismo
            }

            if (peer.getIp() == null || peer.getIp().trim().isEmpty()) {
                log.warn("⚠️ Peer {} tiene IP inválida o vacía. Ignorando...", peer.getPeerId());
                invalidCount++;
                continue;
            }

            if (peer.getPuerto() <= 0 || peer.getPuerto() > 65535) {
                log.warn("⚠️ Peer {} tiene puerto inválido: {}. Ignorando...",
                        peer.getPeerId(), peer.getPuerto());
                invalidCount++;
                continue;
            }

            connectToPeer(peer.getPeerId(), peer.getIp(), peer.getPuerto());
            connectedCount++;
        }

        if (invalidCount > 0) {
            log.warn("⚠️ Se encontraron {} peers con datos inválidos en la BD", invalidCount);
        }

        if (connectedCount == 0) {
            log.warn("⚠️ No se pudo conectar a ningún peer válido de la BD. Probando bootstrap...");
            connectToBootstrapPeers();
        } else {
            log.info("✓ Intentando conectar a {} peers válidos", connectedCount);
        }
    }

    /**
     * Conecta a los peers de arranque configurados en server.properties
     */
    private void connectToBootstrapPeers() {
        if (bootstrapNodes == null || bootstrapNodes.trim().isEmpty()) {
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.warn("⚠️  RED AISLADA - Este servidor NO tiene peers configurados");
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.warn("El servidor está en modo LISTENING en puerto {}", peerPort);
            log.warn("Para conectar manualmente a otro peer:");
            log.warn("  1. Asegúrate que el otro servidor esté corriendo");
            log.warn("  2. O configura 'peer.bootstrap.nodes' en server.properties");
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }

        String[] nodes = bootstrapNodes.split(",");
        int bootstrapAttempts = 0;

        log.info("Encontrados {} bootstrap peers configurados", nodes.length);

        for (String node : nodes) {
            node = node.trim();
            if (node.isEmpty()) continue;

            String[] parts = node.split(":");
            if (parts.length != 2) {
                log.warn("Formato inválido de bootstrap peer: '{}' (esperado ip:puerto)", node);
                continue;
            }

            try {
                String ip = parts[0].trim();
                int port = Integer.parseInt(parts[1].trim());

                Optional<Peer> existingPeer = peerRepository.findByIpAndPuerto(ip, port);

                UUID peerId;
                if (existingPeer.isPresent()) {
                    peerId = existingPeer.get().getPeerId();
                    log.info("Bootstrap peer {}:{} ya existe en BD con ID {}", ip, port, peerId);
                } else {
                    Peer newPeer = new Peer(ip, port, "CONNECTING");
                    newPeer.setUltimoLatido(LocalDateTime.now());
                    Peer saved = peerRepository.save(newPeer);
                    peerId = saved.getPeerId();
                    log.info("Nuevo bootstrap peer {}:{} registrado con ID {}", ip, port, peerId);
                }

                connectToPeer(peerId, ip, port);
                bootstrapAttempts++;

            } catch (NumberFormatException e) {
                log.error("Puerto inválido en bootstrap peer: '{}'", node);
            } catch (Exception e) {
                log.error("Error procesando bootstrap peer '{}': {}", node, e.getMessage());
            }
        }

        if (bootstrapAttempts > 0) {
            log.info("✓ Intentando conectar a {} bootstrap peers", bootstrapAttempts);
        } else {
            log.warn("⚠️ No se pudo procesar ningún bootstrap peer");
        }
    }

    /**
     * Programar tareas de mantenimiento periódicas
     */
    private void scheduleMaintenanceTasks() {
        maintenancePool.scheduleAtFixedRate(
                this::checkHeartbeats,
                heartbeatIntervalMs,
                heartbeatIntervalMs,
                TimeUnit.MILLISECONDS
        );

        maintenancePool.scheduleAtFixedRate(
                this::attemptReconnections,
                reconnectDelayMs * 2,
                reconnectDelayMs * 2,
                TimeUnit.MILLISECONDS
        );

        maintenancePool.scheduleAtFixedRate(
                this::syncWithDatabase,
                30000,
                60000,
                TimeUnit.MILLISECONDS
        );

        log.info("Tareas de mantenimiento P2P programadas");
    }

    /**
     * Verifica los heartbeats de todos los peers conectados
     */
    private void checkHeartbeats() {
        long now = System.currentTimeMillis();
        long timeoutMs = heartbeatTimeoutSeconds * 1000;

        List<UUID> peersToRemove = new ArrayList<>();

        for (Map.Entry<UUID, IPeerHandler> entry : activePeerConnections.entrySet()) {
            IPeerHandler handler = entry.getValue();
            long timeSinceLastHeartbeat = now - handler.getLastHeartbeat();

            if (timeSinceLastHeartbeat > timeoutMs) {
                log.warn("Peer {} sin heartbeat por {} ms. Desconectando...",
                        entry.getKey(), timeSinceLastHeartbeat);
                peersToRemove.add(entry.getKey());
            }
        }

        for (UUID peerId : peersToRemove) {
            IPeerHandler handler = activePeerConnections.get(peerId);
            if (handler != null) {
                handler.forceDisconnect();
            }
        }

        if (!peersToRemove.isEmpty()) {
            log.info("Verificación de heartbeats: {} peers desconectados", peersToRemove.size());
        }
    }

    /**
     * Intenta reconectar a peers que se desconectaron
     */
    private void attemptReconnections() {
        List<Peer> offlinePeers = peerRepository.findAll().stream()
                .filter(peer -> !peer.getPeerId().equals(localPeerId))
                .filter(peer -> !isConnectedToPeer(peer.getPeerId()))
                .collect(Collectors.toList());

        if (!offlinePeers.isEmpty()) {
            log.debug("Intentando reconectar a {} peers desconectados", offlinePeers.size());

            for (Peer peer : offlinePeers) {
                connectToPeer(peer.getPeerId(), peer.getIp(), peer.getPuerto());
            }
        }
    }

    /**
     * Sincroniza el estado de los peers con la base de datos
     */
    private void syncWithDatabase() {
        try {
            Set<UUID> connectedPeerIds = getConnectedPeerIds();

            for (UUID peerId : connectedPeerIds) {
                Optional<Peer> peerOpt = peerRepository.findById(peerId);
                if (peerOpt.isPresent()) {
                    Peer peer = peerOpt.get();
                    peer.setConectado(EstadoPeer.ONLINE);
                    peer.setUltimoLatido(LocalDateTime.now());
                    peerRepository.save(peer);
                }
            }

            List<Peer> allPeers = peerRepository.findAll();
            for (Peer peer : allPeers) {
                if (!peer.getPeerId().equals(localPeerId) &&
                        !connectedPeerIds.contains(peer.getPeerId())) {

                    if (EstadoPeer.ONLINE.equals(peer.getConectado())) {
                        peer.setConectado(EstadoPeer.OFFLINE);
                        peerRepository.save(peer);
                        log.info("Peer {} marcado como OFFLINE en BD", peer.getPeerId());
                    }
                }
            }

            log.debug("Sincronización con BD completada. Peers conectados: {}", connectedPeerIds.size());

        } catch (Exception e) {
            log.error("Error sincronizando con base de datos: {}", e.getMessage(), e);
        }
    }

    // REEMPLAZA ESTE MÉTODO COMPLETO
    public void onPeerAuthenticated(IPeerHandler handler) {
        UUID peerId = handler.getPeerId(); // Este es el ID del peer REMOTO

        // Evitar conexiones duplicadas
        if (activePeerConnections.containsKey(peerId)) {
            IPeerHandler existing = activePeerConnections.get(peerId);
            if (existing != null && existing != handler && existing.isConnected()) {
                log.warn("Ya existe una conexión activa para peer {}. Cerrando conexión duplicada.", peerId);
                handler.forceDisconnect();
                return;
            }
        }

        activePeerConnections.put(peerId, handler);

        log.info("Peer {} autenticado y agregado al pool. Total peers activos: {}",
                peerId, activePeerConnections.size());

        eventPublisher.publishEvent(new PeerConnectedEvent(
                peerId, handler.getPeerIp(), handler.getPeerPort()
        ));

        // --- INICIO DE LA CORRECCIÓN ---
        try {
            // Buscamos por IP y Puerto, que son únicos en la red.
            Optional<Peer> peerOpt = peerRepository.findByIpAndPuerto(handler.getPeerIp(), handler.getPeerPort());
            Peer peer;

            if (peerOpt.isPresent()) {
                // Si existe, actualizarlo.
                peer = peerOpt.get();
                log.debug("Actualizando peer existente en BD: {}", peer.getPeerId());
            } else {
                // Si NO existe, crear un nuevo registro
                log.info("Registrando nuevo peer en BD: {}:{}", handler.getPeerIp(), handler.getPeerPort());

                // Usamos el constructor (String ip, int puerto)
                peer = new Peer(handler.getPeerIp(), handler.getPeerPort());

                // IMPORTANTE: Como tu 'id' es @GeneratedValue, NO PODEMOS
                // usar peer.setPeerId(peerId). La BD le asignará un nuevo ID.
                // Esto es correcto.
            }

            // Marcar como ONLINE y actualizar latido
            peer.setConectado(EstadoPeer.ONLINE);
            peer.setUltimoLatido(LocalDateTime.now());
            Peer savedPeer = peerRepository.save(peer); // Guardar y obtener el peer con el ID de BD

            log.info("✓ Peer {} guardado/actualizado en BD ({}:{})",
                    savedPeer.getPeerId(), savedPeer.getIp(), savedPeer.getPuerto());

        } catch (Exception e) {
            log.error("Error al registrar peer autenticado {} en la BD: {}", peerId, e.getMessage(), e);
        }
        // --- FIN DE LA CORRECCIÓN ---
    }

    /**
     * Remueve una conexión peer del pool
     */
    private void removePeerConnection(IPeerHandler handler) {
        UUID peerId = handler.getPeerId();
        if (peerId != null) {
            activePeerConnections.remove(peerId);
            log.info("Peer {} removido del pool. Peers activos restantes: {}",
                    peerId, activePeerConnections.size());

            eventPublisher.publishEvent(new PeerDisconnectedEvent(peerId, "Conexión cerrada"));

            // Actualizar en BD
            Optional<Peer> peerOpt = peerRepository.findById(peerId);
            if (peerOpt.isPresent()) {
                Peer peer = peerOpt.get();
                peer.setConectado(EstadoPeer.OFFLINE);
                peerRepository.save(peer);
            }
        }
    }

    // --- MÉTODOS PÚBLICOS (Sin cambios) ---

    public void processPeerRequest(IPeerHandler handler, DTORequest request) {
        log.info("Procesando request P2P '{}' de peer {}", request.getAction(), handler.getPeerId());

        DTOResponse response = new DTOResponse(
                request.getAction(),
                "success",
                "Request procesado por PeerConnectionManager",
                null
        );
        handler.sendMessage(gson.toJson(response));
    }

    public void handleRetransmitRequest(IPeerHandler handler, DTORequest request) {
        log.info("Manejando retransmisión de peer {}", handler.getPeerId());
    }

    public void handleSyncRequest(IPeerHandler handler, DTORequest request) {
        log.info("Manejando sincronización de peer {}", handler.getPeerId());
    }

    public boolean sendToPeer(UUID peerId, String message) {
        IPeerHandler handler = activePeerConnections.get(peerId);
        if (handler != null && handler.isConnected()) {
            handler.sendMessage(message);
            return true;
        }

        PeerOutgoingConnection outgoing = outgoingConnections.get(peerId);
        if (outgoing != null && outgoing.isConnected()) {
            outgoing.sendMessage(message);
            return true;
        }

        log.warn("No se pudo enviar mensaje a peer {}: no conectado", peerId);
        return false;
    }

    public void broadcastToAllPeers(String message) {
        log.info("Broadcasting mensaje a {} peers", activePeerConnections.size());

        activePeerConnections.values().forEach(handler -> {
            if (handler.isConnected()) {
                handler.sendMessage(message);
            }
        });

        outgoingConnections.values().forEach(conn -> {
            if (conn.isConnected()) {
                conn.sendMessage(message);
            }
        });
    }

    public boolean isConnectedToPeer(UUID peerId) {
        IPeerHandler handler = activePeerConnections.get(peerId);
        if (handler != null && handler.isConnected()) {
            return true;
        }

        PeerOutgoingConnection outgoing = outgoingConnections.get(peerId);
        return outgoing != null && outgoing.isConnected();
    }

    public Set<UUID> getConnectedPeerIds() {
        Set<UUID> connected = new HashSet<>(activePeerConnections.keySet());
        outgoingConnections.forEach((id, conn) -> {
            if (conn.isConnected()) {
                connected.add(id);
            }
        });
        return connected;
    }

    public Map<String, Object> getLocalPeerInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("peerId", localPeerId.toString());
        info.put("port", peerPort);
        info.put("clientPort", clientPort);
        info.put("timestamp", System.currentTimeMillis());
        return info;
    }

    public UUID getLocalPeerId() {
        return localPeerId;
    }

    public int getActivePeerCount() {
        return activePeerConnections.size() + (int) outgoingConnections.values().stream()
                .filter(PeerOutgoingConnection::isConnected)
                .count();
    }

    @EventListener
    public void handleRetransmitToOriginEvent(RetransmitToOriginPeerEvent event) {
        log.info("Retransmitiendo respuesta a peer origen: {}", event.getOriginPeerId());

        DTOResponse response = new DTOResponse(
                "retransmit_response",
                "success",
                "Respuesta retransmitida",
                event.getResponse()
        );

        sendToPeer(event.getOriginPeerId(), gson.toJson(response));
    }

    @PreDestroy
    public void shutdown() {
        log.info("Cerrando PeerConnectionManager...");
        running = false;

        activePeerConnections.values().forEach(IPeerHandler::disconnect);
        outgoingConnections.values().forEach(PeerOutgoingConnection::disconnect);

        peerPool.shutdown();
        maintenancePool.shutdown();

        try {
            if (!peerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                peerPool.shutdownNow();
            }
            if (!maintenancePool.awaitTermination(5, TimeUnit.SECONDS)) {
                maintenancePool.shutdownNow();
            }
        } catch (InterruptedException e) {
            peerPool.shutdownNow();
            maintenancePool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("PeerConnectionManager cerrado correctamente");
    }
}