package com.arquitectura.transporte;

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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Maneja una conexión saliente a otro peer (este servidor inicia la conexión)
 */
public class PeerOutgoingConnection implements Runnable {
    
    private static final Logger log = LoggerFactory.getLogger(PeerOutgoingConnection.class);
    
    private final UUID targetPeerId;
    private final String targetIp;
    private final int targetPort;
    private final Gson gson;
    private final PeerConnectionManager connectionManager;
    private final int maxReconnectAttempts;
    private final long reconnectDelayMs;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private int reconnectAttempt = 0;
    
    public PeerOutgoingConnection(UUID targetPeerId, String targetIp, int targetPort,
                                  Gson gson, PeerConnectionManager connectionManager,
                                  int maxReconnectAttempts, long reconnectDelayMs) {
        this.targetPeerId = targetPeerId;
        this.targetIp = targetIp;
        this.targetPort = targetPort;
        this.gson = gson;
        this.connectionManager = connectionManager;
        this.maxReconnectAttempts = maxReconnectAttempts;
        this.reconnectDelayMs = reconnectDelayMs;
    }
    
    @Override
    public void run() {
        while (running.get() && reconnectAttempt < maxReconnectAttempts) {
            try {
                connect();
                if (connected.get()) {
                    handleCommunication();
                }
            } catch (Exception e) {
                log.error("Error en conexión saliente a peer {} ({}:{}): {}", 
                         targetPeerId, targetIp, targetPort, e.getMessage());
            }
            
            if (running.get() && !connected.get()) {
                reconnectAttempt++;
                if (reconnectAttempt < maxReconnectAttempts) {
                    log.info("Reintentando conexión a peer {} en {} ms (intento {}/{})", 
                            targetPeerId, reconnectDelayMs, reconnectAttempt, maxReconnectAttempts);
                    try {
                        Thread.sleep(reconnectDelayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.warn("Máximo de reintentos alcanzado para peer {}", targetPeerId);
                }
            }
        }
    }
    
    private void connect() throws IOException {
        log.info("Conectando a peer {} ({}:{})...", targetPeerId, targetIp, targetPort);
        
        socket = new Socket(targetIp, targetPort);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        connected.set(true);
        reconnectAttempt = 0;
        
        log.info("Conexión establecida con peer {} ({}:{})", targetPeerId, targetIp, targetPort);
        
        // Enviar handshake
        sendHandshake();
    }
    
    private void sendHandshake() {
        Map<String, Object> handshakeData = connectionManager.getLocalPeerInfo();
        
        DTORequest handshake = new DTORequest(
            "peer_handshake",
            handshakeData
        );
        
        String message = gson.toJson(handshake);
        out.println(message);
        
        log.info("Handshake enviado a peer {}", targetPeerId);
    }
    
    private void handleCommunication() {
        try {
            String line;
            while (running.get() && connected.get() && (line = in.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                log.debug("Mensaje recibido de peer {}: {}", targetPeerId,
                         line.length() > 100 ? line.substring(0, 100) + "..." : line);
                
                try {
                    processIncomingMessage(line);
                } catch (Exception e) {
                    log.error("Error procesando mensaje de peer {}: {}", targetPeerId, e.getMessage(), e);
                }
            }
        } catch (SocketException e) {
            log.warn("Conexión cerrada con peer {}: {}", targetPeerId, e.getMessage());
        } catch (IOException e) {
            log.error("Error de I/O con peer {}: {}", targetPeerId, e.getMessage());
        } finally {
            connected.set(false);
            closeResources();
        }
    }
    
    private void processIncomingMessage(String jsonMessage) {
        try {
            DTOResponse response = gson.fromJson(jsonMessage, DTOResponse.class);
            
            if (response == null || response.getAction() == null) {
                log.warn("Respuesta inválida de peer {}", targetPeerId);
                return;
            }
            
            log.debug("Procesando respuesta '{}' de peer {}", response.getAction(), targetPeerId);
            
            // Manejar respuestas específicas
            switch (response.getAction()) {
                case "peer_handshake":
                    handleHandshakeResponse(response);
                    break;
                    
                case "peer_heartbeat":
                    log.debug("Heartbeat ACK de peer {}", targetPeerId);
                    break;
                    
                default:
                    log.debug("Respuesta '{}' de peer {}: {}", 
                             response.getAction(), targetPeerId, response.getMessage());
                    break;
            }
            
        } catch (Exception e) {
            log.error("Error parseando respuesta de peer {}: {}", targetPeerId, e.getMessage());
        }
    }
    
    private void handleHandshakeResponse(DTOResponse response) {
        if ("success".equals(response.getStatus())) {
            log.info("Handshake confirmado con peer {}", targetPeerId);
        } else {
            log.warn("Handshake rechazado por peer {}: {}", targetPeerId, response.getMessage());
            disconnect();
        }
    }
    
    public void sendMessage(String message) {
        if (out != null && connected.get() && !socket.isClosed()) {
            out.println(message);
            log.debug("Mensaje enviado a peer {}: {} bytes", targetPeerId, message.length());
        } else {
            log.warn("Intento de enviar mensaje a peer desconectado: {}", targetPeerId);
        }
    }
    
    public void sendHeartbeat() {
        if (!connected.get()) {
            return;
        }
        
        DTORequest heartbeat = new DTORequest("peer_heartbeat", Map.of(
            "peerId", connectionManager.getLocalPeerId().toString(),
            "timestamp", System.currentTimeMillis()
        ));
        
        sendMessage(gson.toJson(heartbeat));
    }
    
    public void disconnect() {
        if (running.compareAndSet(true, false)) {
            log.info("Cerrando conexión saliente con peer {} ({}:{})", 
                    targetPeerId, targetIp, targetPort);
            connected.set(false);
            closeResources();
        }
    }
    
    private void closeResources() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            log.error("Error cerrando recursos de conexión con peer {}: {}", 
                     targetPeerId, e.getMessage());
        }
    }
    
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed();
    }
    
    public UUID getTargetPeerId() {
        return targetPeerId;
    }
    
    public String getTargetIp() {
        return targetIp;
    }
    
    public int getTargetPort() {
        return targetPort;
    }
}
