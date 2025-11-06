package com.arquitectura.transporte;

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

    @Override
    public void run() {
        int attempts = 0;
        while (running) {
            try {
                log.info("Intentando conexión saliente a {}:{} (peer {})", ip, port, peerId);
                socket = new Socket(ip, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;
                attempts = 0;
                log.info("✓ Conexión saliente establecida con peer {} ({}:{})", peerId, ip, port);

                // Bucle de lectura para mantener la conexión viva y detectar desconexiones
                String line;
                while (running && (line = in.readLine()) != null) {
                    // Aquí se pueden procesar mensajes recibidos del peer
                    log.debug("Recibido de peer {}: {}", peerId, line);
                }

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
            } finally {
                connected = false;
                closeSilently();
            }
        }
        log.info("Conexión saliente a peer {} terminada", peerId);
    }

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

