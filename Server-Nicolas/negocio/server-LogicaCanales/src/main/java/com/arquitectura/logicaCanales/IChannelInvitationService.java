package com.arquitectura.logicaCanales;

import com.arquitectura.DTO.canales.ChannelResponseDto;

import java.util.List;
import java.util.UUID;

public interface IChannelInvitationService {
    void invitarUsuario(UUID channelId, UUID userIdToInvite, UUID requesterId) throws Exception;
    void responderInvitacion(UUID channelId, UUID userId, boolean aceptada) throws Exception;
    List<ChannelResponseDto> obtenerInvitacionesPendientes(UUID userId);
}