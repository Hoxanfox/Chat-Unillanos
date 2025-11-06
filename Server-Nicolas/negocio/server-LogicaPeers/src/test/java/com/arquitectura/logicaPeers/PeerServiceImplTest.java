package com.arquitectura.logicaPeers;

import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.domain.Peer;
import com.arquitectura.logicaPeers.config.P2PConfig;
import com.arquitectura.persistence.repository.PeerRepository;
import com.arquitectura.utils.network.NetworkUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para PeerServiceImpl.
 */
class PeerServiceImplTest {

    @Mock
    private PeerRepository peerRepository;

    @Mock
    private NetworkUtils networkUtils;

    @Mock
    private P2PConfig p2pConfig;

    private PeerServiceImpl peerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Configurar mocks por defecto
        when(p2pConfig.isValid()).thenReturn(true);
        when(p2pConfig.getPuerto()).thenReturn(22100);
        when(p2pConfig.getNombreServidor()).thenReturn("Test-Server");
        when(p2pConfig.getHeartbeatInterval()).thenReturn(30000L);
        when(p2pConfig.getHeartbeatTimeout()).thenReturn(90000L);
        
        peerService = new PeerServiceImpl(peerRepository, networkUtils, p2pConfig);
    }

    @Test
    void testAgregarPeer_NuevoPeer() throws Exception {
        // Arrange
        String ip = "192.168.1.10";
        int puerto = 22100;
        String nombreServidor = "Servidor-Test";
        
        when(peerRepository.findByIpAndPuerto(ip, puerto)).thenReturn(Optional.empty());
        
        Peer peerGuardado = new Peer(ip, puerto, nombreServidor);
        peerGuardado.setPeerId(UUID.randomUUID());
        when(peerRepository.save(any(Peer.class))).thenReturn(peerGuardado);
        
        // Act
        PeerResponseDto resultado = peerService.agregarPeer(ip, puerto, nombreServidor);
        
        // Assert
        assertNotNull(resultado);
        assertEquals(ip, resultado.getIp());
        assertEquals(puerto, resultado.getPuerto());
        assertEquals(nombreServidor, resultado.getNombreServidor());
        verify(peerRepository, times(1)).save(any(Peer.class));
    }

    @Test
    void testAgregarPeer_PeerExistente() throws Exception {
        // Arrange
        String ip = "192.168.1.10";
        int puerto = 22100;
        
        Peer peerExistente = new Peer(ip, puerto, "Servidor-Viejo");
        peerExistente.setPeerId(UUID.randomUUID());
        when(peerRepository.findByIpAndPuerto(ip, puerto)).thenReturn(Optional.of(peerExistente));
        when(peerRepository.save(any(Peer.class))).thenReturn(peerExistente);
        
        // Act
        PeerResponseDto resultado = peerService.agregarPeer(ip, puerto, "Servidor-Nuevo");
        
        // Assert
        assertNotNull(resultado);
        assertEquals(ip, resultado.getIp());
        assertEquals(puerto, resultado.getPuerto());
        verify(peerRepository, times(1)).save(any(Peer.class));
    }

    @Test
    void testAgregarPeer_IpInvalida() {
        // Arrange
        String ip = "";
        int puerto = 22100;
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            peerService.agregarPeer(ip, puerto);
        });
    }

    @Test
    void testAgregarPeer_PuertoInvalido() {
        // Arrange
        String ip = "192.168.1.10";
        int puerto = -1;
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            peerService.agregarPeer(ip, puerto);
        });
    }

    @Test
    void testListarPeersDisponibles() {
        // Arrange
        Peer peer1 = new Peer("192.168.1.10", 22100, "Servidor-1");
        peer1.setPeerId(UUID.randomUUID());
        peer1.marcarComoOnline();
        
        Peer peer2 = new Peer("192.168.1.11", 22100, "Servidor-2");
        peer2.setPeerId(UUID.randomUUID());
        peer2.marcarComoOffline();
        
        when(peerRepository.findAll()).thenReturn(Arrays.asList(peer1, peer2));
        
        // Act
        List<PeerResponseDto> resultado = peerService.listarPeersDisponibles();
        
        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(peerRepository, times(1)).findAll();
    }

    @Test
    void testListarPeersActivos() {
        // Arrange
        Peer peer1 = new Peer("192.168.1.10", 22100, "Servidor-1");
        peer1.setPeerId(UUID.randomUUID());
        peer1.marcarComoOnline();
        
        when(peerRepository.findByConectado("ONLINE")).thenReturn(Arrays.asList(peer1));
        
        // Act
        List<PeerResponseDto> resultado = peerService.listarPeersActivos();
        
        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("ONLINE", resultado.get(0).getConectado());
        verify(peerRepository, times(1)).findByConectado("ONLINE");
    }

    @Test
    void testReportarLatido() throws Exception {
        // Arrange
        UUID peerId = UUID.randomUUID();
        Peer peer = new Peer("192.168.1.10", 22100, "Servidor-Test");
        peer.setPeerId(peerId);
        
        when(peerRepository.findById(peerId)).thenReturn(Optional.of(peer));
        when(peerRepository.save(any(Peer.class))).thenReturn(peer);
        
        // Act
        peerService.reportarLatido(peerId);
        
        // Assert
        verify(peerRepository, times(1)).findById(peerId);
        verify(peerRepository, times(1)).save(any(Peer.class));
    }

    @Test
    void testReportarLatido_PeerNoExiste() {
        // Arrange
        UUID peerId = UUID.randomUUID();
        when(peerRepository.findById(peerId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(Exception.class, () -> {
            peerService.reportarLatido(peerId);
        });
    }

    @Test
    void testVerificarPeersInactivos() {
        // Arrange
        Peer peerInactivo = new Peer("192.168.1.10", 22100, "Servidor-Inactivo");
        peerInactivo.setPeerId(UUID.randomUUID());
        peerInactivo.setUltimoLatido(LocalDateTime.now().minusMinutes(5));
        
        when(peerRepository.findPeersInactivos(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(peerInactivo));
        when(peerRepository.save(any(Peer.class))).thenReturn(peerInactivo);
        
        // Act
        int resultado = peerService.verificarPeersInactivos();
        
        // Assert
        assertEquals(1, resultado);
        verify(peerRepository, times(1)).findPeersInactivos(any(LocalDateTime.class));
        verify(peerRepository, times(1)).save(any(Peer.class));
    }

    @Test
    void testObtenerIntervaloHeartbeat() {
        // Act
        long resultado = peerService.obtenerIntervaloHeartbeat();
        
        // Assert
        assertEquals(30000L, resultado);
        verify(p2pConfig, times(1)).getHeartbeatInterval();
    }

    @Test
    void testContarTotalPeers() {
        // Arrange
        when(peerRepository.count()).thenReturn(5L);
        
        // Act
        long resultado = peerService.contarTotalPeers();
        
        // Assert
        assertEquals(5L, resultado);
        verify(peerRepository, times(1)).count();
    }

    @Test
    void testContarPeersActivos() {
        // Arrange
        when(peerRepository.countByConectado("ONLINE")).thenReturn(3L);
        
        // Act
        long resultado = peerService.contarPeersActivos();
        
        // Assert
        assertEquals(3L, resultado);
        verify(peerRepository, times(1)).countByConectado("ONLINE");
    }

    @Test
    void testContarPeersInactivos() {
        // Arrange
        when(peerRepository.countByConectado("OFFLINE")).thenReturn(2L);
        
        // Act
        long resultado = peerService.contarPeersInactivos();
        
        // Assert
        assertEquals(2L, resultado);
        verify(peerRepository, times(1)).countByConectado("OFFLINE");
    }
}
