package com.arquitectura.fachada.canal;

import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.canales.CreateChannelRequestDto;
import com.arquitectura.DTO.canales.InviteMemberRequestDto;
import com.arquitectura.DTO.canales.RespondToInviteRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.logicaCanales.IChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación de la fachada de canales.
 * Coordina las operaciones relacionadas con canales del sistema.
 */
@Component
public class CanalFachadaImpl implements ICanalFachada {

    private final IChannelService channelService;

    @Autowired
    public CanalFachadaImpl(IChannelService channelService) {
        this.channelService = channelService;
    }

    @Override
    public ChannelResponseDto crearCanal(CreateChannelRequestDto requestDto, UUID ownerId) throws Exception {
        return channelService.crearCanal(requestDto, ownerId);
    }

    @Override
    public ChannelResponseDto crearCanalDirecto(UUID user1Id, UUID user2Id) throws Exception {
        return channelService.crearCanalDirecto(user1Id, user2Id);
    }

    @Override
    public Map<ChannelResponseDto, List<UserResponseDto>> obtenerCanalesConMiembros() {
        return channelService.obtenerCanalesConMiembros();
    }

    @Override
    public List<ChannelResponseDto> obtenerCanalesPorUsuario(UUID userId) {
        return channelService.obtenerCanalesPorUsuario(userId);
    }

    @Override
    public void invitarMiembro(InviteMemberRequestDto requestDto, UUID ownerId) throws Exception {
        channelService.invitarMiembro(requestDto, ownerId);
    }

    @Override
    public void responderInvitacion(RespondToInviteRequestDto requestDto, UUID userId) throws Exception {
        channelService.responderInvitacion(requestDto, userId);
    }

    @Override
    public List<ChannelResponseDto> getPendingInvitationsForUser(UUID userId) {
        return channelService.getPendingInvitationsForUser(userId);
    }

    @Override
    public void agregarMiembroACanal(InviteMemberRequestDto inviteMemberRequestDto, UUID userId) throws Exception {
        channelService.invitarMiembro(inviteMemberRequestDto, userId);
    }

    @Override
    public List<ChannelResponseDto> obtenerTodosLosCanales() {
        return channelService.obtenerTodosLosCanales();
    }

    @Override
    public List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId) throws Exception {
        return channelService.obtenerMiembrosDeCanal(canalId, solicitanteId);
    }
}

