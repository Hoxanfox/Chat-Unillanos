package com.arquitectura.transporte.config;

import com.arquitectura.controlador.RequestDispatcher;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Configuration
public class PeerTransportConfig {

    @Value("${peer.server.port:22100}")
    private int peerPort;

    @Value("${peer.max.connections:10}")
    private int peerMaxConnections;

    @Bean
    public com.arquitectura.transporte.peer.server.PeerServer peerServer(RequestDispatcher requestDispatcher, com.arquitectura.transporte.peer.PeerConnectionManager manager, Gson gson) {
        System.out.println("→ [PeerTransportConfig] Creando PeerServer en puerto " + peerPort + " con max connections " + peerMaxConnections);
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(2, peerMaxConnections));
        AtomicBoolean running = new AtomicBoolean(true);
        Supplier<Boolean> runningSupplier = running::get;
        return new com.arquitectura.transporte.peer.server.PeerServer(peerPort, peerMaxConnections, pool, gson, manager, requestDispatcher, runningSupplier);
    }
}
