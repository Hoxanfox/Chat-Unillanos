package com.arquitectura.logicaUsuarios;

import com.arquitectura.DTO.peers.AddPeerRequestDto;
import com.arquitectura.DTO.peers.HeartbeatRequestDto;
import com.arquitectura.DTO.peers.HeartbeatResponseDto;
import com.arquitectura.DTO.peers.PeerResponseDto;
import com.arquitectura.DTO.peers.RetransmitRequestDto;
import com.arquitectura.DTO.peers.RetransmitResponseDto;
import com.arquitectura.DTO.peers.UpdatePeerListRequestDto;
import com.arquitectura.DTO.peers.UpdatePeerListResponseDto;
import com.arquitectura.domain.Peer;
import com.arquitectura.domain.enums.EstadoPeer;
import com.arquitectura.persistence.repository.PeerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service("peerServiceUsuarios")
public class PeerServiceImpl implements IPeerService {

    private final PeerRepository peerRepository;
    private static final long HEARTBEAT_INTERVAL_MS = 30000; // 30 segundos
    private static final long HEARTBEAT_TIMEOUT_SECONDS = 60; // 60 segundos sin latido = desconectado
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
    );

    @Autowired
    public PeerServiceImpl(PeerRepository peerRepository) {
        this.peerRepository = peerRepository;
    }

    @Override
    public List<PeerResponseDto> listarPeersDisponibles(UUID excludePeerId) {
        // Obtener todos los peers de la base de datos
        List<Peer> peers = peerRepository.findAll();

        // Filtrar el peer actual (excludePeerId) y convertir a DTO
        return peers.stream()
                .filter(peer -> !peer.getPeerId().equals(excludePeerId))
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    public HeartbeatResponseDto reportarLatido(HeartbeatRequestDto requestDto) throws Exception {
        // Validar datos
        if (requestDto.getPeerId() == null) {
            throw new IllegalArgumentException("El peerId es requerido");
        }

        if (requestDto.getIp() == null || !isValidIP(requestDto.getIp())) {
            throw new IllegalArgumentException("Formato de IP inválido");
        }

        if (requestDto.getPuerto() == null || requestDto.getPuerto() < 1 || requestDto.getPuerto() > 65535) {
            throw new IllegalArgumentException("Puerto inválido");
        }

        // Buscar el peer por ID
        Optional<Peer> peerOptional = peerRepository.findById(requestDto.getPeerId());

        if (peerOptional.isEmpty()) {
            throw new Exception("Peer no reconocido o no registrado");
        }

        Peer peer = peerOptional.get();

        // Actualizar IP y puerto si cambiaron
        peer.setIp(requestDto.getIp());
        peer.setPuerto(requestDto.getPuerto());

        // Actualizar el latido
        peer.actualizarLatido();
        peer.setConectado(EstadoPeer.ONLINE);
        peerRepository.save(peer);

        // Retornar el intervalo del próximo latido
        return new HeartbeatResponseDto(HEARTBEAT_INTERVAL_MS);
    }

    @Override
    public PeerResponseDto añadirPeer(AddPeerRequestDto requestDto) throws Exception {
        // Validar datos
        if (requestDto.getIp() == null || !isValidIP(requestDto.getIp())) {
            throw new IllegalArgumentException("Formato de IP inválido");
        }

        if (requestDto.getPuerto() == null || requestDto.getPuerto() < 1 || requestDto.getPuerto() > 65535) {
            throw new IllegalArgumentException("Puerto inválido");
        }

        // Verificar si el peer ya existe
        Optional<Peer> existingPeer = peerRepository.findByIpAndPuerto(requestDto.getIp(), requestDto.getPuerto());
        if (existingPeer.isPresent()) {
            throw new Exception("El peer ya se encuentra en la lista");
        }

        // Crear nuevo peer
        Peer newPeer = new Peer(requestDto.getIp(), requestDto.getPuerto());
        newPeer.setConectado(EstadoPeer.ONLINE);
        newPeer.setUltimoLatido(LocalDateTime.now());
        Peer savedPeer = peerRepository.save(newPeer);

        return convertirADTO(savedPeer);
    }

    @Override
    public PeerResponseDto verificarEstadoPeer(UUID peerId) throws Exception {
        if (peerId == null) {
            throw new IllegalArgumentException("El peerId es requerido");
        }

        Peer peer = peerRepository.findByPeerId(peerId);
        if (peer == null) {
            throw new Exception("Peer no encontrado");
        }

        // Verificar si el peer está desconectado (sin latido por más de HEARTBEAT_TIMEOUT_SECONDS)
        if (peer.getUltimoLatido() != null) {
            long secondsSinceLastHeartbeat = ChronoUnit.SECONDS.between(peer.getUltimoLatido(), LocalDateTime.now());
            if (secondsSinceLastHeartbeat > HEARTBEAT_TIMEOUT_SECONDS) {
                peer.setConectado(EstadoPeer.OFFLINE);
                peerRepository.save(peer);
            }
        }

        return convertirADTO(peer);
    }

    @Override
    public RetransmitResponseDto retransmitirPeticion(RetransmitRequestDto requestDto) throws Exception {
        // Validar que el peer de origen existe y está activo
        if (requestDto.getPeerOrigen() == null || requestDto.getPeerOrigen().getPeerId() == null) {
            throw new IllegalArgumentException("El peer de origen es requerido");
        }

        UUID peerOrigenId = requestDto.getPeerOrigen().getPeerId();
        Peer peerOrigen = peerRepository.findByPeerId(peerOrigenId);

        if (peerOrigen == null) {
            throw new Exception("Peer de origen no encontrado");
        }

        // Verificar que el peer de origen está conectado
        if (peerOrigen.getUltimoLatido() != null) {
            long secondsSinceLastHeartbeat = ChronoUnit.SECONDS.between(peerOrigen.getUltimoLatido(), LocalDateTime.now());
            if (secondsSinceLastHeartbeat > HEARTBEAT_TIMEOUT_SECONDS) {
                throw new Exception("Peer de origen no está conectado");
            }
        }

        // La petición del cliente se procesa en el RequestDispatcher
        // Aquí solo validamos que el peer de origen está autorizado
        // El resultado se construirá en el RequestDispatcher
        return new RetransmitResponseDto(requestDto.getPeticionCliente());
    }

    @Override
    public UpdatePeerListResponseDto actualizarListaPeers(UpdatePeerListRequestDto requestDto) throws Exception {
        // Validar que la lista no sea nula o vacía
        if (requestDto.getListaPeers() == null) {
            throw new IllegalArgumentException("La lista de peers es requerida");
        }

        // Obtener todos los peers actuales
        List<Peer> peersActuales = peerRepository.findAll();

        // Procesar cada peer de la lista recibida
        for (UpdatePeerListRequestDto.PeerInfo peerInfo : requestDto.getListaPeers()) {
            // Validar datos del peer
            if (peerInfo.getIp() == null || !isValidIP(peerInfo.getIp())) {
                continue; // Saltar peers con IP inválida
            }

            if (peerInfo.getPuerto() == null || peerInfo.getPuerto() < 1 || peerInfo.getPuerto() > 65535) {
                continue; // Saltar peers con puerto inválido
            }

            UUID peerId = null;
            if (peerInfo.getPeerId() != null && !peerInfo.getPeerId().isEmpty()) {
                try {
                    peerId = UUID.fromString(peerInfo.getPeerId());
                } catch (IllegalArgumentException e) {
                    continue; // Saltar peers con UUID inválido
                }
            }
            // Buscar si el peer ya existe (por ID o por IP+Puerto)
            Optional<Peer> existingPeer = Optional.empty();

            if (peerId != null) {
                existingPeer = peerRepository.findById(peerId);
            }

            if (existingPeer.isEmpty()) {
                existingPeer = peerRepository.findByIpAndPuerto(peerInfo.getIp(), peerInfo.getPuerto());
            }

            if (existingPeer.isPresent()) {
                // Actualizar peer existente
                Peer peer = existingPeer.get();
                peer.setIp(peerInfo.getIp());
                peer.setPuerto(peerInfo.getPuerto());

                // Convertir String a EstadoPeer
                EstadoPeer estado = parseEstadoFromString(peerInfo.getConectado());
                peer.setConectado(estado);

                // Si el peer está online, actualizar el latido
                if (estado == EstadoPeer.ONLINE) {
                    peer.actualizarLatido();
                }

                peerRepository.save(peer);
            } else {
                // Crear nuevo peer
                EstadoPeer estado = parseEstadoFromString(peerInfo.getConectado());
                Peer newPeer = new Peer(peerInfo.getIp(), peerInfo.getPuerto());
                newPeer.setConectado(estado);

                if (peerId != null) {
                    newPeer.setPeerId(peerId);
                }

                if (estado == EstadoPeer.ONLINE) {
                    newPeer.setUltimoLatido(LocalDateTime.now());
                }

                peerRepository.save(newPeer);
            }
        }

        // Retornar la lista actualizada de todos los peers
        List<Peer> peersActualizados = peerRepository.findAll();
        List<PeerResponseDto> listaPeersDto = peersActualizados.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());

        return new UpdatePeerListResponseDto(listaPeersDto);
    }

    private boolean isValidIP(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        return IP_PATTERN.matcher(ip).matches();
    }

    /**
     * Convierte un String a EstadoPeer, retornando DESCONOCIDO si el valor es inválido
     */
    private EstadoPeer parseEstadoFromString(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            return EstadoPeer.DESCONOCIDO;
        }

        try {
            return EstadoPeer.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EstadoPeer.DESCONOCIDO;
        }
    }

    private PeerResponseDto convertirADTO(Peer peer) {
        return new PeerResponseDto(
                peer.getPeerId(),
                peer.getIp(),
                peer.getPuerto(),
                peer.getConectado() != null ? peer.getConectado().name() : EstadoPeer.DESCONOCIDO.name()
        );
    }
}
