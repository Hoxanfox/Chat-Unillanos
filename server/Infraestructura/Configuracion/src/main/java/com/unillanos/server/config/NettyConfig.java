package com.unillanos.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del servidor Netty.
 */
@Configuration
public class NettyConfig {

    @Value("${server.netty.port:8080}")
    private int port;

    @Value("${server.netty.boss-threads:1}")
    private int bossThreads;

    @Value("${server.netty.worker-threads:4}")
    private int workerThreads;

    public int getPort() {
        return port;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }
}

