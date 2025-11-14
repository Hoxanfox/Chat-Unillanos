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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// Nuevas importaciones para los servicios extraídos
import com.arquitectura.logicaPeers.transfer.FileTransferService;
import com.arquitectura.logicaPeers.notifier.PeerNotifier;
import com.arquitectura.logicaPeers.sync.UserSyncService;

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

    // Dependencias extraídas — inyectadas por Spring (field injection para compatibilidad mínima)
    @Autowired
    private FileTransferService fileTransferService;

    @Autowired
    private PeerNotifier peerNotifier;

    @Autowired
    private UserSyncService userSyncService;

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
        
        // Verificar si el peer ya existe (ahora devuelve List)
        List<Peer> peersExistentes = peerRepository.findByIpAndPuerto(ip, puerto);
        
        if (!peersExistentes.isEmpty()) {
            // Tomar el primero y eliminar duplicados si existen
            Peer peer = peersExistentes.get(0);
            System.out.println("✓ [PeerService] Peer ya existe, actualizando: " + peer.getPeerId());
            
            // Eliminar duplicados si hay más de uno
            if (peersExistentes.size() > 1) {
                for (int i = 1; i < peersExistentes.size(); i++) {
                    Peer duplicate = peersExistentes.get(i);
                    System.out.println("!! [PeerService] Borrando peer duplicado: " + duplicate.getPeerId());
                    peerRepository.delete(duplicate);
                }
            }
            
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
        
        System.out.println("→ [PeerService] Se encontraron " + peersInactivos.size() + " peers potencialmente inactivos");
        System.out.println("→ [PeerService] Timeout configurado: " + timeoutSegundos + " segundos");

        int peersDesconectados = 0;
        for (Peer peer : peersInactivos) {
            // Verificar que NO sea el peer local
            if (peer.getPeerId().equals(obtenerPeerActualId())) {
                System.out.println("  ⚠ Ignorando peer local en verificación de inactivos");
                continue;
            }

            // Calcular tiempo desde último latido
            long segundosSinLatido = java.time.Duration.between(peer.getUltimoLatido(), LocalDateTime.now()).getSeconds();
            System.out.println("  → Peer " + peer.getIp() + ":" + peer.getPuerto() +
                " sin latido por " + segundosSinLatido + " segundos");

            peer.marcarComoOffline();
            peerRepository.save(peer);
            peersDesconectados++;
            System.out.println("  ✗ Peer marcado como OFFLINE: " + peer.getIp() + ":" + peer.getPuerto());
        }
        
        System.out.println("✓ [PeerService] Verificación completada: " + peersDesconectados + " peers desconectados");
        return peersDesconectados;
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
        // Delegar la responsabilidad al servicio especializado
        return fileTransferService.descargarArchivoDesdePeer(peerDestinoId, fileId);
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
            
            // Buscar si ya existe un peer para este servidor (ahora devuelve List)
            List<Peer> peersExistentes = peerRepository.findByIpAndPuerto(ipServidor, puerto);
            
            if (!peersExistentes.isEmpty()) {
                // Tomar el primero como el peer actual
                peerActual = peersExistentes.get(0);
                peerActual.setNombreServidor(nombreServidor);
                peerActual.marcarComoOnline();
                peerRepository.save(peerActual);
                System.out.println("✓ [PeerService] Peer actual recuperado: " + peerActual.getPeerId());

                // Eliminar duplicados si existen
                if (peersExistentes.size() > 1) {
                    for (int i = 1; i < peersExistentes.size(); i++) {
                        Peer duplicate = peersExistentes.get(i);
                        System.out.println("!! [PeerService] Borrando peer duplicado del servidor local: " + duplicate.getPeerId());
                        peerRepository.delete(duplicate);
                    }
                }
            } else {
                // Crear nuevo peer para este servidor
                peerActual = new Peer(ipServidor, puerto, nombreServidor);
                peerActual.marcarComoOnline();
                peerActual = peerRepository.save(peerActual);
                System.out.println("✓ [PeerService] Peer actual creado: " + peerActual.getPeerId());
            }

            // NUEVO: Configurar el PeerConnectionPool con el ID y puerto del peer local
            peerConnectionPool.configurarPeerLocal(peerActual.getPeerId().toString(), puerto);
            System.out.println("✓ [PeerService] PeerConnectionPool configurado con peer local: " + peerActual.getPeerId());

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

        // 1. PRIMERO: Buscar en la base de datos local
        Optional<com.arquitectura.domain.User> usuarioLocalOpt = userRepository.findByIdWithPeer(usuarioId);

        if (usuarioLocalOpt.isPresent()) {
            // Usuario encontrado en BD local
            com.arquitectura.domain.User usuario = usuarioLocalOpt.get();
            Peer peerAsociado = usuario.getPeerId();

            if (peerAsociado == null) {
                System.out.println("✗ [PeerService] Usuario local sin peer asociado");
                return new com.arquitectura.DTO.p2p.UserLocationResponseDto(
                    usuario.getUserId(),
                    usuario.getUsername(),
                    null,
                    null,
                    null,
                    usuario.getConectado()
                );
            }

            System.out.println("✓ [PeerService] Usuario encontrado en BD local, peer: " + peerAsociado.getPeerId());
            return new com.arquitectura.DTO.p2p.UserLocationResponseDto(
                usuario.getUserId(),
                usuario.getUsername(),
                peerAsociado.getPeerId(),
                peerAsociado.getIp(),
                peerAsociado.getPuerto(),
                usuario.getConectado()
            );
        }

        // 2. NO está en BD local, buscar en PEERS REMOTOS
        System.out.println("⚠ [PeerService] Usuario NO encontrado en BD local, consultando peers remotos...");

        List<Peer> peersActivos = peerRepository.findByConectado(EstadoPeer.ONLINE);
        Peer peerActual = obtenerPeerActual();

        for (Peer peerRemoto : peersActivos) {
            // Saltar el peer actual (nosotros mismos)
            if (peerRemoto.getPeerId().equals(peerActual.getPeerId())) {
                continue;
            }

            try {
                System.out.println("  ├─ Consultando peer: " + peerRemoto.getPeerId() + " (" + peerRemoto.getIp() + ":" + peerRemoto.getPuerto() + ")");

                // Crear petición para buscar usuario en peer remoto
                Map<String, Object> payload = new HashMap<>();
                payload.put("usuarioId", usuarioId.toString());

                com.arquitectura.DTO.Comunicacion.DTORequest request = new com.arquitectura.DTO.Comunicacion.DTORequest("buscarusuario", payload);

                // Enviar petición al peer remoto
                com.arquitectura.DTO.Comunicacion.DTOResponse response = peerConnectionPool.enviarPeticion(
                    peerRemoto.getIp(),
                    peerRemoto.getPuerto(),
                    request
                );

                if ("success".equals(response.getStatus()) && response.getData() != null) {
                    // Usuario encontrado en peer remoto
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userData = (Map<String, Object>) response.getData();

                    String userIdStr = (String) userData.get("usuarioId");
                    String username = (String) userData.get("username");
                    String peerIdStr = (String) userData.get("peerId");
                    String peerIp = (String) userData.get("peerIp");
                    Integer peerPuerto = userData.get("peerPuerto") != null ?
                        ((Number) userData.get("peerPuerto")).intValue() : null;
                    Boolean conectado = (Boolean) userData.get("conectado");

                    if (userIdStr != null && username != null) {
                        System.out.println("  └─ ✅ Usuario encontrado en peer remoto: " + peerRemoto.getPeerId());

                        return new com.arquitectura.DTO.p2p.UserLocationResponseDto(
                            UUID.fromString(userIdStr),
                            username,
                            peerIdStr != null ? UUID.fromString(peerIdStr) : null,
                            peerIp,
                            peerPuerto,
                            conectado != null ? conectado : false
                        );
                    }
                }

            } catch (Exception e) {
                System.err.println("  └─ ⚠ Error al consultar peer " + peerRemoto.getPeerId() + ": " + e.getMessage());
                // Continuar con el siguiente peer
            }
        }

        // 3. No encontrado en ningún peer
        System.out.println("❌ [PeerService] Usuario no encontrado en ningún peer de la red P2P");
        throw new Exception("Usuario no encontrado: " + usuarioId);
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
        List<Peer> peers = peerRepository.findByIpAndPuerto(ip, puerto);
        
        // Si hay duplicados, advertir
        if (peers.size() > 1) {
            System.out.println("⚠ [PeerService] Se encontraron " + peers.size() + " peers con la misma IP:Puerto");
        }
        
        // Devolver el primero si existe
        return peers.isEmpty() ? Optional.empty() : Optional.of(mapearAPeerResponseDto(peers.get(0)));
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

        // PASO 1: Buscar por ID primero (es lo más confiable)
        Optional<Peer> peerById = peerRepository.findById(peerId);

        if (peerById.isPresent()) {
            // El peer YA existe con este ID, solo actualizar IP y puerto si cambió
            Peer peer = peerById.get();
            System.out.println("→ [PeerService] Peer encontrado por ID: " + peerId);

            // Verificar si cambió la IP o puerto
            if (!peer.getIp().equals(ip) || peer.getPuerto() != puerto) {
                System.out.println("→ [PeerService] Actualizando ubicación del peer de " +
                    peer.getIp() + ":" + peer.getPuerto() + " a " + ip + ":" + puerto);
                peer.setIp(ip);
                peer.setPuerto(puerto);
            }

            // Limpiar duplicados por IP:Puerto si existen (pero sin borrar este)
            List<Peer> duplicadosPorIp = peerRepository.findByIpAndPuerto(ip, puerto);
            for (Peer dup : duplicadosPorIp) {
                if (!dup.getPeerId().equals(peerId)) {
                    // Hay un peer DIFERENTE con la misma IP:Puerto
                    System.out.println("⚠ [PeerService] Peer duplicado detectado: " + dup.getPeerId() +
                        " en la misma IP:Puerto que " + peerId);

                    // Verificar si el duplicado tiene usuarios asociados
                    long usuariosAsociados = userRepository.countByPeerId(dup);
                    if (usuariosAsociados > 0) {
                        System.out.println("⚠ [PeerService] Peer duplicado " + dup.getPeerId() +
                            " tiene " + usuariosAsociados + " usuarios. Migrando usuarios al peer correcto...");
                        // Migrar usuarios al peer correcto
                        migrarUsuariosDePeerAPeer(dup, peer);
                    }
                    System.out.println("!! [PeerService] Eliminando peer duplicado: " + dup.getPeerId());
                    peerRepository.delete(dup);
                }
            }

            peer.setConectado(EstadoPeer.ONLINE);
            peer.actualizarLatido();
            Peer savedPeer = peerRepository.save(peer);
            System.out.println("✓ [PeerService] Peer actualizado: " + savedPeer.getPeerId());
            return mapearAPeerResponseDto(savedPeer);
        }

        // PASO 2: El peer NO existe por ID, verificar si hay conflicto con IP:Puerto
        List<Peer> peersPorIpPuerto = peerRepository.findByIpAndPuerto(ip, puerto);

        if (!peersPorIpPuerto.isEmpty()) {
            // Ya existe(n) peer(s) en esta IP:Puerto pero con ID diferente
            System.out.println("⚠ [PeerService] Conflicto: Existen " + peersPorIpPuerto.size() +
                " peer(s) en " + ip + ":" + puerto + " pero con IDs diferentes al recibido: " + peerId);

            // IMPORTANTE: Esto significa que:
            // - O el peer se reinició y generó un nuevo UUID (MAL)
            // - O hay un peer diferente usando la misma IP:Puerto (CONFLICTO)

            // Solución: Eliminar los peers antiguos sin usuarios y crear el nuevo
            // Si tienen usuarios, migrarlos al nuevo peer

            for (Peer peerAntiguo : peersPorIpPuerto) {
                long usuariosAsociados = userRepository.countByPeerId(peerAntiguo);

                if (usuariosAsociados > 0) {
                    System.out.println("⚠ [PeerService] Peer antiguo " + peerAntiguo.getPeerId() +
                        " tiene " + usuariosAsociados + " usuarios.");
                    System.out.println("→ [PeerService] Estos usuarios quedarán huérfanos hasta que el peer correcto se conecte.");
                    System.out.println("→ [PeerService] Marcando peer antiguo como OFFLINE");

                    // NO eliminar, solo marcar como offline
                    peerAntiguo.setConectado(EstadoPeer.OFFLINE);
                    peerRepository.save(peerAntiguo);
                } else {
                    // No tiene usuarios, se puede eliminar
                    System.out.println("!! [PeerService] Eliminando peer antiguo sin usuarios: " + peerAntiguo.getPeerId());
                    peerRepository.delete(peerAntiguo);
                }
            }

            peerRepository.flush();
        }

        // PASO 3: Crear el nuevo peer con el ID que nos envía el handshake
        System.out.println("→ [PeerService] Creando nuevo peer con ID " + peerId);
        Peer nuevoPeer = new Peer(ip, puerto);
        nuevoPeer.setPeerId(peerId);
        nuevoPeer.setConectado(EstadoPeer.ONLINE);
        nuevoPeer.actualizarLatido();

        Peer savedPeer = peerRepository.save(nuevoPeer);
        System.out.println("✓ [PeerService] Nuevo peer registrado: " + savedPeer.getPeerId());

        return mapearAPeerResponseDto(savedPeer);
    }

    /**
     * Migra todos los usuarios de un peer a otro.
     * Útil cuando se detectan peers duplicados con usuarios asociados.
     */
    private void migrarUsuariosDePeerAPeer(Peer peerOrigen, Peer peerDestino) {
        try {
            List<com.arquitectura.domain.User> usuarios = userRepository.findByPeerId(peerOrigen);
            System.out.println("→ [PeerService] Migrando " + usuarios.size() +
                " usuarios de peer " + peerOrigen.getPeerId() + " a " + peerDestino.getPeerId());

            for (com.arquitectura.domain.User user : usuarios) {
                user.setPeerId(peerDestino);
                userRepository.save(user);
            }

            System.out.println("✓ [PeerService] Migración de usuarios completada");
        } catch (Exception e) {
            System.err.println("✗ [PeerService] Error migrando usuarios: " + e.getMessage());
        }
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

        List<Peer> peersExistentes = peerRepository.findByIpAndPuerto(ip, puerto);

        Peer peer;
        if (!peersExistentes.isEmpty()) {
            peer = peersExistentes.get(0);
            System.out.println("✓ [PeerService] Peer local encontrado: " + peer.getPeerId());

            // Eliminar duplicados si existen
            if (peersExistentes.size() > 1) {
                for (int i = 1; i < peersExistentes.size(); i++) {
                    Peer duplicate = peersExistentes.get(i);
                    System.out.println("!! [PeerService] Borrando peer local duplicado: " + duplicate.getPeerId());
                    peerRepository.delete(duplicate);
                }
            }
        } else {
            peer = new Peer(ip, puerto, "ONLINE");
            peer.setUltimoLatido(LocalDateTime.now());
            peer = peerRepository.save(peer);
            System.out.println("✓ [PeerService] Nuevo peer local creado: " + peer.getPeerId());
        }

        // Cachear el peer actual
        this.peerActual = peer;
        
        // IMPORTANTE: Configurar el PeerConnectionPool con el ID y puerto del peer local
        peerConnectionPool.configurarPeerLocal(peer.getPeerId().toString(), puerto);
        System.out.println("✓ [PeerService] PeerConnectionPool configurado con peer local: " + peer.getPeerId());

        return mapearAPeerResponseDto(peer);
    }

    // ==================== NOTIFICACIONES PUSH ====================

    @Override
    public void notificarCambioUsuarioATodosLosPeers(
            UUID usuarioId,
            String username,
            String nuevoEstado,
            UUID peerId,
            String peerIp,
            Integer peerPuerto) {

        // Delegar al notificador especializado
        peerNotifier.notificarCambioUsuarioATodosLosPeers(usuarioId, username, nuevoEstado, peerId, peerIp, peerPuerto);
    }

    // ==================== SINCRONIZACIÓN DE USUARIOS ====================

    @Override
    public List<java.util.Map<String, Object>> sincronizarUsuariosDeTodosLosPeers() {
        // Delegar la sincronización al servicio especializado
        return userSyncService.sincronizarUsuariosDeTodosLosPeers();
    }
}
