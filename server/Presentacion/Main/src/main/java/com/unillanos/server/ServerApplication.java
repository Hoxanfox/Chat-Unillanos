package com.unillanos.server;

import com.unillanos.server.netty.server.NettyServer;
import com.unillanos.server.gui.MainWindow;
import com.unillanos.server.gui.SharedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Clase principal de la aplicación Chat-Unillanos Server.
 * Inicializa Spring Boot y arranca el servidor Netty.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.unillanos.server")
public class ServerApplication implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);
    
    private final NettyServer nettyServer;
    private final ApplicationContext applicationContext;

    public ServerApplication(NettyServer nettyServer, ApplicationContext applicationContext) {
        this.nettyServer = nettyServer;
        this.applicationContext = applicationContext;
    }

    public static void main(String[] args) {
        logger.info("=================================================");
        logger.info("   Iniciando Chat-Unillanos Server");
        logger.info("=================================================");
        
        SpringApplication.run(ServerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Establecer el contexto de Spring ANTES de lanzar JavaFX
        SharedContext.set(applicationContext);
        
        logger.info("Inicializando servidor Netty...");
        
        // Iniciar servidor Netty en un hilo virtual (Java 21 - Project Loom)
        Thread nettyThread = Thread.ofVirtual().name("Netty-Server-Thread").start(() -> {
            try {
                nettyServer.start();
            } catch (InterruptedException e) {
                logger.error("Servidor Netty interrumpido", e);
                Thread.currentThread().interrupt();
            }
        });

        // Registrar shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Apagando servidor...");
            nettyServer.stop();
        }));
        
        logger.info("=================================================");
        logger.info("   Servidor Chat-Unillanos iniciado correctamente");
        logger.info("   Usando hilos virtuales de Java 21");
        logger.info("=================================================");

        // Lanzar GUI JavaFX en el hilo principal (bloquea hasta que se cierre la ventana)
        javafx.application.Application.launch(MainWindow.class, args);
    }
}

