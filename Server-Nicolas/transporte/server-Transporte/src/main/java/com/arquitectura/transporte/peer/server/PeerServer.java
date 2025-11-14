package com.arquitectura.transporte.peer.server;

import com.arquitectura.controlador.RequestDispatcher;
import com.arquitectura.transporte.peer.PeerConnectionManager;
import com.arquitectura.transporte.peer.PeerHandler;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Componente responsable de escuchar el puerto P2P y aceptar conexiones entrantes.
 */
public class PeerServer {
    private static final Logger log = LoggerFactory.getLogger(PeerServer.class);

    private final int peerPort;
    private final int maxPeerConnections;
    private final ExecutorService peerPool;
    private final Gson gson;
    private final PeerConnectionManager manager;
    private final RequestDispatcher requestDispatcher;
    private final Supplier<Boolean> runningSupplier;

    public PeerServer(int peerPort,
                      int maxPeerConnections,
                      ExecutorService peerPool,
                      Gson gson,
                      PeerConnectionManager manager,
                      RequestDispatcher requestDispatcher,
                      Supplier<Boolean> runningSupplier) {
        this.peerPort = peerPort;
        this.maxPeerConnections = maxPeerConnections;
        this.peerPool = peerPool;
        this.gson = gson;
        this.manager = manager;
        this.requestDispatcher = requestDispatcher;
        this.runningSupplier = runningSupplier;
    }

    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(peerPort)) {
                log.info("Servidor P2P iniciado en puerto {}", peerPort);

                while (runningSupplier.get()) {
                    try {
                        Socket peerSocket = serverSocket.accept();

                        if (manager.getActivePeerCount() >= maxPeerConnections) {
                            log.warn("Conexión P2P rechazada de {}. Límite de {} peers alcanzado.",
                                    peerSocket.getInetAddress().getHostAddress(), maxPeerConnections);

                            try (PrintWriter out = new PrintWriter(peerSocket.getOutputStream(), true)) {
                                out.println("{\"action\":\"connect\",\"status\":\"error\",\"message\":\"El servidor ha alcanzado su capacidad máxima de peers.\"}");
                            }
                            peerSocket.close();
                            continue;
                        }

                        log.info("Nueva conexión P2P entrante desde: {}", peerSocket.getInetAddress().getHostAddress());

                        PeerHandler peerHandler = new PeerHandler(
                                peerSocket, gson, manager, manager::removePeerConnection,
                                requestDispatcher
                        );
                        peerPool.submit(peerHandler);

                    } catch (IOException e) {
                        if (runningSupplier.get()) {
                            log.error("Error aceptando conexión P2P: {}", e.getMessage(), e);
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Error fatal al iniciar servidor P2P: {}", e.getMessage(), e);
            }
        }, "PeerServerListener").start();
    }

    public void stop() {
        log.info("PeerServer stop() called");
    }
}

