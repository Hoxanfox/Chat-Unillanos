package com.arquitectura.logicaPeers.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para P2PConfig.
 */
class P2PConfigTest {

    private P2PConfig config;

    @BeforeEach
    void setUp() {
        config = new P2PConfig();
    }

    @Test
    void testConfiguracionPorDefecto() {
        // Arrange - usar valores por defecto
        ReflectionTestUtils.setField(config, "enabled", true);
        ReflectionTestUtils.setField(config, "puerto", 22100);
        ReflectionTestUtils.setField(config, "heartbeatInterval", 30000L);
        ReflectionTestUtils.setField(config, "heartbeatTimeout", 90000L);
        ReflectionTestUtils.setField(config, "clientTimeout", 10000);
        
        // Assert
        assertTrue(config.isEnabled());
        assertEquals(22100, config.getPuerto());
        assertEquals(30000L, config.getHeartbeatInterval());
        assertEquals(90000L, config.getHeartbeatTimeout());
        assertEquals(10000, config.getClientTimeout());
    }

    @Test
    void testValidacion_ConfiguracionValida() {
        // Arrange
        ReflectionTestUtils.setField(config, "enabled", true);
        ReflectionTestUtils.setField(config, "puerto", 22100);
        ReflectionTestUtils.setField(config, "heartbeatInterval", 30000L);
        ReflectionTestUtils.setField(config, "heartbeatTimeout", 90000L);
        ReflectionTestUtils.setField(config, "clientTimeout", 10000);
        
        // Act
        boolean resultado = config.isValid();
        
        // Assert
        assertTrue(resultado);
    }

    @Test
    void testValidacion_PuertoInvalido() {
        // Arrange
        ReflectionTestUtils.setField(config, "enabled", true);
        ReflectionTestUtils.setField(config, "puerto", -1);
        ReflectionTestUtils.setField(config, "heartbeatInterval", 30000L);
        ReflectionTestUtils.setField(config, "heartbeatTimeout", 90000L);
        ReflectionTestUtils.setField(config, "clientTimeout", 10000);
        
        // Act
        boolean resultado = config.isValid();
        
        // Assert
        assertFalse(resultado);
    }

    @Test
    void testValidacion_PuertoFueraDeRango() {
        // Arrange
        ReflectionTestUtils.setField(config, "enabled", true);
        ReflectionTestUtils.setField(config, "puerto", 70000);
        ReflectionTestUtils.setField(config, "heartbeatInterval", 30000L);
        ReflectionTestUtils.setField(config, "heartbeatTimeout", 90000L);
        ReflectionTestUtils.setField(config, "clientTimeout", 10000);
        
        // Act
        boolean resultado = config.isValid();
        
        // Assert
        assertFalse(resultado);
    }

    @Test
    void testValidacion_IntervaloHeartbeatInvalido() {
        // Arrange
        ReflectionTestUtils.setField(config, "enabled", true);
        ReflectionTestUtils.setField(config, "puerto", 22100);
        ReflectionTestUtils.setField(config, "heartbeatInterval", -1000L);
        ReflectionTestUtils.setField(config, "heartbeatTimeout", 90000L);
        ReflectionTestUtils.setField(config, "clientTimeout", 10000);
        
        // Act
        boolean resultado = config.isValid();
        
        // Assert
        assertFalse(resultado);
    }

    @Test
    void testValidacion_TimeoutMenorQueIntervalo() {
        // Arrange
        ReflectionTestUtils.setField(config, "enabled", true);
        ReflectionTestUtils.setField(config, "puerto", 22100);
        ReflectionTestUtils.setField(config, "heartbeatInterval", 90000L);
        ReflectionTestUtils.setField(config, "heartbeatTimeout", 30000L);
        ReflectionTestUtils.setField(config, "clientTimeout", 10000);
        
        // Act
        boolean resultado = config.isValid();
        
        // Assert
        assertFalse(resultado);
    }

    @Test
    void testValidacion_ClientTimeoutInvalido() {
        // Arrange
        ReflectionTestUtils.setField(config, "enabled", true);
        ReflectionTestUtils.setField(config, "puerto", 22100);
        ReflectionTestUtils.setField(config, "heartbeatInterval", 30000L);
        ReflectionTestUtils.setField(config, "heartbeatTimeout", 90000L);
        ReflectionTestUtils.setField(config, "clientTimeout", -1);
        
        // Act
        boolean resultado = config.isValid();
        
        // Assert
        assertFalse(resultado);
    }

    @Test
    void testValidacion_P2PDeshabilitado() {
        // Arrange
        ReflectionTestUtils.setField(config, "enabled", false);
        ReflectionTestUtils.setField(config, "puerto", -1); // Puerto inválido
        
        // Act
        boolean resultado = config.isValid();
        
        // Assert
        assertTrue(resultado); // Si está deshabilitado, no valida
    }

    @Test
    void testGetters() {
        // Arrange
        ReflectionTestUtils.setField(config, "enabled", true);
        ReflectionTestUtils.setField(config, "puerto", 22100);
        ReflectionTestUtils.setField(config, "nombreServidor", "Test-Server");
        ReflectionTestUtils.setField(config, "ip", "192.168.1.10");
        ReflectionTestUtils.setField(config, "heartbeatInterval", 30000L);
        ReflectionTestUtils.setField(config, "heartbeatTimeout", 90000L);
        ReflectionTestUtils.setField(config, "heartbeatEnabled", true);
        ReflectionTestUtils.setField(config, "discoveryEnabled", true);
        ReflectionTestUtils.setField(config, "discoveryInterval", 300000L);
        ReflectionTestUtils.setField(config, "peersBootstrap", "192.168.1.11:22100");
        ReflectionTestUtils.setField(config, "clientTimeout", 10000);
        ReflectionTestUtils.setField(config, "clientPoolThreads", 10);
        ReflectionTestUtils.setField(config, "clientRetryAttempts", 3);
        ReflectionTestUtils.setField(config, "clientRetryDelay", 1000L);
        
        // Assert
        assertTrue(config.isEnabled());
        assertEquals(22100, config.getPuerto());
        assertEquals("Test-Server", config.getNombreServidor());
        assertEquals("192.168.1.10", config.getIp());
        assertEquals(30000L, config.getHeartbeatInterval());
        assertEquals(90000L, config.getHeartbeatTimeout());
        assertTrue(config.isHeartbeatEnabled());
        assertTrue(config.isDiscoveryEnabled());
        assertEquals(300000L, config.getDiscoveryInterval());
        assertEquals("192.168.1.11:22100", config.getPeersBootstrap());
        assertEquals(10000, config.getClientTimeout());
        assertEquals(10, config.getClientPoolThreads());
        assertEquals(3, config.getClientRetryAttempts());
        assertEquals(1000L, config.getClientRetryDelay());
    }

    @Test
    void testToString() {
        // Arrange
        ReflectionTestUtils.setField(config, "enabled", true);
        ReflectionTestUtils.setField(config, "puerto", 22100);
        ReflectionTestUtils.setField(config, "nombreServidor", "Test-Server");
        ReflectionTestUtils.setField(config, "heartbeatInterval", 30000L);
        ReflectionTestUtils.setField(config, "heartbeatTimeout", 90000L);
        ReflectionTestUtils.setField(config, "clientTimeout", 10000);
        
        // Act
        String resultado = config.toString();
        
        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.contains("enabled=true"));
        assertTrue(resultado.contains("puerto=22100"));
        assertTrue(resultado.contains("nombreServidor='Test-Server'"));
    }
}
