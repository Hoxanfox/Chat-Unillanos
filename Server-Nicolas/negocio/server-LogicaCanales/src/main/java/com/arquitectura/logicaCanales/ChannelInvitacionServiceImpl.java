package com.arquitectura.logicaCanales;

import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.domain.Channel;
import com.arquitectura.domain.MembresiaCanal;
import com.arquitectura.domain.MembresiaCanalId;
import com.arquitectura.domain.User;
import com.arquitectura.domain.enums.EstadoMembresia;
import com.arquitectura.domain.enums.TipoCanal;
import com.arquitectura.events.UserInvitedEvent;
import com.arquitectura.persistence.repository.ChannelRepository;
import com.arquitectura.persistence.repository.MembresiaCanalRepository;
import com.arquitectura.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChannelInvitacionServiceImpl implements IChannelInvitationService {
    private final MembresiaCanalRepository membresiaRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final ChannelMapper channelMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ChannelInvitacionServiceImpl(MembresiaCanalRepository membresiaRepository, ChannelRepository channelRepository, UserRepository userRepository, ChannelMapper channelMapper, ApplicationEventPublisher eventPublisher) {
        this.membresiaRepository = membresiaRepository;
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.channelMapper = channelMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void invitarUsuario(UUID channelId, UUID userIdToInvite, UUID requesterId) throws Exception {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new Exception("Canal no encontrado."));
        // Validación de negocio: Solo el dueño invita
        if (!channel.getOwner().getUserId().equals(requesterId)) {
            throw new Exception("Solo el propietario del canal puede enviar invitaciones.");
        }
        if (channel.getTipo() != TipoCanal.GRUPO) {
            throw new Exception("Solo se pueden enviar invitaciones a canales de tipo GRUPO.");
        }
        // Validación: ¿Ya existe?
        MembresiaCanalId membresiaId = new MembresiaCanalId(channel.getChannelId(), userIdToInvite);
        if (membresiaRepository.existsById(membresiaId)) {
            throw new Exception("El usuario ya es miembro o tiene una invitación pendiente.");
        }
        User userToInvite = userRepository.findById(userIdToInvite)
                .orElseThrow(() -> new Exception("Usuario a invitar no encontrado."));
        // Crear la invitación
        MembresiaCanal nuevaInvitacion = new MembresiaCanal(membresiaId, userToInvite, channel, EstadoMembresia.PENDIENTE);
        membresiaRepository.save(nuevaInvitacion);
        // Publicar evento
        eventPublisher.publishEvent(new UserInvitedEvent(this, userToInvite.getUserId(), channelMapper.toChannelResponseDto(channel)));
    }

    @Override
    @Transactional
    public void responderInvitacion(UUID channelId, UUID userId, boolean aceptada) throws Exception {
        MembresiaCanalId id = new MembresiaCanalId(channelId, userId);
        MembresiaCanal invitacion = membresiaRepository.findById(id)
                .orElseThrow(() -> new Exception("No se encontró invitación."));

        if (invitacion.getEstado() != EstadoMembresia.PENDIENTE) {
            throw new Exception("No hay una invitación pendiente que responder.");
        }

        if (aceptada) {
            invitacion.setEstado(EstadoMembresia.ACTIVO);
            membresiaRepository.save(invitacion);
        } else {
            membresiaRepository.delete(invitacion);
        }

    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponseDto> obtenerInvitacionesPendientes(UUID userId) {
        return membresiaRepository.findPendingMembresiasByUserIdWithDetails(userId, EstadoMembresia.PENDIENTE)
                .stream()
                .map(MembresiaCanal::getCanal)
                .map(channelMapper::toChannelResponseDto)
                .collect(Collectors.toList());
    }

}
