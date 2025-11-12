package com.arquitectura.transporte.peer;

import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.controlador.peer.IPeerHandler;
import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.controlador.RequestDispatcher;
import com.arquitectura.events.*;
import com.arquitectura.logicaPeers.IPeerService;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

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
    
    @Value("${peer.bootstrap.nodes:}")
    private String bootstrapNodes;

    private final Gson gson;
    private final IPeerService peerService;
    private final ApplicationEventPublisher eventPublisher;
    private final com.arquitectura.utils.network.NetworkUtils networkUtils;
    @Autowired
    private RequestDispatcher requestDispatcher;

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
    public PeerConnectionManager(
            Gson gson,
            @Qualifier("peerServiceP2P") IPeerService peerService,
            ApplicationEventPublisher eventPublisher,
            com.arquitectura.utils.network.NetworkUtils networkUtils) {
        this.gson = gson;
        this.peerService = peerService;
        this.eventPublisher = eventPublisher;
        this.networkUtils = networkUtils;
    }
    
    @PostConstruct
    public void init() {
        this.peerPool = Executors.newFixedThreadPool(maxPeerConnections);
        this.maintenancePool = Executors.newScheduledThreadPool(4);
        
        // Obtener o crear el ID de este peer
        initializeLocalPeerId();
        
        log.info("PeerConnectionManager inicializado. Local Peer ID: {}", localPeerId);
        log.info("Puerto P2P: {}, Max conexiones: {}", peerPort, maxPeerConnections);
        
        // NUEVO: Inicializar tabla de peers y conectar a la red
        initializePeersOnStartup();

        // Iniciar tareas de mantenimiento
        scheduleMaintenanceTasks();
        
        // Auto-registrar con bootstrap peers (con delay para que el servidor esté listo)
        if (bootstrapNodes != null && !bootstrapNodes.trim().isEmpty()) {
            maintenancePool.schedule(this::autoRegisterWithBootstrapPeers, 5, TimeUnit.SECONDS);
        }
    }
    
    /**
     * Inicializa la tabla de peers al arrancar el servidor:
     * 1) Lee peers de la BD
     * 2) Si solo está el peer local, intenta poblar desde bootstrap (y registra en BD)
     * 3) Espera breve para que la BD persista
     * 4) Lanza conexiones a todos los peers conocidos
     */
    public void initializePeersOnStartup() {
        try {
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("INICIALIZANDO PEERS EN STARTUP");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // PASO 1: Leer peers desde la BD
            List<PeerResponseDto> peers = peerService.listarPeersDisponibles();
            log.info("Peers encontrados en BD: {}", peers == null ? 0 : peers.size());

            if (peers != null) {
                for (PeerResponseDto peer : peers) {
                    log.info("  - Peer: {} ({}:{}) - Estado: {}",
                            peer.getPeerId(), peer.getIp(), peer.getPuerto(), peer.getConectado());
                }
            }

            // PASO 2: Si no hay peers o solo existe el local, intentar bootstrap
            if (peers == null || peers.isEmpty() || peers.size() == 1) {
                log.warn("⚠️ La BD contiene 0 o solo 1 peer (probablemente solo el local)");
                log.info("Intentando poblar tabla de peers desde bootstrap...");

                connectToBootstrapPeers();

                // PASO 3: Esperar para que los registros de bootstrap se persistan en BD
                log.info("Esperando 2 segundos para que peers bootstrap se registren en BD...");
                try {
                    Thread.sleep(2000); // 2 segundos
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Espera interrumpida durante inicialización de peers");
                }

                // Volver a leer la BD luego del intento de bootstrap
                peers = peerService.listarPeersDisponibles();
                log.info("Peers en BD tras bootstrap: {}", peers == null ? 0 : peers.size());
            } else {
                log.info("✓ Se encontraron {} peers en BD", peers.size());
            }

            // PASO 4: Conectar a todos los peers conocidos en BD
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("CONECTANDO A PEERS CONOCIDOS");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            connectToAllKnownPeers();

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("INICIALIZACIÓN DE PEERS COMPLETADA");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("ERROR INICIALIZANDO PEERS EN STARTUP: {}", e.getMessage(), e);
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }

    /**
     * Auto-registra este servidor con los peers bootstrap configurados
     */
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
                
                // Verificar si el peer ya está registrado usando el servicio
                Optional<PeerResponseDto> existingPeer = peerService.buscarPeerPorIpYPuerto(ip, puerto);
                if (existingPeer.isPresent()) {
                    log.info("Peer bootstrap {} ya está registrado con ID: {}", 
                            peerAddress, existingPeer.get().getPeerId());
                    continue;
                }
                
                // Registrar el peer bootstrap usando el servicio
                PeerResponseDto savedPeer = peerService.agregarPeer(ip, puerto);

                log.info("✓ Peer bootstrap registrado exitosamente: {} (ID: {})", 
                        peerAddress, savedPeer.getPeerId());
                
                // Intentar conectar al peer
                connectToPeer(savedPeer.getPeerId(), ip, puerto);
                
            } catch (NumberFormatException e) {
                log.error("Puerto inválido en peer bootstrap: {}", peerAddress);
            } catch (Exception e) {
                log.error("Error al registrar peer bootstrap {}: {}", peerAddress, e.getMessage());
            }
        }
    }
    
    private void initializeLocalPeerId() {
        // Obtener la IP real del servidor (no usar "localhost")
        String localIp = networkUtils.getServerIPAddress();

        // Si no se puede obtener la IP, usar "localhost" como fallback
        if (localIp == null || localIp.isEmpty() || "127.0.0.1".equals(localIp)) {
            log.warn("No se pudo obtener IP local, usando localhost");
            localIp = "localhost";
        }

        log.info("Inicializando peer local con IP: {} y puerto: {}", localIp, peerPort);

        // Obtener o crear el peer local usando el servicio
        PeerResponseDto localPeer = peerService.obtenerOCrearPeerLocal(localIp, peerPort);
        this.localPeerId = localPeer.getPeerId();
        log.info("Local Peer ID: {}", localPeerId);
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
                            peerSocket, gson, this, this::removePeerConnection,
                            requestDispatcher
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
        
        // Verificar si ya existe una conexión ENTRANTE activa con este peer
        if (activePeerConnections.containsKey(peerId)) {
            IPeerHandler handler = activePeerConnections.get(peerId);
            if (handler != null && handler.isConnected()) {
                log.debug("Ya existe una conexión entrante activa con peer {}", peerId);
                return;
            }
        }

        // Verificar si ya existe una conexión SALIENTE activa con este peer
        if (outgoingConnections.containsKey(peerId)) {
            PeerOutgoingConnection outgoing = outgoingConnections.get(peerId);
            if (outgoing != null && outgoing.isConnected()) {
                log.debug("Ya existe una conexión saliente activa con peer {}", peerId);
                return;
            } else {
                // La conexión saliente existe pero no está conectada, removerla
                outgoingConnections.remove(peerId);
                log.debug("Removiendo conexión saliente inactiva con peer {}", peerId);
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
        // Obtener peers usando el servicio
        List<PeerResponseDto> peers = peerService.listarPeersDisponibles();

        log.info("Conectando a {} peers conocidos en BD...", peers.size());

        if (peers.isEmpty() || peers.size() == 1) {
            log.warn("⚠️ No hay peers registrados en la base de datos.");
            log.info("Intentando conectar a peers de arranque (bootstrap)...");
            connectToBootstrapPeers();
            return;
        }

        int connectedCount = 0;
        int invalidCount = 0;

        for (PeerResponseDto peer : peers) {
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
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
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

                // Verificar si ya existe en BD usando el servicio
                Optional<PeerResponseDto> existingPeer = peerService.buscarPeerPorIpYPuerto(ip, port);

                UUID peerId;
                if (existingPeer.isPresent()) {
                    peerId = existingPeer.get().getPeerId();
                    log.info("Bootstrap peer {}:{} ya existe en BD con ID {}", ip, port, peerId);
                } else {
                    // Crear un peer temporal para conectar usando el servicio
                    PeerResponseDto saved = peerService.agregarPeer(ip, port);
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
        // Obtener peers usando el servicio
        List<PeerResponseDto> allPeers = peerService.listarPeersDisponibles();

        // FILTRAR: Solo intentar reconectar peers que están ONLINE en BD pero desconectados en memoria
        List<PeerResponseDto> offlinePeers = allPeers.stream()
            .filter(peer -> !peer.getPeerId().equals(localPeerId))
            .filter(peer -> "ONLINE".equals(peer.getConectado())) // SOLO peers marcados como ONLINE
            .filter(peer -> !isConnectedToPeer(peer.getPeerId()))
            .toList();

        if (!offlinePeers.isEmpty()) {
            log.debug("Intentando reconectar a {} peers ONLINE desconectados", offlinePeers.size());

            for (PeerResponseDto peer : offlinePeers) {
                connectToPeer(peer.getPeerId(), peer.getIp(), peer.getPuerto());
            }
        }
    }
    
    private void syncWithDatabase() {
        try {
            log.debug("━━━━━━━━━━━ INICIANDO SINCRONIZACIÓN CON BD ━━━━━━━━━━━");

            // Actualizar estado ONLINE para peers conectados
            Set<UUID> connectedPeerIds = getConnectedPeerIds();
            log.debug("Peers conectados en memoria: {}", connectedPeerIds.size());
            for (UUID id : connectedPeerIds) {
                log.debug("  - Peer conectado: {}", id);
            }

            // IMPORTANTE: Asegurar que el peer local SIEMPRE esté marcado como ONLINE
            connectedPeerIds.add(localPeerId);
            log.debug("Peer local agregado: {}", localPeerId);

            // Lista para rastrear peers que no existen en BD y deben ser limpiados
            Set<UUID> peersToRemove = new HashSet<>();

            log.debug("Reportando latidos para {} peers...", connectedPeerIds.size());
            for (UUID peerId : connectedPeerIds) {
                try {
                    peerService.reportarLatido(peerId);
                    log.trace("  ✓ Latido reportado para peer: {}", peerId);
                } catch (Exception e) {
                    // Si el peer no existe en BD, marcarlo para limpieza
                    if (e.getMessage() != null && e.getMessage().contains("Peer no encontrado")) {
                        log.warn("  ✗ Peer {} NO EXISTE EN BD - Marcado para limpieza", peerId);
                        log.debug("     └─ Mensaje de error: {}", e.getMessage());
                        peersToRemove.add(peerId);
                    } else {
                        log.warn("  ⚠ Error actualizando latido de peer {}: {}", peerId, e.getMessage());
                    }
                }
            }

            // Limpiar peers inexistentes de las colecciones en memoria
            if (!peersToRemove.isEmpty()) {
                log.info("━━━━━━━━━━━ LIMPIANDO {} PEERS INEXISTENTES ━━━━━━━━━━━", peersToRemove.size());

                for (UUID peerId : peersToRemove) {
                    log.info("Limpiando peer inexistente: {}", peerId);

                    // Remover de conexiones activas
                    IPeerHandler handler = activePeerConnections.remove(peerId);
                    if (handler != null) {
                        try {
                            log.debug("  ├─ Cerrando conexión ENTRANTE...");
                            handler.disconnect();
                            log.info("  ├─ ✓ Conexión entrante cerrada y removida");
                        } catch (Exception e) {
                            log.debug("  ├─ ⚠ Error cerrando conexión entrante: {}", e.getMessage());
                        }
                    } else {
                        log.debug("  ├─ No hay conexión entrante activa");
                    }

                    // Remover de conexiones salientes
                    PeerOutgoingConnection outgoing = outgoingConnections.remove(peerId);
                    if (outgoing != null) {
                        try {
                            log.debug("  ├─ Cerrando conexión SALIENTE...");
                            outgoing.disconnect();
                            log.info("  └─ ✓ Conexión saliente cerrada y removida");
                        } catch (Exception e) {
                            log.debug("  └─ ⚠ Error cerrando conexión saliente: {}", e.getMessage());
                        }
                    } else {
                        log.debug("  └─ No hay conexión saliente activa");
                    }

                    log.info("Peer inexistente {} completamente removido de memoria", peerId);
                }

                log.info("━━━━━━━━━━━ LIMPIEZA COMPLETADA ━━━━━━━━━━━");
            } else {
                log.trace("No hay peers inexistentes para limpiar");
            }

            // Marcar peers desconectados como OFFLINE (excepto el local)
            log.debug("Verificando peers en BD para marcar OFFLINE...");
            List<PeerResponseDto> allPeers = peerService.listarPeersDisponibles();
            log.debug("Total peers en BD: {}", allPeers.size());

            int offlineCount = 0;
            for (PeerResponseDto peer : allPeers) {
                // NO marcar el peer local como offline
                if (peer.getPeerId().equals(localPeerId)) {
                    log.trace("  - Peer local {} - SKIP", peer.getPeerId());
                    continue;
                }

                if (!connectedPeerIds.contains(peer.getPeerId())) {
                    if ("ONLINE".equals(peer.getConectado())) {
                        log.debug("  - Peer {} está ONLINE en BD pero desconectado - marcando OFFLINE", peer.getPeerId());
                        peerService.marcarPeerComoDesconectado(peer.getPeerId());
                        log.info("Peer {} marcado como OFFLINE en BD", peer.getPeerId());
                        offlineCount++;
                    } else {
                        log.trace("  - Peer {} ya está OFFLINE", peer.getPeerId());
                    }
                } else {
                    log.trace("  - Peer {} está conectado y ONLINE", peer.getPeerId());
                }
            }

            if (offlineCount > 0) {
                log.info("Total peers marcados como OFFLINE: {}", offlineCount);
            }

            log.debug("━━━━━━━━━━━ SINCRONIZACIÓN COMPLETADA ━━━━━━━━━━━");
            log.info("Resumen: {} peers conectados, {} limpiados, {} marcados offline",
                    connectedPeerIds.size() - 1, peersToRemove.size(), offlineCount); // -1 para excluir local

        } catch (Exception e) {
            log.error("━━━━━━━━━━━ ERROR EN SINCRONIZACIÓN ━━━━━━━━━━━");
            log.error("Error sincronizando con base de datos: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Callback cuando un peer completa el handshake
     */
    public void onPeerAuthenticated(IPeerHandler handler) {
        UUID peerId = handler.getPeerId();

        // Verificar si este peer ya está conectado (evitar duplicados)
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
        
        // Publicar evento
        eventPublisher.publishEvent(new PeerConnectedEvent(
            peerId, handler.getPeerIp(), handler.getPeerPort()
        ));
        
        // Registrar o actualizar el peer usando el servicio de la capa de lógica
        try {
            PeerResponseDto peerDto = peerService.registrarPeerAutenticado(
                peerId,
                handler.getPeerIp(),
                handler.getPeerPort()
            );

            log.info("✓ Peer {} guardado/actualizado en BD ({}:{})",
                    peerId, peerDto.getIp(), peerDto.getPuerto());
        } catch (Exception e) {
            log.error("Error al registrar peer autenticado: {}", e.getMessage(), e);
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
            
            // Marcar como desconectado usando el servicio
            peerService.marcarPeerComoDesconectado(peerId);
        }
    }
    
    // --- MÉTODOS PÚBLICOS PARA PROCESAMIENTO DE MENSAJES ---
    
    public void processPeerRequest(IPeerHandler handler, DTORequest request) {
        log.info("Procesando request P2P '{}' de peer {}", request.getAction(), handler.getPeerId());
        
        // Delegar al RequestDispatcher para procesar la petición
        // El RequestDispatcher tiene todos los controladores (PeerController, UserController, etc.)
        try {
            String requestJson = gson.toJson(request);
            // Cast explícito a IClientHandler ya que IPeerHandler extiende IClientHandler
            requestDispatcher.dispatch(requestJson, (IClientHandler) handler);
            log.debug("Request P2P '{}' procesado exitosamente", request.getAction());
        } catch (Exception e) {
            log.error("Error procesando request P2P '{}': {}", request.getAction(), e.getMessage(), e);
            // Enviar respuesta de error
            DTOResponse response = new DTOResponse(
                request.getAction(),
                "error",
                "Error procesando request: " + e.getMessage(),
                null
            );
            handler.sendMessage(gson.toJson(response));
        }
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
     * Marca un peer como OFFLINE después de fallos consecutivos de conexión
     */
    public void markPeerAsOfflineAfterFailure(UUID peerId) {
        try {
            log.info("Marcando peer {} como OFFLINE tras fallos consecutivos de conexión", peerId);
            peerService.marcarPeerComoDesconectado(peerId);
        } catch (Exception e) {
            log.warn("Error al marcar peer {} como OFFLINE: {}", peerId, e.getMessage());
        }
    }

    /**
     * Registra peers descubiertos desde otro peer en la base de datos.
     * No conecta automáticamente, solo actualiza la tabla de peers.
     */
    public void registerDiscoveredPeers(Object payload) {
        if (payload == null) {
            log.debug("registerDiscoveredPeers: payload nulo, nada que registrar");
            return;
        }

        try {
            var json = gson.toJsonTree(payload).getAsJsonObject();
            if (!json.has("peersDisponibles")) {
                log.debug("registerDiscoveredPeers: no contiene 'peersDisponibles'");
                return;
            }

            var arr = json.getAsJsonArray("peersDisponibles");
            int added = 0;
            int updated = 0;

            log.info("Procesando {} peers descubiertos...", arr.size());

            for (var elem : arr) {
                var obj = elem.getAsJsonObject();

                String ip = obj.has("ip") && !obj.get("ip").isJsonNull() ? obj.get("ip").getAsString() : null;
                int puerto = obj.has("puerto") && !obj.get("puerto").isJsonNull() ? obj.get("puerto").getAsInt() : -1;

                if (ip == null || ip.trim().isEmpty() || puerto <= 0 || puerto > 65535) {
                    log.debug("Peer descubierto inválido (ip/puerto): {}/{}", ip, puerto);
                    continue;
                }

                try {
                    var existing = peerService.buscarPeerPorIpYPuerto(ip, puerto);
                    if (existing.isPresent()) {
                        // Ya existe, podrías actualizar datos si es necesario
                        log.debug("Peer descubierto ya existe en BD: {}:{}", ip, puerto);
                        updated++;
                    } else {
                        // Nuevo peer, agregar a la BD
                        peerService.agregarPeer(ip, puerto);
                        added++;
                        log.info("✓ Nuevo peer registrado en BD: {}:{}", ip, puerto);
                    }
                } catch (Exception e) {
                    log.warn("Error registrando peer descubierto {}:{} -> {}", ip, puerto, e.getMessage());
                }
            }

            if (added > 0 || updated > 0) {
                log.info("Descubrimiento completado: {} nuevos, {} existentes", added, updated);

                // NUEVO: Después de registrar peers, intentar conectar a los nuevos
                if (added > 0) {
                    log.info("Esperando 1 segundo antes de conectar a {} peers nuevos...", added);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    // Conectar a todos los peers conocidos (incluidos los nuevos)
                    connectToAllKnownPeers();
                }
            } else {
                log.debug("No se registraron peers nuevos desde descubrimiento");
            }

        } catch (Exception e) {
            log.warn("Error procesando payload de descubrimiento: {}", e.getMessage());
        }
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
