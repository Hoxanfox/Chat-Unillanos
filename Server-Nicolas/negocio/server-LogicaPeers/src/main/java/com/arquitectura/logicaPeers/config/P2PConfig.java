package com.arquitectura.logicaPeers.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Clase de configuración para propiedades P2P.
 * Lee las propiedades del archivo application.properties y las hace disponibles
 * para los servicios P2P.
 */
@Configuration
@PropertySource("file:./config/p2p.properties")
public class P2PConfig {
    
    // ==================== CONFIGURACIÓN GENERAL P2P ====================
    
    @Value("${p2p.enabled:true}")
    private boolean enabled;
    
    @Value("${p2p.puerto:22100}")
    private int puerto;
    
    @Value("${p2p.nombre.servidor:Servidor-P2P}")
    private String nombreServidor;
    
    @Value("${p2p.ip:}")
    private String ip;
    
    // ==================== CONFIGURACIÓN DE HEARTBEAT ====================
    
    @Value("${p2p.heartbeat.interval:30000}")
    private long heartbeatInterval;
    
    @Value("${p2p.heartbeat.timeout:90000}")
    private long heartbeatTimeout;
    
    @Value("${p2p.heartbeat.enabled:true}")
    private boolean heartbeatEnabled;
    
    // ==================== CONFIGURACIÓN DE DESCUBRIMIENTO ====================
    
    @Value("${p2p.discovery.enabled:true}")
    private boolean discoveryEnabled;
    
    @Value("${p2p.discovery.interval:300000}")
    private long discoveryInterval;
    
    @Value("${p2p.peers.bootstrap:}")
    private String peersBootstrap;
    
    // ==================== CONFIGURACIÓN DE CLIENTE P2P ====================
    
    @Value("${p2p.client.timeout:10000}")
    private int clientTimeout;
    
    @Value("${p2p.client.pool.threads:10}")
    private int clientPoolThreads;
    
    @Value("${p2p.client.retry.attempts:3}")
    private int clientRetryAttempts;
    
    @Value("${p2p.client.retry.delay:1000}")
    private long clientRetryDelay;
    
    // ==================== GETTERS ====================
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getPuerto() {
        return puerto;
    }
    
    public String getNombreServidor() {
        return nombreServidor;
    }
    
    public String getIp() {
        return ip;
    }
    
    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    public long getHeartbeatTimeout() {
        return heartbeatTimeout;
    }
    
    public boolean isHeartbeatEnabled() {
        return heartbeatEnabled;
    }
    
    public boolean isDiscoveryEnabled() {
        return discoveryEnabled;
    }
    
    public long getDiscoveryInterval() {
        return discoveryInterval;
    }
    
    public String getPeersBootstrap() {
        return peersBootstrap;
    }
    
    public int getClientTimeout() {
        return clientTimeout;
    }
    
    public int getClientPoolThreads() {
        return clientPoolThreads;
    }
    
    public int getClientRetryAttempts() {
        return clientRetryAttempts;
    }
    
    public long getClientRetryDelay() {
        return clientRetryDelay;
    }
    
    // ==================== MÉTODOS DE UTILIDAD ====================
    
    /**
     * Verifica si la configuración P2P es válida.
     * 
     * @return true si la configuración es válida
     */
    public boolean isValid() {
        if (!enabled) {
            return true; // Si está deshabilitado, no necesita validación
        }
        
        // Validar puerto
        if (puerto <= 0 || puerto > 65535) {
            System.err.println("✗ [P2PConfig] Puerto inválido: " + puerto);
            return false;
        }
        
        // Validar intervalos de heartbeat
        if (heartbeatInterval <= 0) {
            System.err.println("✗ [P2PConfig] Intervalo de heartbeat inválido: " + heartbeatInterval);
            return false;
        }
        
        if (heartbeatTimeout <= heartbeatInterval) {
            System.err.println("✗ [P2PConfig] Timeout de heartbeat debe ser mayor que el intervalo");
            return false;
        }
        
        // Validar timeout de cliente
        if (clientTimeout <= 0) {
            System.err.println("✗ [P2PConfig] Timeout de cliente inválido: " + clientTimeout);
            return false;
        }
        
        return true;
    }
    
    /**
     * Muestra la configuración P2P actual en la consola.
     */
    public void printConfig() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           CONFIGURACIÓN P2P DEL SERVIDOR                   ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║ P2P Habilitado:        " + formatValue(enabled));
        System.out.println("║ Puerto:                " + formatValue(puerto));
        System.out.println("║ Nombre Servidor:       " + formatValue(nombreServidor));
        System.out.println("║ IP:                    " + formatValue(ip.isEmpty() ? "Auto-detectar" : ip));
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║ HEARTBEAT");
        System.out.println("║ - Habilitado:          " + formatValue(heartbeatEnabled));
        System.out.println("║ - Intervalo:           " + formatValue(heartbeatInterval + " ms"));
        System.out.println("║ - Timeout:             " + formatValue(heartbeatTimeout + " ms"));
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║ DESCUBRIMIENTO");
        System.out.println("║ - Habilitado:          " + formatValue(discoveryEnabled));
        System.out.println("║ - Intervalo:           " + formatValue(discoveryInterval + " ms"));
        System.out.println("║ - Peers Bootstrap:     " + formatValue(peersBootstrap.isEmpty() ? "Ninguno" : peersBootstrap));
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║ CLIENTE P2P");
        System.out.println("║ - Timeout:             " + formatValue(clientTimeout + " ms"));
        System.out.println("║ - Pool Threads:        " + formatValue(clientPoolThreads));
        System.out.println("║ - Reintentos:          " + formatValue(clientRetryAttempts));
        System.out.println("║ - Delay Reintentos:    " + formatValue(clientRetryDelay + " ms"));
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }
    
    /**
     * Formatea un valor para la impresión en consola.
     */
    private String formatValue(Object value) {
        String str = String.valueOf(value);
        int padding = 40 - str.length();
        return str + " ".repeat(Math.max(0, padding)) + "║";
    }
    
    /**
     * Obtiene un resumen de la configuración como String.
     * 
     * @return String con el resumen de configuración
     */
    @Override
    public String toString() {
        return "P2PConfig{" +
                "enabled=" + enabled +
                ", puerto=" + puerto +
                ", nombreServidor='" + nombreServidor + '\'' +
                ", heartbeatInterval=" + heartbeatInterval +
                ", heartbeatTimeout=" + heartbeatTimeout +
                ", clientTimeout=" + clientTimeout +
                '}';
    }

    // ==================== BEANS ====================

    /**
     * Crea y configura el bean de PeerConnectionPool.
     *
     * @return Instancia configurada de PeerConnectionPool
     */
    @Bean
    public com.arquitectura.utils.p2p.PeerConnectionPool peerConnectionPool() {
        System.out.println("→ [P2PConfig] Creando PeerConnectionPool con " + clientPoolThreads + " threads");
        return new com.arquitectura.utils.p2p.PeerConnectionPool(clientPoolThreads);
    }
}
