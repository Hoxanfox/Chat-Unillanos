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
@Service("peerServiceP2P")
public class PeerServiceImpl implements IPeerService {

    private final PeerRepository peerRepository;
    private final NetworkUtils networkUtils;
    private final P2PConfig p2pConfig;
    private final com.arquitectura.utils.p2p.PeerConnectionPool peerConnectionPool;
    private final com.arquitectura.persistence.repository.UserRepository userRepository;

    // Cache del peer actual
    private Peer peerActual;

    @Autowired
    public PeerServiceImpl(PeerRepository peerRepository, NetworkUtils networkUtils, P2PConfig p2pConfig, 
                          com.arquitectura.utils.p2p.PeerConnectionPool peerConnectionPool,
                          com.arquitectura.persistence.repository.UserRepository userRepository) {
        this.peerRepository = peerRepository;
        this.networkUtils = networkUtils;
        this.p2pConfig = p2pConfig;
        this.peerConnectionPool = peerConnectionPool;
        this.userRepository = userRepository;

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
        
        // Usar PeerConnectionPool para enviar la petición
        DTOResponse response = peerConnectionPool.enviarPeticion(
            peerDestino.getIp(),
            peerDestino.getPuerto(),
            peticionOriginal
        );
        
        return response;
    }

    @Override
    public byte[] descargarArchivoDesdePeer(UUID peerDestinoId, String fileId) throws Exception {
        System.out.println("→ [PeerService] Descargando archivo " + fileId + " desde peer: " + peerDestinoId);

        // Obtener información del peer destino
        Peer peerDestino = peerRepository.findById(peerDestinoId)
                .orElseThrow(() -> new Exception("Peer destino no encontrado: " + peerDestinoId));

        if (!peerDestino.estaActivo()) {
            throw new Exception("El peer destino no está activo: " + peerDestinoId);
        }

        // Paso 1: Iniciar la descarga para obtener información del archivo
        System.out.println("→ [PeerService] Iniciando descarga con startFileDownload");
        DTORequest startDownloadRequest = new DTORequest(
            "startFileDownload",
            java.util.Map.of("fileId", fileId)
        );

        DTOResponse startResponse = peerConnectionPool.enviarPeticion(
            peerDestino.getIp(),
            peerDestino.getPuerto(),
            startDownloadRequest
        );

        if (!"success".equals(startResponse.getStatus())) {
            throw new Exception("Error al iniciar descarga: " + startResponse.getMessage());
        }

        // Parsear la información de descarga
        com.google.gson.Gson gson = new com.google.gson.Gson();
        com.google.gson.JsonObject payload = gson.toJsonTree(startResponse.getData()).getAsJsonObject();
        String downloadId = payload.get("downloadId").getAsString();
        int totalChunks = payload.get("totalChunks").getAsInt();
        long fileSize = payload.get("fileSize").getAsLong();

        System.out.println("→ [PeerService] Archivo info: downloadId=" + downloadId + ", totalChunks=" + totalChunks + ", size=" + fileSize);

        // Paso 2: Descargar todos los chunks
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        for (int chunkNumber = 0; chunkNumber < totalChunks; chunkNumber++) {
            System.out.println("→ [PeerService] Solicitando chunk " + (chunkNumber + 1) + "/" + totalChunks);

            DTORequest chunkRequest = new DTORequest(
                "requestFileChunk",
                java.util.Map.of(
                    "downloadId", downloadId,
                    "chunkNumber", chunkNumber
                )
            );

            DTOResponse chunkResponse = peerConnectionPool.enviarPeticion(
                peerDestino.getIp(),
                peerDestino.getPuerto(),
                chunkRequest
            );

            if (!"success".equals(chunkResponse.getStatus())) {
                throw new Exception("Error al descargar chunk " + chunkNumber + ": " + chunkResponse.getMessage());
            }

            // Extraer y decodificar el chunk
            com.google.gson.JsonObject chunkData = gson.toJsonTree(chunkResponse.getData()).getAsJsonObject();
            String chunkBase64 = chunkData.get("chunkDataBase64").getAsString();
            byte[] chunkBytes = java.util.Base64.getDecoder().decode(chunkBase64);

            // Escribir los bytes del chunk al stream
            baos.write(chunkBytes);
        }

        System.out.println("✓ [PeerService] Archivo descargado exitosamente (" + baos.size() + " bytes)");
        return baos.toByteArray();
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

    // ==================== BÚSQUEDA DE USUARIOS ====================

    @Override
    @Transactional(readOnly = true)
    public com.arquitectura.DTO.p2p.UserLocationResponseDto buscarUsuario(UUID usuarioId) throws Exception {
        System.out.println("→ [PeerService] Buscando ubicación del usuario: " + usuarioId);
        
        // Obtener el usuario con su peer asociado
        com.arquitectura.domain.User usuario = userRepository.findByIdWithPeer(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado: " + usuarioId));
        
        // Obtener el peer asociado
        Peer peerAsociado = usuario.getPeerId();
        
        if (peerAsociado == null) {
            System.out.println("✗ [PeerService] Usuario no tiene peer asociado");
            // Usuario existe pero no está asociado a ningún peer
            return new com.arquitectura.DTO.p2p.UserLocationResponseDto(
                usuario.getUserId(),
                usuario.getUsername(),
                null,
                null,
                null,
                usuario.getConectado()
            );
        }
        
        System.out.println("✓ [PeerService] Usuario encontrado en peer: " + peerAsociado.getPeerId());
        
        return new com.arquitectura.DTO.p2p.UserLocationResponseDto(
            usuario.getUserId(),
            usuario.getUsername(),
            peerAsociado.getPeerId(),
            peerAsociado.getIp(),
            peerAsociado.getPuerto(),
            usuario.getConectado()
        );
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

    @Override
    @Transactional(readOnly = true)
    public Optional<PeerResponseDto> buscarPeerPorIpYPuerto(String ip, int puerto) {
        System.out.println("→ [PeerService] Buscando peer por IP:Puerto: " + ip + ":" + puerto);
        Optional<Peer> peerOpt = peerRepository.findByIpAndPuerto(ip, puerto);
        return peerOpt.map(this::mapearAPeerResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PeerResponseDto> buscarPeerPorId(UUID peerId) {
        System.out.println("→ [PeerService] Buscando peer por ID: " + peerId);
        Optional<Peer> peerOpt = peerRepository.findById(peerId);
        return peerOpt.map(this::mapearAPeerResponseDto);
    }

    @Override
    @Transactional
    public PeerResponseDto registrarPeerAutenticado(UUID peerId, String ip, Integer puerto) {
        System.out.println("→ [PeerService] Registrando peer autenticado: " + peerId + " (" + ip + ":" + puerto + ")");
        
        // Buscar primero por peerId
        Optional<Peer> peerOpt = peerRepository.findById(peerId);
        
        // Si no se encuentra por ID, buscar por IP y Puerto
        if (!peerOpt.isPresent() && ip != null && puerto != null) {
            peerOpt = peerRepository.findByIpAndPuerto(ip, puerto);
            if (peerOpt.isPresent()) {
                System.out.println("→ [PeerService] Peer encontrado por IP:Puerto pero con ID diferente. Actualizando ID.");
            }
        }
        
        Peer peer;
        
        if (peerOpt.isPresent()) {
            // El peer ya existe, actualizar
            peer = peerOpt.get();
            
            // Si el peerId cambió, actualizarlo
            if (!peer.getPeerId().equals(peerId)) {
                System.out.println("→ [PeerService] PeerId cambió de " + peer.getPeerId() + " a " + peerId);
                peer.setPeerId(peerId);
            }
            
            System.out.println("→ [PeerService] Actualizando peer existente");
        } else {
            // El peer NO existe, crearlo
            System.out.println("→ [PeerService] Creando nuevo peer");
            peer = new Peer();
            peer.setPeerId(peerId);
            peer.setIp(ip);
            peer.setPuerto(puerto != null ? puerto : 0);
        }
        
        // Actualizar estado y latido
        peer.setConectado(EstadoPeer.ONLINE);
        peer.actualizarLatido();
        Peer savedPeer = peerRepository.save(peer);
        
        System.out.println("✓ [PeerService] Peer registrado: " + savedPeer.getPeerId());
        
        return mapearAPeerResponseDto(savedPeer);
    }

    @Override
    @Transactional
    public void marcarPeerComoDesconectado(UUID peerId) {
        System.out.println("→ [PeerService] Marcando peer como desconectado: " + peerId);
        
        Optional<Peer> peerOpt = peerRepository.findById(peerId);
        if (peerOpt.isPresent()) {
            Peer peer = peerOpt.get();
            peer.setConectado(EstadoPeer.OFFLINE);
            peerRepository.save(peer);
            System.out.println("✓ [PeerService] Peer marcado como OFFLINE");
        } else {
            System.out.println("⚠ [PeerService] Peer no encontrado: " + peerId);
        }
    }

    @Override
    @Transactional
    public PeerResponseDto obtenerOCrearPeerLocal(String ip, int puerto) {
        System.out.println("→ [PeerService] Obteniendo o creando peer local: " + ip + ":" + puerto);
        
        Optional<Peer> peerOpt = peerRepository.findByIpAndPuerto(ip, puerto);
        
        Peer peer;
        if (peerOpt.isPresent()) {
            peer = peerOpt.get();
            System.out.println("✓ [PeerService] Peer local encontrado: " + peer.getPeerId());
        } else {
            peer = new Peer(ip, puerto, "ONLINE");
            peer.setUltimoLatido(LocalDateTime.now());
            peer = peerRepository.save(peer);
            System.out.println("✓ [PeerService] Nuevo peer local creado: " + peer.getPeerId());
        }
        
        // Cachear el peer actual
        this.peerActual = peer;
        
        return mapearAPeerResponseDto(peer);
    }
}
