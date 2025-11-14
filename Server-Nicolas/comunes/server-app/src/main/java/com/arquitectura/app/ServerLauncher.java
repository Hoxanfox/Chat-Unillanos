package com.arquitectura.app;

import com.arquitectura.controlador.ServerViewController;
import com.arquitectura.utils.mail.MailConfig;
import com.arquitectura.configdb.ConfiguracionPersistencia;
import com.arquitectura.transporte.server.ServerListener;
import com.arquitectura.transporte.peer.PeerConnectionManager;
import com.arquitectura.vista.ServerMainWindow;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerLauncher {

    private static final Logger log = LoggerFactory.getLogger(ServerLauncher.class);

    public static void main(String[] args) {
        log.info("Starting ServerLauncher...");

        // 1. Crear el contexto de Spring a partir de nuestras clases de configuración.
        // ApplicationConfig es clave aquí porque tiene la anotación @ComponentScan("com.arquitectura"),
        // que le dice a Spring que busque todos los @Component, @Service, @Controller, etc.
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            ConfiguracionPersistencia.class,
            MailConfig.class,
            ApplicationConfig.class
        );

        log.debug("Spring context initialized with provided configuration classes.");

        // Asegurarse de que el contexto se cierre correctamente al terminar la aplicación.
        context.registerShutdownHook();
        log.debug("Registered shutdown hook for Spring context.");

        // 2. Obtener los beans principales que necesitamos para arrancar.
        log.debug("Retrieving required beans from Spring context: ServerListener, PeerConnectionManager, ServerViewController");
        ServerListener serverListener = context.getBean(ServerListener.class);
        PeerConnectionManager peerConnectionManager = context.getBean(PeerConnectionManager.class);
        ServerViewController viewController = context.getBean(ServerViewController.class);

        log.info("Beans obtained: serverListener={}, peerConnectionManager={}, viewController={}",
                serverListener.getClass().getSimpleName(),
                peerConnectionManager.getClass().getSimpleName(),
                viewController.getClass().getSimpleName());

        // --------------------------------------------------------
        // P2P: arrancar primero para que lea server.properties, verifique BD y se una/registre en la red P2P
        // --------------------------------------------------------
        Thread peerServerThread = new Thread(() -> {
            try {
                log.info("Peer server thread starting...");
                // Inicia el servidor P2P (escucha conexiones entrantes)
                peerConnectionManager.startPeerServer();

                log.info("Peer server started. Ejecutando inicialización de peers en startup (BD / bootstrap)...");
                // Inicializa peers en startup: comprobar BD, intentar bootstrap si es necesario, registrar peers descubiertos
                peerConnectionManager.initializePeersOnStartup();

                log.info("Peer server thread finished initialization.");
            } catch (Throwable t) {
                log.error("Unhandled error in peer server thread", t);
            }
        });
        peerServerThread.setDaemon(true);
        try {
            peerServerThread.start();
            log.debug("Peer server thread started (daemon={}).", peerServerThread.isDaemon());
        } catch (Exception e) {
            log.error("Failed to start peer server thread", e);
        }

        // 3. Iniciar el servidor de sockets para clientes en un hilo separado (después de P2P)
        Thread serverThread = new Thread(() -> {
            try {
                log.info("Server listener thread starting...");
                serverListener.startServer();
                log.info("Server listener thread finished.");
            } catch (Throwable t) {
                log.error("Unhandled error in server listener thread", t);
            }
        });
        serverThread.setDaemon(true); // Esto permite que la JVM se cierre aunque este hilo esté corriendo
        try {
            // Esperar brevemente a que el peer server haga su trabajo inicial antes de arrancar el servidor de clientes
            try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            serverThread.start();
            log.debug("Server listener thread started (daemon={}).", serverThread.isDaemon());
        } catch (Exception e) {
            log.error("Failed to start server listener thread", e);
        }

        // 3.2. Thread para intentar conectar a peers conocidos (si hay componentes para conexiones salientes)
        Thread peerConnectorThread = new Thread(() -> {
            try {
                log.debug("Peer connector thread will sleep briefly before attempting connections.");
                Thread.sleep(2000); // Dar tiempo al servidor P2P y a la inicialización de peers para completar
                log.info("Peer connector thread attempting to connect to all known peers...");
                peerConnectionManager.connectToAllKnownPeers();
                log.info("Peer connector thread finished connecting to peers.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Peer connector thread was interrupted", e);
            } catch (Throwable t) {
                log.error("Unhandled error in peer connector thread", t);
            }
        });
        peerConnectorThread.setDaemon(true);
        try {
            peerConnectorThread.start();
            log.debug("Peer connector thread started (daemon={}).", peerConnectorThread.isDaemon());
        } catch (Exception e) {
            log.error("Failed to start peer connector thread", e);
        }

        // 4. Lanzar la interfaz gráfica de administrador en el hilo de eventos de Swing.
        log.info("Scheduling ServerMainWindow display on Swing EDT.");
        SwingUtilities.invokeLater(() -> {
            try {
                ServerMainWindow mainWindow = new ServerMainWindow(viewController);
                mainWindow.display();
                log.info("Server main window displayed.");
            } catch (Throwable t) {
                log.error("Failed to create or display ServerMainWindow on EDT", t);
            }
        });

        log.info("ServerLauncher main method completed.");
    }
}