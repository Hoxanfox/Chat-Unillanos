package com.arquitectura.transporte;

import com.arquitectura.controlador.IPeerHandler;
import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Maneja una conexión entrante de otro peer (otro servidor se conectó a este)
 */
public class PeerHandler implements IPeerHandler, Runnable {

    private static final Logger log = LoggerFactory.getLogger(PeerHandler.class);

    private final Socket socket;
    private final Gson gson;
    private final PeerConnectionManager connectionManager;
    private final Consumer<IPeerHandler> onDisconnect;

    private BufferedReader in;
    private PrintWriter out;

    private UUID peerId;
    private String peerIp;
    private Integer peerPort;
    private volatile boolean connected = false;
    private volatile boolean authenticated = false;
    private volatile long lastHeartbeat;

    public PeerHandler(Socket socket, Gson gson, PeerConnectionManager connectionManager,
                       Consumer<IPeerHandler> onDisconnect) {
        this.socket = socket;
        this.gson = gson;
        this.connectionManager = connectionManager;
        this.onDisconnect = onDisconnect;
        this.peerIp = socket.getInetAddress().getHostAddress();
        this.lastHeartbeat = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;

            log.info("PeerHandler iniciado para conexión desde {}", peerIp);

            String line;
            while (connected && (line = in.readLine()) != null) {
                updateHeartbeat();
                processMessage(line);
            }

        } catch (IOException e) {
            if (connected) {
                log.error("Error en PeerHandler para peer {}: {}", peerId, e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    private void processMessage(String message) {
        try {
            DTORequest request = gson.fromJson(message, DTORequest.class);

            if (request == null || request.getAction() == null) {
                log.warn("Mensaje inválido recibido de peer {}: {}", peerId, message);
                return;
            }

            log.debug("Mensaje recibido de peer {}: action={}", peerId, request.getAction());

            switch (request.getAction()) {
                case "peer_handshake":
                    handleHandshake(request);
                    break;

                case "heartbeat":
                    handleHeartbeat(request);
                    break;

                case "retransmit":
                    connectionManager.handleRetransmitRequest(this, request);
                    break;

                case "sync":
                    connectionManager.handleSyncRequest(this, request);
                    break;

                default:
                    connectionManager.processPeerRequest(this, request);
                    break;
            }

        } catch (JsonSyntaxException e) {
            log.error("Error parseando mensaje JSON de peer {}: {}", peerId, e.getMessage());
        } catch (Exception e) {
            log.error("Error procesando mensaje de peer {}: {}", peerId, e.getMessage(), e);
        }
    }

    private void handleHandshake(DTORequest request) {
        try {
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) request.getPayload();

            if (data == null || !data.containsKey("peerId")) {
                log.warn("Handshake inválido de {}: falta peerId", peerIp);
                forceDisconnect();
                return;
            }

            this.peerId = UUID.fromString(data.get("peerId").toString());

            if (data.containsKey("port")) {
                this.peerPort = ((Number) data.get("port")).intValue();
            }

            this.authenticated = true;

            log.info("✓ Handshake completado con peer {} ({}:{})", peerId, peerIp, peerPort);

            // Notificar al manager que el peer está autenticado
            connectionManager.onPeerAuthenticated(this);

            // Enviar respuesta de handshake
            var response = new com.arquitectura.DTO.Comunicacion.DTOResponse(
                "peer_handshake",
                "success",
                "Handshake completado",
                connectionManager.getLocalPeerInfo()
            );
            sendMessage(gson.toJson(response));

        } catch (Exception e) {
            log.error("Error procesando handshake: {}", e.getMessage(), e);
            forceDisconnect();
        }
    }

    private void handleHeartbeat(DTORequest request) {
        updateHeartbeat();
        log.debug("Heartbeat recibido de peer {}", peerId);

        // Responder con un heartbeat_ack
        var response = new com.arquitectura.DTO.Comunicacion.DTOResponse(
            "heartbeat_ack",
            "success",
            "Heartbeat recibido",
            null
        );
        sendMessage(gson.toJson(response));
    }

    @Override
    public void sendMessage(String message) {
        if (connected && out != null) {
            out.println(message);
            out.flush();
        } else {
            log.warn("Intento de enviar mensaje a peer {} desconectado", peerId);
        }
    }

    @Override
    public void disconnect() {
        if (!connected) {
            return;
        }

        connected = false;
        authenticated = false;

        log.info("Desconectando peer {}", peerId);

        closeSilently();

        // Notificar al callback de desconexión
        if (onDisconnect != null) {
            onDisconnect.accept(this);
        }
    }

    @Override
    public void forceDisconnect() {
        log.warn("Forzando desconexión de peer {}", peerId);
        disconnect();
    }

    private void closeSilently() {
        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception ignored) {}

        try {
            if (in != null) {
                in.close();
            }
        } catch (Exception ignored) {}

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public boolean isAuthenticated() {
        return authenticated;
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
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    @Override
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }
}
