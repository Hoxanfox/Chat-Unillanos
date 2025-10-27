package com.arquitectura.logicaCanales;

import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.canales.CreateChannelRequestDto; // <-- IMPORT AÑADIDO
import com.arquitectura.DTO.canales.InviteMemberRequestDto;
import com.arquitectura.DTO.canales.RespondToInviteRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.Channel;
import com.arquitectura.domain.MembresiaCanal;
import com.arquitectura.domain.MembresiaCanalId;
import com.arquitectura.domain.User;
import com.arquitectura.domain.enums.EstadoMembresia;
import com.arquitectura.domain.enums.TipoCanal; // <-- IMPORT AÑADIDO
import com.arquitectura.events.UserInvitedEvent;
import com.arquitectura.persistence.repository.ChannelRepository;
import com.arquitectura.persistence.repository.MembresiaCanalRepository;
import com.arquitectura.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChannelServiceImpl implements IChannelService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final MembresiaCanalRepository membresiaCanalRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Autowired
    public ChannelServiceImpl(ChannelRepository channelRepository, UserRepository userRepository, MembresiaCanalRepository membresiaCanalRepository, ApplicationEventPublisher eventPublisher) {
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.membresiaCanalRepository = membresiaCanalRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ChannelResponseDto crearCanal(CreateChannelRequestDto requestDto, int idOwner) throws Exception {
        User owner = userRepository.findById(idOwner)
                .orElseThrow(() -> new Exception("El usuario con ID " + idOwner + " no existe."));
        // Se extraen los datos del DTO
        TipoCanal tipo = TipoCanal.valueOf(requestDto.getChannelType().toUpperCase());
        if (tipo == TipoCanal.DIRECTO) {
            throw new Exception("Los canales directos deben crearse con el método crearCanalDirecto.");
        }
        Channel newChannel = new Channel(requestDto.getChannelName(), owner, tipo);
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
    public ChannelResponseDto crearCanalDirecto(int user1Id, int user2Id) throws Exception {
        if (user1Id == user2Id) {
            throw new Exception("No se puede crear un canal directo con uno mismo.");
        }
        //evitar duplicados
        // 1. Buscamos en ambas direcciones (A->B y B->A) por si ya existe.
        Optional<Channel> existingChannel = channelRepository.findDirectChannelBetweenUsers(TipoCanal.DIRECTO, user1Id, user2Id);
        if (existingChannel.isPresent()) {
            System.out.println("Canal directo ya existe entre " + user1Id + " y " + user2Id + ". Devolviendo existente.");
            return mapToChannelResponseDto(existingChannel.get());
        }
        // Hacemos la búsqueda inversa por si se creó al revés
        existingChannel = channelRepository.findDirectChannelBetweenUsers(TipoCanal.DIRECTO, user2Id, user1Id);
        if (existingChannel.isPresent()) {
            System.out.println("Canal directo ya existe entre " + user2Id + " y " + user1Id + ". Devolviendo existente.");
            return mapToChannelResponseDto(existingChannel.get());
        }
        // Si no existe, procedemos a crear uno nuevo
        User user1 = userRepository.findById(user1Id).orElseThrow(() -> new Exception("El usuario con ID " + user1Id + " no existe."));
        User user2 = userRepository.findById(user2Id).orElseThrow(() -> new Exception("El usuario con ID " + user2Id + " no existe."));
        String channelName = "Directo: " + user1.getUsername() + " - " + user2.getUsername();
        Channel directChannel = new Channel(channelName, user1, TipoCanal.DIRECTO); // user1 es el "owner" simbólico
        //guardamos el canal
        Channel savedChannel = channelRepository.save(directChannel);
        // Añadimos a ambos usuarios como miembros activos
        anadirMiembroConEstado(savedChannel, user1, EstadoMembresia.ACTIVO);
        anadirMiembroConEstado(savedChannel, user2, EstadoMembresia.ACTIVO);
        // Volvemos a guardar para persistir las nuevas membresías
        return mapToChannelResponseDto(savedChannel);
    }
    
    @Override
    @Transactional
    public void invitarMiembro(InviteMemberRequestDto inviteMemberRequestDto,int ownerId) throws Exception {
        Channel channel = channelRepository.findById(inviteMemberRequestDto.getChannelId())
                .orElseThrow(() -> new Exception("Canal no encontrado."));

        if (channel.getOwner().getUserId() != ownerId) {
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
    public void responderInvitacion(RespondToInviteRequestDto requestDto, int userId) throws Exception {
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
    public List<ChannelResponseDto> getPendingInvitationsForUser(int userId) {
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
    public List<ChannelResponseDto> obtenerCanalesPorUsuario(int userId) {
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

        return new ChannelResponseDto(
                channel.getChannelId(),
                channel.getName(),
                channel.getTipo().toString(),
                ownerDto
        );
    }
    private UserResponseDto mapToUserResponseDto(User user) {
        return new UserResponseDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhotoAddress()
        );
    }
    private void anadirMiembroConEstado(Channel channel, User user, EstadoMembresia estado) {
        MembresiaCanalId membresiaId = new MembresiaCanalId(channel.getChannelId(), user.getUserId());
        MembresiaCanal nuevaMembresia = new MembresiaCanal(membresiaId, user, channel, estado);
        membresiaCanalRepository.save(nuevaMembresia);
    }

}