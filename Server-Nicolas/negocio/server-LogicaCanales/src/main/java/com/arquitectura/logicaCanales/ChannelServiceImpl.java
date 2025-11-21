package com.arquitectura.logicaCanales;

import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.canales.CreateChannelRequestDto;
import com.arquitectura.DTO.canales.InviteMemberRequestDto;
import com.arquitectura.DTO.canales.RespondToInviteRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.Channel;
import com.arquitectura.domain.MembresiaCanal;
import com.arquitectura.domain.MembresiaCanalId;
import com.arquitectura.domain.Peer;
import com.arquitectura.domain.User;
import com.arquitectura.domain.enums.EstadoMembresia;
import com.arquitectura.domain.enums.TipoCanal;
import com.arquitectura.events.UserInvitedEvent;
import com.arquitectura.persistence.repository.ChannelRepository;
import com.arquitectura.persistence.repository.MembresiaCanalRepository;
import com.arquitectura.persistence.repository.PeerRepository;
import com.arquitectura.persistence.repository.UserRepository;
import com.arquitectura.utils.network.NetworkUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChannelServiceImpl implements IChannelService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final MembresiaCanalRepository membresiaCanalRepository;
    private final PeerRepository peerRepository;
    private final NetworkUtils networkUtils;
    private final ApplicationEventPublisher eventPublisher;
    private final ChannelMapper channelMapper;
    private final IChannelInvitationService invitationService;

    @Autowired
    public ChannelServiceImpl(ChannelRepository channelRepository, UserRepository userRepository,
                              MembresiaCanalRepository membresiaCanalRepository, PeerRepository peerRepository,
                              NetworkUtils networkUtils, ApplicationEventPublisher eventPublisher, ChannelMapper channelMapper, @Lazy IChannelInvitationService invitationService) {
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.membresiaCanalRepository = membresiaCanalRepository;
        this.peerRepository = peerRepository;
        this.networkUtils = networkUtils;
        this.eventPublisher = eventPublisher;
        this.channelMapper = channelMapper;
        this.invitationService = invitationService;
    }

    @Override
    @Transactional
    public ChannelResponseDto crearCanal(CreateChannelRequestDto requestDto, UUID idOwner) throws Exception {
        User owner = userRepository.findById(idOwner)
                .orElseThrow(() -> new Exception("El usuario con ID " + idOwner + " no existe."));
        TipoCanal tipo = TipoCanal.valueOf(requestDto.getChannelType().toUpperCase());
        if (tipo == TipoCanal.DIRECTO) {
            throw new Exception("Los canales directos deben crearse con el método crearCanalDirecto.");
        }
        // Asignar Peer actual
        String serverPeerAddress = networkUtils.getServerIPAddress();
        Peer currentPeer = peerRepository.findByIp(serverPeerAddress)
                .orElseGet(() -> peerRepository.save(new Peer(serverPeerAddress, 9000, "ONLINE")));
        Channel newChannel = new Channel(requestDto.getChannelName(), owner, tipo);
        newChannel.setPeerId(currentPeer);
        Channel savedChannel = channelRepository.save(newChannel);
        // Agregar al creador como miembro automáticamente
        anadirMiembroConEstado(savedChannel, owner, EstadoMembresia.ACTIVO);

        return channelMapper.toChannelResponseDto(savedChannel);
    }

    // Devuelve el DTO del canal directo
    @Override
    @Transactional
    public ChannelResponseDto crearCanalDirecto(UUID user1Id, UUID user2Id) throws Exception {
        Channel channel = obtenerOCrearCanalDirecto(user1Id, user2Id);
        return channelMapper.toChannelResponseDto(channel);
    }
    //Devuelve la entidad Channel directamente
    @Override
    @Transactional
    public Channel obtenerOCrearCanalDirecto(UUID user1Id, UUID user2Id) throws Exception {
        if (user1Id.equals(user2Id)) {
            throw new Exception("No se puede crear un canal directo con uno mismo.");
        }
        // 1. Buscar si ya existe
        Optional<Channel> existingChannel = channelRepository.findDirectChannelBetweenUsers(TipoCanal.DIRECTO, user1Id, user2Id);
        if (existingChannel.isPresent()) {
            return existingChannel.get();
        }
        // Hacemos la búsqueda inversa por si se creó al revés
        existingChannel = channelRepository.findDirectChannelBetweenUsers(TipoCanal.DIRECTO, user2Id, user1Id);
        if (existingChannel.isPresent()) {
            return existingChannel.get();
        }
        // Si no existe, procedemos a crear uno nuevo
        System.out.println("→ Creando nuevo canal directo entre " + user1Id + " y " + user2Id);
        User user1 = userRepository.findById(user1Id).orElseThrow(() -> new Exception("El usuario con ID " + user1Id + " no existe."));
        User user2 = userRepository.findById(user2Id).orElseThrow(() -> new Exception("El usuario con ID " + user2Id + " no existe."));
        // Obtener el Peer (servidor) actual
        String serverPeerAddress = networkUtils.getServerIPAddress();
        Peer currentPeer = peerRepository.findByIp(serverPeerAddress)
                .orElseGet(() -> peerRepository.save(new Peer(serverPeerAddress, 9000, "ONLINE")));
        String channelName = "Directo: " + user1.getUsername() + " - " + user2.getUsername();
        Channel directChannel = new Channel(channelName, user1, TipoCanal.DIRECTO);
        directChannel.setPeerId(currentPeer);
        // Guardamos el canal
        Channel savedChannel = channelRepository.save(directChannel);
        System.out.println("✓ Canal guardado con ID: " + savedChannel.getChannelId());
        // Añadimos a ambos usuarios como miembros activos
        anadirMiembroConEstado(savedChannel, user1, EstadoMembresia.ACTIVO);
        anadirMiembroConEstado(savedChannel, user2, EstadoMembresia.ACTIVO);

        return savedChannel;
    }
    // CONSULTAS (Usando el Mapper)
    //--------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponseDto> obtenerCanalesPorUsuario(UUID userId) {
        return membresiaCanalRepository.findActiveMembresiasByUserIdWithDetails(userId, EstadoMembresia.ACTIVO)
                .stream()
                .map(MembresiaCanal::getCanal)
                .map(channelMapper::toChannelResponseDto)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponseDto> obtenerTodosLosCanales() {
        return channelRepository.findAll().stream()
                .map(channelMapper::toChannelResponseDto)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public Map<ChannelResponseDto, List<UserResponseDto>> obtenerCanalesConMiembros() {
        List<Channel> canales = channelRepository.findAllWithMembresiasAndUsuarios();

        return canales.stream()
                .collect(Collectors.toMap(
                        channelMapper::toChannelResponseDto,
                        canal -> canal.getMembresias().stream()
                                .filter(membresia -> membresia.getEstado() == EstadoMembresia.ACTIVO)
                                .map(membresia -> channelMapper.toUserResponseDto(membresia.getUsuario()))
                                .collect(Collectors.toList())
                ));
    }
    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId) throws Exception {
        // Validar que el canal exista
        Channel channel = channelRepository.findById(canalId)
                .orElseThrow(() -> new IllegalArgumentException("Canal no encontrado"));

        // Validar membresía del solicitante
        boolean isMember = membresiaCanalRepository
                .findAllByUsuarioUserIdAndEstado(solicitanteId, EstadoMembresia.ACTIVO)
                .stream()
                .anyMatch(m -> m.getCanal().getChannelId().equals(canalId));

        if (!isMember) {
            throw new IllegalArgumentException("No eres miembro de este canal");
        }
        // Retornar miembros mapeados
        return membresiaCanalRepository
                .findAllByCanal_ChannelIdAndEstado(canalId, EstadoMembresia.ACTIVO)
                .stream()
                .map(membresia -> {
                    UserResponseDto dto = channelMapper.toUserResponseDto(membresia.getUsuario());
                    // Lógica extra de rol que no está en el mapper genérico
                    if (channel.getOwner().getUserId().equals(membresia.getUsuario().getUserId())) {
                        dto.setRol("ADMIN");
                    } else {
                        dto.setRol("MIEMBRO");
                    }
                    // Estado de conexión
                    dto.setEstado(membresia.getUsuario().getConectado() != null ?
                            membresia.getUsuario().getConectado().toString() : "false");
                    return dto;
                })
                .collect(Collectors.toList());
    }
    //delegamos la invitación al servicio especializado
    //--------------------------------------------------------------
    @Override
    public void invitarMiembro(InviteMemberRequestDto requestDto, UUID ownerId) throws Exception {
        invitationService.invitarUsuario(requestDto.getChannelId(), requestDto.getUserIdToInvite(), ownerId);
    }
    @Override
    public void responderInvitacion(RespondToInviteRequestDto requestDto, UUID userId) throws Exception {
        invitationService.responderInvitacion(requestDto.getChannelId(), userId, requestDto.isAccepted());
    }
    @Override
    public List<ChannelResponseDto> getPendingInvitationsForUser(UUID userId) {
        return invitationService.obtenerInvitacionesPendientes(userId);
    }
    // MÉTODOS AUXILIARES
    private void anadirMiembroConEstado(Channel channel, User user, EstadoMembresia estado) {
        MembresiaCanalId membresiaId = new MembresiaCanalId(channel.getChannelId(), user.getUserId());
        MembresiaCanal nuevaMembresia = new MembresiaCanal(membresiaId, user, channel, estado);
        membresiaCanalRepository.save(nuevaMembresia);
    }

}

