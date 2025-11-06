package com.arquitectura.transporte;

import com.arquitectura.controlador.IPeerHandler;
import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Maneja una conexión P2P individual con otro servidor
 * Similar a ClientHandler pero para comunicación peer-to-peer
 */
public class PeerHandler implements IPeerHandler, Runnable {
    
    private static final Logger log = LoggerFactory.getLogger(PeerHandler.class);
    
    private final Socket socket;
    private final Gson gson;
    private final PeerConnectionManager connectionManager;
    private final Consumer<IPeerHandler> onDisconnect;
    
    private PrintWriter out;
    private BufferedReader in;
    
    private UUID peerId;
    private String peerIp;
    private Integer peerPort;
    private long lastHeartbeat;
    
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean authenticated = new AtomicBoolean(false);
    
    public PeerHandler(Socket socket, Gson gson, PeerConnectionManager connectionManager, 
                       Consumer<IPeerHandler> onDisconnect) {
        this.socket = socket;
        this.gson = gson;
        this.connectionManager = connectionManager;
        this.onDisconnect = onDisconnect;
        this.peerIp = socket.getInetAddress().getHostAddress();
        this.lastHeartbeat = System.currentTimeMillis();
        
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            log.info("PeerHandler inicializado para peer desde IP: {}", peerIp);
        } catch (IOException e) {
            log.error("Error al inicializar streams para peer {}: {}", peerIp, e.getMessage());
            disconnect();
        }
    }
    
    @Override
    public void run() {
        log.info("Iniciando hilo de comunicación con peer desde {}", peerIp);
        
        try {
            String line;
            while (running.get() && (line = in.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                log.debug("Mensaje recibido de peer {}: {}", peerId != null ? peerId : peerIp, 
                         line.length() > 100 ? line.substring(0, 100) + "..." : line);
                
                try {
                    processMessage(line);
                } catch (Exception e) {
                    log.error("Error procesando mensaje de peer {}: {}", 
                             peerId != null ? peerId : peerIp, e.getMessage(), e);
                    sendErrorResponse("Error procesando mensaje: " + e.getMessage());
                }
            }
        } catch (SocketException e) {
            log.warn("Conexión cerrada con peer {}: {}", peerId != null ? peerId : peerIp, e.getMessage());
        } catch (IOException e) {
            log.error("Error de I/O con peer {}: {}", peerId != null ? peerId : peerIp, e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    private void processMessage(String jsonMessage) {
        try {
            DTORequest request = gson.fromJson(jsonMessage, DTORequest.class);
            
            if (request == null || request.getAction() == null) {
                log.warn("Mensaje inválido recibido de peer {}", peerIp);
                sendErrorResponse("Formato de mensaje inválido");
                return;
            }
            
            log.info("Procesando acción '{}' de peer {}", request.getAction(), 
                    peerId != null ? peerId : peerIp);
            
            // Manejar acciones específicas de P2P
            switch (request.getAction()) {
                case "peer_handshake":
                    handleHandshake(request);
                    break;
                    
                case "peer_heartbeat":
                    handleHeartbeat(request);
                    break;
                    
                case "peer_retransmit":
                    handleRetransmit(request);
                    break;
                    
                case "peer_sync":
                    handleSync(request);
                    break;
                    
                default:
                    // Delegar al connection manager para procesar
                    connectionManager.processPeerRequest(this, request);
                    break;
            }
            
        } catch (Exception e) {
            log.error("Error parseando mensaje JSON de peer {}: {}", peerIp, e.getMessage());
            sendErrorResponse("Error parseando mensaje");
        }
    }
    
    private void handleHandshake(DTORequest request) {
        try {
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) request.getPayload();

            String peerIdStr = (String) data.get("peerId");
            String ip = (String) data.get("ip");
            Object portObj = data.get("port");
            
            if (peerIdStr == null || ip == null || portObj == null) {
                log.warn("Handshake incompleto de peer {}", peerIp);
                sendErrorResponse("Datos de handshake incompletos");
                return;
            }
            
            this.peerId = UUID.fromString(peerIdStr);
            this.peerPort = portObj instanceof Double ? ((Double) portObj).intValue() : (Integer) portObj;
            this.authenticated.set(true);
            
            log.info("Handshake exitoso con peer ID: {} ({}:{})", peerId, ip, peerPort);
            
            // Enviar respuesta de handshake
            DTOResponse response = new DTOResponse(
                "peer_handshake", 
                "success", 
                "Handshake aceptado",
                connectionManager.getLocalPeerInfo()
            );
            sendMessage(gson.toJson(response));
            
            // Notificar al connection manager
            connectionManager.onPeerAuthenticated(this);
            
        } catch (Exception e) {
            log.error("Error en handshake con peer {}: {}", peerIp, e.getMessage());
            sendErrorResponse("Error en handshake: " + e.getMessage());
        }
    }
    
    private void handleHeartbeat(DTORequest request) {
        updateHeartbeat();
        log.debug("Heartbeat recibido de peer {}", peerId);
        
        DTOResponse response = new DTOResponse(
            "peer_heartbeat", 
            "success", 
            "Heartbeat acknowledged",
            System.currentTimeMillis()
        );
        sendMessage(gson.toJson(response));
    }
    
    private void handleRetransmit(DTORequest request) {
        log.info("Petición de retransmisión recibida de peer {}", peerId);
        // La lógica de retransmisión se delega al connection manager
        connectionManager.handleRetransmitRequest(this, request);
    }
    
    private void handleSync(DTORequest request) {
        log.info("Petición de sincronización recibida de peer {}", peerId);
        // La lógica de sincronización se delega al connection manager
        connectionManager.handleSyncRequest(this, request);
    }
    
    @Override
    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
            log.debug("Mensaje enviado a peer {}: {} bytes", 
                     peerId != null ? peerId : peerIp, message.length());
        } else {
            log.warn("Intento de enviar mensaje a peer desconectado: {}", peerId);
        }
    }
    
    private void sendErrorResponse(String errorMessage) {
        DTOResponse response = new DTOResponse("error", "error", errorMessage, null);
        sendMessage(gson.toJson(response));
    }
    
    @Override
    public void disconnect() {
        if (running.compareAndSet(true, false)) {
            log.info("Cerrando conexión con peer {} ({}:{})", 
                    peerId != null ? peerId : "desconocido", peerIp, peerPort);
            
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                log.error("Error cerrando recursos de peer {}: {}", peerId, e.getMessage());
            }
            
            if (onDisconnect != null) {
                onDisconnect.accept(this);
            }
        }
    }
    
    @Override
    public void forceDisconnect() {
        log.warn("Forzando desconexión de peer {}", peerId);
        disconnect();
    }
    
    @Override
    public UUID getPeerId() {
        return peerId;
    }
    
    @Override
    public String getPeerIp() {
        return peerIp;
    }
    
    @Override
    public Integer getPeerPort() {
        return peerPort;
    }
    
    @Override
    public boolean isConnected() {
        return running.get() && socket != null && !socket.isClosed() && authenticated.get();
    }
    
    @Override
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }
    
    @Override
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }
    
    public boolean isAuthenticated() {
        return authenticated.get();
    }
}

