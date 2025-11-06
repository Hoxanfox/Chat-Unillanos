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


    @Autowired
    public ChannelServiceImpl(ChannelRepository channelRepository, UserRepository userRepository, 
                              MembresiaCanalRepository membresiaCanalRepository, PeerRepository peerRepository,
                              NetworkUtils networkUtils, ApplicationEventPublisher eventPublisher) {
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.membresiaCanalRepository = membresiaCanalRepository;
        this.peerRepository = peerRepository;
        this.networkUtils = networkUtils;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ChannelResponseDto crearCanal(CreateChannelRequestDto requestDto, UUID idOwner) throws Exception {
        User owner = userRepository.findById(idOwner)
                .orElseThrow(() -> new Exception("El usuario con ID " + idOwner + " no existe."));
        // Se extraen los datos del DTO
        TipoCanal tipo = TipoCanal.valueOf(requestDto.getChannelType().toUpperCase());
        if (tipo == TipoCanal.DIRECTO) {
            throw new Exception("Los canales directos deben crearse con el método crearCanalDirecto.");
        }
        
        // Obtener el Peer (servidor) actual
        String serverPeerAddress = networkUtils.getServerIPAddress();
        Peer currentPeer = peerRepository.findByIp(serverPeerAddress)
                .orElseGet(() -> peerRepository.save(new Peer(serverPeerAddress)));
        
        Channel newChannel = new Channel(requestDto.getChannelName(), owner, tipo);
        newChannel.setPeerId(currentPeer); // Asignamos el servidor padre
        
        // Se guarda primero para obtener el ID del canal
        Channel savedChannel = channelRepository.save(newChannel);
        //Creador se agrega automáticamente como miembro
        MembresiaCanal membresiaInicial = new MembresiaCanal(
                new MembresiaCanalId(savedChannel.getChannelId(), idOwner),
                owner,
                newChannel,
                EstadoMembresia.ACTIVO
        );
        membresiaCanalRepository.save(membresiaInicial);
        return mapToChannelResponseDto(savedChannel);
    }

    @Override
    @Transactional
    public ChannelResponseDto crearCanalDirecto(UUID user1Id, UUID user2Id) throws Exception {
        if (user1Id.equals(user2Id)) {
            throw new Exception("No se puede crear un canal directo con uno mismo.");
        }
        //evitar duplicados
        // 1. Buscamos en ambas direcciones (A->B y B->A) por si ya existe.
        Optional<Channel> existingChannel = channelRepository.findDirectChannelBetweenUsers(TipoCanal.DIRECTO, user1Id, user2Id);
        if (existingChannel.isPresent()) {
            Channel channel = existingChannel.get();
            System.out.println("✓ Canal directo ya existe entre " + user1Id + " y " + user2Id + ". ID: " + channel.getChannelId());
            ChannelResponseDto dto = mapToChannelResponseDto(channel);
            System.out.println("✓ Devolviendo DTO: channelId=" + dto.getChannelId() + ", channelName=" + dto.getChannelName());
            return dto;
        }
        // Hacemos la búsqueda inversa por si se creó al revés
        existingChannel = channelRepository.findDirectChannelBetweenUsers(TipoCanal.DIRECTO, user2Id, user1Id);
        if (existingChannel.isPresent()) {
            Channel channel = existingChannel.get();
            System.out.println("✓ Canal directo ya existe entre " + user2Id + " y " + user1Id + ". ID: " + channel.getChannelId());
            ChannelResponseDto dto = mapToChannelResponseDto(channel);
            System.out.println("✓ Devolviendo DTO: channelId=" + dto.getChannelId() + ", channelName=" + dto.getChannelName());
            return dto;
        }
        // Si no existe, procedemos a crear uno nuevo
        System.out.println("→ Creando nuevo canal directo entre " + user1Id + " y " + user2Id);
        User user1 = userRepository.findById(user1Id).orElseThrow(() -> new Exception("El usuario con ID " + user1Id + " no existe."));
        User user2 = userRepository.findById(user2Id).orElseThrow(() -> new Exception("El usuario con ID " + user2Id + " no existe."));
        
        // Obtener el Peer (servidor) actual
        String serverPeerAddress = networkUtils.getServerIPAddress();
        Peer currentPeer = peerRepository.findByIp(serverPeerAddress)
                .orElseGet(() -> peerRepository.save(new Peer(serverPeerAddress)));
        
        String channelName = "Directo: " + user1.getUsername() + " - " + user2.getUsername();
        Channel directChannel = new Channel(channelName, user1, TipoCanal.DIRECTO); // user1 es el "owner" simbólico
        directChannel.setPeerId(currentPeer); // Asignamos el servidor padre
        
        //guardamos el canal
        Channel savedChannel = channelRepository.save(directChannel);
        System.out.println("✓ Canal guardado con ID: " + savedChannel.getChannelId());
        // Añadimos a ambos usuarios como miembros activos
        anadirMiembroConEstado(savedChannel, user1, EstadoMembresia.ACTIVO);
        anadirMiembroConEstado(savedChannel, user2, EstadoMembresia.ACTIVO);
        System.out.println("✓ Miembros agregados al canal");
        // Volvemos a guardar para persistir las nuevas membresías
        ChannelResponseDto dto = mapToChannelResponseDto(savedChannel);
        System.out.println("✓ Devolviendo DTO: channelId=" + dto.getChannelId() + ", channelName=" + dto.getChannelName());
        return dto;
    }
    
    @Override
    @Transactional
    public void invitarMiembro(InviteMemberRequestDto inviteMemberRequestDto, UUID ownerId) throws Exception {
        Channel channel = channelRepository.findById(inviteMemberRequestDto.getChannelId())
                .orElseThrow(() -> new Exception("Canal no encontrado."));

        if (!channel.getOwner().getUserId().equals(ownerId)) {
            throw new Exception("Solo el propietario del canal puede enviar invitaciones.");
        }

        if (channel.getTipo() != TipoCanal.GRUPO) {
            throw new Exception("Solo se pueden enviar invitaciones a canales de tipo GRUPO.");
        }

        User userToInvite = userRepository.findById(inviteMemberRequestDto.getUserIdToInvite())
                .orElseThrow(() -> new Exception("Usuario a invitar no encontrado."));

        MembresiaCanalId membresiaId = new MembresiaCanalId(channel.getChannelId(), userToInvite.getUserId());

        // Verificar si ya existe una membresía
        if(membresiaCanalRepository.existsById(membresiaId)){
            throw new Exception("El usuario ya es miembro o tiene una invitación pendiente.");
        }

        MembresiaCanal nuevaInvitacion = new MembresiaCanal(membresiaId, userToInvite, channel, EstadoMembresia.PENDIENTE);
        membresiaCanalRepository.save(nuevaInvitacion);

        ChannelResponseDto channelDto = mapToChannelResponseDto(channel);
        eventPublisher.publishEvent(new UserInvitedEvent(this, userToInvite.getUserId(), channelDto));
    }

    @Override
    @Transactional
    public void responderInvitacion(RespondToInviteRequestDto requestDto, UUID userId) throws Exception {
        MembresiaCanalId membresiaId = new MembresiaCanalId(requestDto.getChannelId(), userId);

        MembresiaCanal invitacion = membresiaCanalRepository.findById(membresiaId)
                .orElseThrow(() -> new Exception("No se encontró una invitación para este usuario en este canal."));

        if (invitacion.getEstado() != EstadoMembresia.PENDIENTE) {
            throw new Exception("No hay una invitación pendiente que responder.");
        }

        if (requestDto.isAccepted()) {
            invitacion.setEstado(EstadoMembresia.ACTIVO);
            membresiaCanalRepository.save(invitacion);
        } else {
            membresiaCanalRepository.delete(invitacion);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponseDto> getPendingInvitationsForUser(UUID userId) {
        // 1. Llamamos al nuevo método que trae toda la información de una vez.
        List<MembresiaCanal> invitaciones = membresiaCanalRepository.findPendingMembresiasByUserIdWithDetails(userId, EstadoMembresia.PENDIENTE);

        // 2. La conversión a DTO ahora es 100% segura y no causará errores.
        return invitaciones.stream()
                .map(MembresiaCanal::getCanal)
                .map(this::mapToChannelResponseDto)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public Map<ChannelResponseDto, List<UserResponseDto>> obtenerCanalesConMiembros() {
        // 1. La consulta ahora trae TODOS los datos necesarios de una sola vez.
        List<Channel> canales = channelRepository.findAllWithMembresiasAndUsuarios();

        // 2. La conversión a DTOs ahora es segura y no necesita bucles intermedios.
        return canales.stream()
                .collect(Collectors.toMap(
                        this::mapToChannelResponseDto, // Esto ya no fallará
                        canal -> canal.getMembresias().stream()
                                .filter(membresia -> membresia.getEstado() == EstadoMembresia.ACTIVO)
                                .map(membresia -> mapToUserResponseDto(membresia.getUsuario())) // Esto tampoco fallará
                                .collect(Collectors.toList())
                ));
    }
    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponseDto> obtenerCanalesPorUsuario(UUID userId) {
        // 1. Llamamos al nuevo método que nos trae toda la información de una vez.
        List<MembresiaCanal> membresias = membresiaCanalRepository.findActiveMembresiasByUserIdWithDetails(userId, EstadoMembresia.ACTIVO);

        // 2. La conversión es ahora 100% segura. Ya no se necesita forzar la carga
        //    con "owner.getUsername()" porque los datos ya vienen completos desde la BD.
        return membresias.stream()
                .map(MembresiaCanal::getCanal)      // De cada membresía, obtenemos el canal
                .map(this::mapToChannelResponseDto) // Convertimos la entidad a DTO (¡Sin errores!)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponseDto> obtenerTodosLosCanales() {
        return channelRepository.findAll().stream()
                .map(this::mapToChannelResponseDto)
                .collect(Collectors.toList());
    }


    private ChannelResponseDto mapToChannelResponseDto(Channel channel) {
        UserResponseDto ownerDto = mapToUserResponseDto(channel.getOwner());

        // Obtener el peerId si existe, sino null
        UUID peerId = channel.getPeerId() != null ? channel.getPeerId().getPeerId() : null;

        return new ChannelResponseDto(
                channel.getChannelId(),
                channel.getName(),
                channel.getTipo().toString(),
                ownerDto,
                peerId
        );
    }
    private UserResponseDto mapToUserResponseDto(User user) {
        return new UserResponseDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhotoAddress(),
                user.getFechaRegistro()
        );
    }
    private void anadirMiembroConEstado(Channel channel, User user, EstadoMembresia estado) {
        MembresiaCanalId membresiaId = new MembresiaCanalId(channel.getChannelId(), user.getUserId());
        MembresiaCanal nuevaMembresia = new MembresiaCanal(membresiaId, user, channel, estado);
        membresiaCanalRepository.save(nuevaMembresia);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId) throws Exception {
        // 1. Validar que el canal exista
        Channel channel = channelRepository.findById(canalId)
                .orElseThrow(() -> new IllegalArgumentException("Canal no encontrado"));

        // 2. Validar que el solicitante sea miembro del canal
        boolean isMember = membresiaCanalRepository
                .findAllByUsuarioUserIdAndEstado(solicitanteId, EstadoMembresia.ACTIVO)
                .stream()
                .anyMatch(m -> m.getCanal().getChannelId().equals(canalId));

        if (!isMember) {
            throw new IllegalArgumentException("No eres miembro de este canal");
        }

        // 3. Obtener todas las membresías ACTIVAS del canal
        List<MembresiaCanal> membresias = membresiaCanalRepository
                .findAllByCanal_ChannelIdAndEstado(canalId, EstadoMembresia.ACTIVO);

        // 4. Convertir a UserResponseDto
        return membresias.stream()
                .map(membresia -> {
                    User usuario = membresia.getUsuario();
                    UserResponseDto dto = mapToUserResponseDto(usuario);
                    
                    // Determinar el rol (si es el owner del canal, es ADMIN)
                    if (channel.getOwner().getUserId().equals(usuario.getUserId())) {
                        dto.setRol("ADMIN");
                    } else {
                        dto.setRol("MIEMBRO");
                    }
                    
                    // Agregar estado de conexión
                    dto.setEstado(usuario.getConectado() != null ? usuario.getConectado().toString() : "false");
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }

}