package com.arquitectura.logicaPeers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.domain.Peer;
import com.arquitectura.domain.enums.EstadoPeer;
import com.arquitectura.logicaPeers.config.P2PConfig;
import com.arquitectura.persistence.repository.PeerRepository;
import com.arquitectura.utils.network.NetworkUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de gestión de peers P2P.
 * Maneja la lógica de negocio para peers, heartbeats y retransmisión.
 */
@Service
public class PeerServiceImpl implements IPeerService {

    private final PeerRepository peerRepository;
    private final NetworkUtils networkUtils;
    private final P2PConfig p2pConfig;
    
    // Cache del peer actual
    private Peer peerActual;

    @Autowired
    public PeerServiceImpl(PeerRepository peerRepository, NetworkUtils networkUtils, P2PConfig p2pConfig) {
        this.peerRepository = peerRepository;
        this.networkUtils = networkUtils;
        this.p2pConfig = p2pConfig;
        
        // Inicializar al construir
        System.out.println("✓ [PeerService] Servicio de peers inicializado");
        
        // Validar configuración
        if (!p2pConfig.isValid()) {
            System.err.println("✗ [PeerService] Configuración P2P inválida");
        }
        
        // Mostrar configuración
        p2pConfig.printConfig();
    }

    // ==================== GESTIÓN DE PEERS ====================

    @Override
    @Transactional
    public PeerResponseDto agregarPeer(String ip, int puerto) throws Exception {
        return agregarPeer(ip, puerto, null);
    }

    @Override
    @Transactional
    public PeerResponseDto agregarPeer(String ip, int puerto, String nombreServidor) throws Exception {
        System.out.println("→ [PeerService] Agregando peer: " + ip + ":" + puerto);
        
        // Validar parámetros
        if (ip == null || ip.trim().isEmpty()) {
            throw new IllegalArgumentException("La IP del peer es requerida");
        }
        if (puerto <= 0 || puerto > 65535) {
            throw new IllegalArgumentException("Puerto inválido: " + puerto);
        }
        
        // Verificar si el peer ya existe
        Optional<Peer> peerExistente = peerRepository.findByIpAndPuerto(ip, puerto);
        
        if (peerExistente.isPresent()) {
            Peer peer = peerExistente.get();
            System.out.println("✓ [PeerService] Peer ya existe, actualizando: " + peer.getPeerId());
            
            // Actualizar información si es necesario
            if (nombreServidor != null && !nombreServidor.trim().isEmpty()) {
                peer.setNombreServidor(nombreServidor);
            }
            peer.marcarComoOnline();
            
            Peer peerActualizado = peerRepository.save(peer);
            return mapearAPeerResponseDto(peerActualizado);
        }
        
        // Crear nuevo peer
        Peer nuevoPeer = new Peer(ip, puerto, nombreServidor);
        nuevoPeer.marcarComoOnline();
        
        Peer peerGuardado = peerRepository.save(nuevoPeer);
        System.out.println("✓ [PeerService] Peer agregado exitosamente: " + peerGuardado.getPeerId());
        
        return mapearAPeerResponseDto(peerGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeerResponseDto> listarPeersDisponibles() {
        System.out.println("→ [PeerService] Listando todos los peers");
        List<Peer> peers = peerRepository.findAll();
        System.out.println("✓ [PeerService] Se encontraron " + peers.size() + " peers");
        
        return peers.stream()
                .map(this::mapearAPeerResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeerResponseDto> listarPeersActivos() {
        System.out.println("→ [PeerService] Listando peers activos");
        List<Peer> peersActivos = peerRepository.findByConectado(EstadoPeer.ONLINE);
        System.out.println("✓ [PeerService] Se encontraron " + peersActivos.size() + " peers activos");
        
        return peersActivos.stream()
                .map(this::mapearAPeerResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PeerResponseDto obtenerPeer(UUID peerId) throws Exception {
        System.out.println("→ [PeerService] Obteniendo peer: " + peerId);
        
        Peer peer = peerRepository.findById(peerId)
                .orElseThrow(() -> new Exception("Peer no encontrado: " + peerId));
        
        return mapearAPeerResponseDto(peer);
    }

    @Override
    @Transactional
    public void actualizarEstadoPeer(UUID peerId, String estado) throws Exception {
        System.out.println("→ [PeerService] Actualizando estado del peer " + peerId + " a " + estado);
        
        Peer peer = peerRepository.findById(peerId)
                .orElseThrow(() -> new Exception("Peer no encontrado: " + peerId));
        
        EstadoPeer nuevoEstado = EstadoPeer.valueOf(estado.toUpperCase());
        peer.setConectado(nuevoEstado);
        
        peerRepository.save(peer);
        System.out.println("✓ [PeerService] Estado actualizado");
    }

    @Override
    @Transactional
    public void eliminarPeer(UUID peerId) throws Exception {
        System.out.println("→ [PeerService] Eliminando peer: " + peerId);
        
        if (!peerRepository.existsById(peerId)) {
            throw new Exception("Peer no encontrado: " + peerId);
        }
        
        peerRepository.deleteById(peerId);
        System.out.println("✓ [PeerService] Peer eliminado");
    }

    // ==================== HEARTBEAT ====================

    @Override
    @Transactional
    public void reportarLatido(UUID peerId) throws Exception {
        System.out.println("→ [PeerService] Reportando latido del peer: " + peerId);
        
        Peer peer = peerRepository.findById(peerId)
                .orElseThrow(() -> new Exception("Peer no encontrado: " + peerId));
        
        peer.actualizarLatido();
        peerRepository.save(peer);
        
        System.out.println("✓ [PeerService] Latido reportado");
    }

    @Override
    @Transactional
    public void reportarLatido(UUID peerId, String ip, int puerto) throws Exception {
        System.out.println("→ [PeerService] Reportando latido con info: " + ip + ":" + puerto);
        
        Optional<Peer> peerOpt = peerRepository.findById(peerId);
        
        if (peerOpt.isPresent()) {
            // Peer existe, actualizar latido
            Peer peer = peerOpt.get();
            peer.actualizarLatido();
            peerRepository.save(peer);
            System.out.println("✓ [PeerService] Latido actualizado para peer existente");
        } else {
            // Peer no existe, crearlo
            System.out.println("→ [PeerService] Peer no existe, creando nuevo");
            Peer nuevoPeer = new Peer(ip, puerto);
            nuevoPeer.setPeerId(peerId);
            nuevoPeer.marcarComoOnline();
            peerRepository.save(nuevoPeer);
            System.out.println("✓ [PeerService] Nuevo peer creado y latido registrado");
        }
    }

    @Override
    @Transactional
    public int verificarPeersInactivos() {
        System.out.println("→ [PeerService] Verificando peers inactivos");
        
        long timeoutSegundos = p2pConfig.getHeartbeatTimeout() / 1000;
        LocalDateTime limiteTimeout = LocalDateTime.now().minusSeconds(timeoutSegundos);
        
        List<Peer> peersInactivos = peerRepository.findPeersInactivos(limiteTimeout);
        
        System.out.println("→ [PeerService] Se encontraron " + peersInactivos.size() + " peers inactivos");
        
        for (Peer peer : peersInactivos) {
            peer.marcarComoOffline();
            peerRepository.save(peer);
            System.out.println("  ✗ Peer marcado como OFFLINE: " + peer.getIp() + ":" + peer.getPuerto());
        }
        
        System.out.println("✓ [PeerService] Verificación completada");
        return peersInactivos.size();
    }

    @Override
    public long obtenerIntervaloHeartbeat() {
        return p2pConfig.getHeartbeatInterval();
    }

    // ==================== RETRANSMISIÓN ====================

    @Override
    public DTOResponse retransmitirPeticion(UUID peerDestinoId, DTORequest peticionOriginal) throws Exception {
        System.out.println("→ [PeerService] Retransmitiendo petición al peer: " + peerDestinoId);
        
        // Obtener información del peer destino
        Peer peerDestino = peerRepository.findById(peerDestinoId)
                .orElseThrow(() -> new Exception("Peer destino no encontrado: " + peerDestinoId));
        
        if (!peerDestino.estaActivo()) {
            throw new Exception("El peer destino no está activo: " + peerDestinoId);
        }
        
        // TODO: Implementar cliente HTTP/TCP para enviar la petición al peer
        // Por ahora, retornamos una respuesta de error indicando que no está implementado
        System.out.println("⚠ [PeerService] Retransmisión no implementada aún");
        
        DTOResponse response = new DTOResponse(
            peticionOriginal.getAction(),
            "error",
            "Retransmisión P2P no implementada aún",
            null
        );
        
        return response;
    }

    // ==================== PEER ACTUAL ====================

    @Override
    public Peer obtenerPeerActual() {
        if (peerActual == null) {
            inicializarPeerActual();
        }
        return peerActual;
    }

    @Override
    public UUID obtenerPeerActualId() {
        Peer peer = obtenerPeerActual();
        return peer != null ? peer.getPeerId() : null;
    }

    /**
     * Inicializa o recupera el peer que representa este servidor.
     */
    private void inicializarPeerActual() {
        try {
            String ipServidor = networkUtils.getServerIPAddress();
            int puerto = p2pConfig.getPuerto();
            String nombreServidor = p2pConfig.getNombreServidor();
            
            if (nombreServidor == null || nombreServidor.trim().isEmpty()) {
                nombreServidor = "Servidor Local";
            }
            
            // Buscar si ya existe un peer para este servidor
            Optional<Peer> peerOpt = peerRepository.findByIpAndPuerto(ipServidor, puerto);
            
            if (peerOpt.isPresent()) {
                peerActual = peerOpt.get();
                peerActual.setNombreServidor(nombreServidor);
                peerActual.marcarComoOnline();
                peerRepository.save(peerActual);
                System.out.println("✓ [PeerService] Peer actual recuperado: " + peerActual.getPeerId());
            } else {
                // Crear nuevo peer para este servidor
                peerActual = new Peer(ipServidor, puerto, nombreServidor);
                peerActual.marcarComoOnline();
                peerActual = peerRepository.save(peerActual);
                System.out.println("✓ [PeerService] Peer actual creado: " + peerActual.getPeerId());
            }
        } catch (Exception e) {
            System.err.println("✗ [PeerService] Error al inicializar peer actual: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== ESTADÍSTICAS ====================

    @Override
    @Transactional(readOnly = true)
    public long contarTotalPeers() {
        return peerRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarPeersActivos() {
        return peerRepository.contarPeersActivos();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarPeersInactivos() {
        long total = contarTotalPeers();
        long activos = contarPeersActivos();
        return total - activos;
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Mapea una entidad Peer a un DTO de respuesta.
     */
    private PeerResponseDto mapearAPeerResponseDto(Peer peer) {
        return new PeerResponseDto(
            peer.getPeerId(),
            peer.getIp(),
            peer.getPuerto(),
            peer.getConectado().toString(),
            peer.getUltimoLatido(),
            peer.getNombreServidor()
        );
    }
}
