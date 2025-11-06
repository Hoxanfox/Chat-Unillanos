package com.arquitectura.logicaCanales;

import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.canales.CreateChannelRequestDto;
import com.arquitectura.DTO.canales.InviteMemberRequestDto;
import com.arquitectura.DTO.canales.RespondToInviteRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.Channel;
import com.arquitectura.domain.MembresiaCanal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IChannelService {

    ChannelResponseDto crearCanal(CreateChannelRequestDto requestDto, UUID idOwner) throws Exception;
    //chats 1 a 1
    ChannelResponseDto crearCanalDirecto(UUID user1Id, UUID user2Id) throws Exception;

    void invitarMiembro(InviteMemberRequestDto requestDto, UUID ownerId) throws Exception;
    void responderInvitacion(RespondToInviteRequestDto requestDto, UUID userId) throws Exception;
    List<ChannelResponseDto> getPendingInvitationsForUser(UUID userId);
    List<ChannelResponseDto> obtenerCanalesPorUsuario(UUID userId);


    List<ChannelResponseDto> obtenerTodosLosCanales();

    Map<ChannelResponseDto, List<UserResponseDto>> obtenerCanalesConMiembros();

    /**
     * Obtiene la lista de miembros de un canal.
     * @param canalId El ID del canal.
     * @param solicitanteId El ID del usuario que solicita la lista.
     * @return Lista de usuarios que son miembros del canal.
     * @throws Exception si el canal no existe o el solicitante no es miembro.
     */
    List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId) throws Exception;

}