package com.arquitectura.transporte;

// Importaciones necesarias
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
import java.util.UUID;

/**
 * Maneja conexiones salientes a otros peers (este servidor inicia la conexión)
 */
public class PeerOutgoingConnection implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PeerOutgoingConnection.class);

    private final UUID peerId;
    private final String ip;
    private final int port;
    private final Gson gson;
    private final PeerConnectionManager manager;
    private final int maxReconnectAttempts;
    private final long reconnectDelayMs;

    private volatile boolean connected = false;
    private volatile boolean running = true;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // ==================================================================
    // NUEVA VARIABLE PARA EL HEARTBEAT ACTIVO
    // ==================================================================
    private long lastPingTime = 0;
    private final long PING_INTERVAL_MS = 30000; // 30 segundos

    public PeerOutgoingConnection(UUID peerId, String ip, int port, Gson gson,
                                  PeerConnectionManager manager, int reconnectAttempts, long reconnectDelayMs) {
        this.peerId = peerId;
        this.ip = ip;
        this.port = port;
        this.gson = gson;
        this.manager = manager;
        this.maxReconnectAttempts = reconnectAttempts;
        this.reconnectDelayMs = reconnectDelayMs;
    }

    // ==================================================================
    // MÉTODO 'run()' CORREGIDO CON HANDSHAKE Y KEEP-ALIVE
    // ==================================================================
    @Override
    public void run() {
        int attempts = 0;
        while (running) {
            try {
                log.info("Intentando conexión saliente a {}:{} (peer {})", ip, port, peerId);
                socket = new Socket(ip, port);

                // 1. Establecer un timeout para la lectura del handshake (10 segundos)
                socket.setSoTimeout(10000);

                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                log.debug("Socket conectado. Enviando handshake a peer {}", peerId);

                // 2. Crear y enviar la petición de handshake
                DTORequest handshakeRequest = new DTORequest(
                        "peer_handshake",
                        manager.getLocalPeerInfo()
                );
                String requestJson = gson.toJson(handshakeRequest);
                out.println(requestJson);

                // 3. Esperar la respuesta del handshake
                String responseJson = in.readLine();

                if (responseJson == null) {
                    throw new IOException("Conexión cerrada por el peer durante el handshake.");
                }

                // 4. Validar la respuesta
                DTOResponse response = gson.fromJson(responseJson, DTOResponse.class);
                if (response == null || !"peer_handshake".equals(response.getAction()) || !"success".equals(response.getStatus())) {
                    String errorMsg = response != null ? response.getMessage() : "respuesta invalida";
                    throw new IOException("Handshake fallido: " + errorMsg);
                }

                log.info("✓ Handshake completado con peer {} ({}:{})", peerId, ip, port);

                // 5. SOLO AHORA la conexión se marca como 'conectada'
                connected = true;
                attempts = 0; // Reiniciar intentos

                // 6. Quitar el timeout para el bucle de lectura/escritura normal
                socket.setSoTimeout(0);

                // --- INICIO DE LA SOLUCIÓN DEL BUCLE DE TIMEOUT ---

                lastPingTime = System.currentTimeMillis(); // Inicializar contador de ping

                // Bucle de lectura Y escritura para mantener la conexión viva
                while (running) {

                    // 1. Revisar si hay datos para leer (no bloqueante)
                    if (in.ready()) {
                        String line = in.readLine();
                        if (line == null) {
                            throw new IOException("Peer " + peerId + " cerró la conexión");
                        }
                        log.debug("Recibido de peer {}: {}", peerId, line);
                        // Aquí puedes procesar respuestas (ej. "heartbeat_ack") si lo deseas
                    }

                    // 2. Enviar un ping/heartbeat si ha pasado el tiempo
                    long now = System.currentTimeMillis();
                    if (now - lastPingTime > PING_INTERVAL_MS) {
                        // Usamos la acción "heartbeat" que tu PeerHandler ya entiende
                        DTORequest heartbeatRequest = new DTORequest("heartbeat", manager.getLocalPeerInfo());
                        sendMessage(gson.toJson(heartbeatRequest));
                        lastPingTime = now;
                        log.debug("Ping/Heartbeat enviado a peer {}", peerId);
                    }

                    // 3. Dormir un poco para no consumir 100% de CPU
                    Thread.sleep(100);
                }
                // --- FIN DE LA SOLUCIÓN DEL BUCLE DE TIMEOUT ---

            } catch (IOException e) {
                connected = false;
                closeSilently();
                attempts++;
                log.warn("Conexión a peer {} falló (intento {}): {}", peerId, attempts, e.getMessage());

                if (maxReconnectAttempts > 0 && attempts >= maxReconnectAttempts) {
                    log.warn("Máximo de intentos de reconexión alcanzado para peer {}", peerId);
                    break;
                }

                // Esperar antes de reintentar
                try {
                    Thread.sleep(Math.max(1000, reconnectDelayMs));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (InterruptedException e) {
                // Manejar la interrupción del Thread.sleep()
                log.info("Hilo de conexión saliente interrumpido.");
                Thread.currentThread().interrupt();
                running = false;
            } finally {
                connected = false;
                closeSilently();
            }
        }
        log.info("Conexión saliente a peer {} terminada", peerId);
    }
    // ==================================================================
    // FIN DEL MÉTODO CORREGIDO
    // ==================================================================


    /**
     * Verifica si la conexión está activa
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    /**
     * Envía un mensaje al peer remoto
     */
    public synchronized void sendMessage(String message) {
        if (!connected || out == null) {
            log.warn("No se puede enviar mensaje a peer {}: no conectado", peerId);
            return;
        }
        out.println(message);
        out.flush();
    }

    /**
     * Desconecta la conexión saliente
     */
    public void disconnect() {
        running = false;
        connected = false;
        closeSilently();
    }

    /**
     * Cierra todos los recursos de forma segura
     */
    private void closeSilently() {
        try {
            if (out != null) {
                out.close();
                out = null;
            }
        } catch (Exception ignored) {}

        try {
            if (in != null) {
                in.close();
                in = null;
            }
        } catch (Exception ignored) {}

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            socket = null;
        } catch (Exception ignored) {}
    }

    public UUID getPeerId() {
        return peerId;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}