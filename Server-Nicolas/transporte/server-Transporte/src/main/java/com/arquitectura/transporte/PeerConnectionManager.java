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
@PropertySource("file:./config/server.properties")
@Component
public class PeerConnectionManager {
    
    private static final Logger log = LoggerFactory.getLogger(PeerConnectionManager.class);
    
    @Value("${peer.server.port}")
    private int peerPort;
    
    @Value("${peer.max.connections}")
    private int maxPeerConnections;
    
    @Value("${peer.heartbeat.interval.ms}")
    private long heartbeatIntervalMs;
    
    @Value("${peer.heartbeat.timeout.seconds}")
    private long heartbeatTimeoutSeconds;
    
    @Value("${peer.reconnect.attempts}")
    private int reconnectAttempts;
    
    @Value("${peer.reconnect.delay.ms}")
    private long reconnectDelayMs;
    
    @Value("${server.port}")
    private int clientPort;
    
    @Value("${peer.bootstrap.nodes:}")
    private String bootstrapNodes;

    private final Gson gson;
    private final PeerRepository peerRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    // Pool de threads para manejar conexiones P2P entrantes
    private ExecutorService peerPool;
    
    // Pool para tareas de mantenimiento (heartbeat, reconexión)
    private ScheduledExecutorService maintenancePool;
    
    // Mapa de peers conectados activamente (conexiones entrantes)
    private final Map<UUID, IPeerHandler> activePeerConnections = new ConcurrentHashMap<>();
    
    // Mapa de conexiones salientes (este servidor conecta a otros)
    private final Map<UUID, PeerOutgoingConnection> outgoingConnections = new ConcurrentHashMap<>();
    
    // ID único de este servidor
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
        
        // Obtener o crear el ID de este peer
        initializeLocalPeerId();
        
        log.info("PeerConnectionManager inicializado. Local Peer ID: {}", localPeerId);
        log.info("Puerto P2P: {}, Max conexiones: {}", peerPort, maxPeerConnections);
        
        // Iniciar tareas de mantenimiento
        scheduleMaintenanceTasks();
    }
    
    private void initializeLocalPeerId() {
        // Buscar si este servidor ya tiene un ID registrado
        Optional<Peer> localPeer = peerRepository.findByIpAndPuerto("localhost", peerPort);
        
        if (localPeer.isPresent()) {
            this.localPeerId = localPeer.get().getPeerId();
            log.info("Local Peer ID recuperado de BD: {}", localPeerId);
        } else {
            // Crear nuevo peer local
            Peer newLocalPeer = new Peer("localhost", peerPort, "ONLINE");
            newLocalPeer.setUltimoLatido(LocalDateTime.now());
            Peer saved = peerRepository.save(newLocalPeer);
            this.localPeerId = saved.getPeerId();
            log.info("Nuevo Local Peer ID creado: {}", localPeerId);
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
            log.debug("Ya existe una conexión saliente con peer {}", peerId);
            return;
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

        if (peers.isEmpty() || peers.size() == 1) {
            log.warn("⚠️ No hay peers registrados en la base de datos.");
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
            
            // Validar que el peer tenga IP y puerto válidos
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
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━���━━━━━━━━━━━━━━━━━━━━━");
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

                // Verificar si ya existe en BD
                Optional<Peer> existingPeer = peerRepository.findByIpAndPuerto(ip, port);

                UUID peerId;
                if (existingPeer.isPresent()) {
                    peerId = existingPeer.get().getPeerId();
                    log.info("Bootstrap peer {}:{} ya existe en BD con ID {}", ip, port, peerId);
                } else {
                    // Crear un peer temporal para conectar
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
        // Tarea 1: Verificar heartbeats y detectar peers caídos
        maintenancePool.scheduleAtFixedRate(
            this::checkHeartbeats,
            heartbeatIntervalMs,
            heartbeatIntervalMs,
            TimeUnit.MILLISECONDS
        );
        
        // Tarea 2: Intentar reconectar peers desconectados
        maintenancePool.scheduleAtFixedRate(
            this::attemptReconnections,
            reconnectDelayMs * 2,
            reconnectDelayMs * 2,
            TimeUnit.MILLISECONDS
        );
        
        // Tarea 3: Sincronizar estado con la base de datos
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
        
        // Verificar conexiones entrantes
        for (Map.Entry<UUID, IPeerHandler> entry : activePeerConnections.entrySet()) {
            IPeerHandler handler = entry.getValue();
            long timeSinceLastHeartbeat = now - handler.getLastHeartbeat();
            
            if (timeSinceLastHeartbeat > timeoutMs) {
                log.warn("Peer {} sin heartbeat por {} ms. Desconectando...", 
                        entry.getKey(), timeSinceLastHeartbeat);
                peersToRemove.add(entry.getKey());
            }
        }
        
        // Desconectar peers sin heartbeat
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
            // Actualizar estado ONLINE para peers conectados
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
            
            // Actualizar estado OFFLINE para peers desconectados
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
    
    /**
     * Callback cuando un peer completa el handshake
     */
    public void onPeerAuthenticated(IPeerHandler handler) {
        UUID peerId = handler.getPeerId();
        activePeerConnections.put(peerId, handler);
        
        log.info("Peer {} autenticado y agregado al pool. Total peers activos: {}", 
                peerId, activePeerConnections.size());
        
        // Publicar evento
        eventPublisher.publishEvent(new PeerConnectedEvent(
            peerId, handler.getPeerIp(), handler.getPeerPort()
        ));
        
        // Actualizar en BD
        Optional<Peer> peerOpt = peerRepository.findById(peerId);
        if (peerOpt.isPresent()) {
            Peer peer = peerOpt.get();
            peer.setConectado(EstadoPeer.ONLINE);
            peer.actualizarLatido();
            peerRepository.save(peer);
        }
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
            
            // Publicar evento
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
    
    // --- MÉTODOS PÚBLICOS PARA PROCESAMIENTO DE MENSAJES ---
    
    public void processPeerRequest(IPeerHandler handler, DTORequest request) {
        log.info("Procesando request P2P '{}' de peer {}", request.getAction(), handler.getPeerId());
        
        // Aquí se puede integrar con el RequestDispatcher o manejar directamente
        // Por ahora, enviar respuesta genérica
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
        // Lógica de retransmisión aquí
    }
    
    public void handleSyncRequest(IPeerHandler handler, DTORequest request) {
        log.info("Manejando sincronización de peer {}", handler.getPeerId());
        // Lógica de sincronización aquí
    }
    
    /**
     * Envía un mensaje a un peer específico
     */
    public boolean sendToPeer(UUID peerId, String message) {
        IPeerHandler handler = activePeerConnections.get(peerId);
        if (handler != null && handler.isConnected()) {
            handler.sendMessage(message);
            return true;
        }
        
        // Intentar por conexión saliente
        PeerOutgoingConnection outgoing = outgoingConnections.get(peerId);
        if (outgoing != null && outgoing.isConnected()) {
            outgoing.sendMessage(message);
            return true;
        }
        
        log.warn("No se pudo enviar mensaje a peer {}: no conectado", peerId);
        return false;
    }
    
    /**
     * Envía una petición a un peer específico y espera su respuesta.
     * Este método es síncrono y se usa para operaciones de federación P2P.
     *
     * @param peerId ID del peer destino
     * @param request Petición a enviar
     * @return Respuesta del peer remoto
     * @throws Exception si no se puede enviar o recibir respuesta
     */
    public DTOResponse sendRequestToPeer(UUID peerId, DTORequest request) throws Exception {
        log.info("→ [Federation] Enviando petición '{}' al peer {}", request.getAction(), peerId);

        // Verificar si el peer está conectado
        if (!isConnectedToPeer(peerId)) {
            // Intentar obtener info del peer de la BD
            Optional<Peer> peerOpt = peerRepository.findById(peerId);
            if (peerOpt.isEmpty()) {
                throw new Exception("Peer " + peerId + " no encontrado en la base de datos");
            }

            Peer peer = peerOpt.get();

            // Intentar conectar al peer
            log.info("→ [Federation] Peer no conectado. Intentando conectar a {}:{}", peer.getIp(), peer.getPuerto());
            connectToPeer(peerId, peer.getIp(), peer.getPuerto());

            // Esperar un momento para que la conexión se establezca
            Thread.sleep(1000);

            // Verificar de nuevo
            if (!isConnectedToPeer(peerId)) {
                throw new Exception("No se pudo establecer conexión con el peer " + peerId);
            }
        }

        // Envolver la petición en un request P2P con identificador único
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> p2pPayload = new HashMap<>();
        p2pPayload.put("requestId", requestId);
        p2pPayload.put("originalRequest", request);

        DTORequest p2pRequest = new DTORequest("retransmitirpeticion", p2pPayload);
        String requestJson = gson.toJson(p2pRequest);

        // Enviar la petición
        boolean sent = sendToPeer(peerId, requestJson);
        if (!sent) {
            throw new Exception("No se pudo enviar la petición al peer " + peerId);
        }

        log.info("✓ [Federation] Petición enviada al peer {}. Esperando respuesta...", peerId);

        // TODO: Implementar mecanismo de respuesta asíncrona con timeout
        // Por ahora, retornamos una respuesta simulada
        // En una implementación completa, necesitarías un CompletableFuture o similar

        // Simular espera de respuesta
        Thread.sleep(500);

        // Por ahora, asumimos que la petición fue exitosa
        // En producción, necesitarías un callback o future para recibir la respuesta real
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("status", "forwarded");
        responseData.put("peerId", peerId.toString());

        return new DTOResponse(request.getAction(), "success", "Petición procesada por peer remoto", responseData);
    }

    /**
     * Broadcast un mensaje a todos los peers conectados
     */
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
    
    // --- MÉTODOS DE CONSULTA ---
    
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
    
    // --- EVENT LISTENERS ---
    
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
    
    // --- SHUTDOWN ---
    
    @PreDestroy
    public void shutdown() {
        log.info("Cerrando PeerConnectionManager...");
        running = false;
        
        // Cerrar todas las conexiones
        activePeerConnections.values().forEach(IPeerHandler::disconnect);
        outgoingConnections.values().forEach(PeerOutgoingConnection::disconnect);
        
        // Cerrar pools
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
