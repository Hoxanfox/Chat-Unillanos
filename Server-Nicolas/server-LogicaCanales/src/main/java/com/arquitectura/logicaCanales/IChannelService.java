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

public interface IChannelService {

    ChannelResponseDto crearCanal(CreateChannelRequestDto requestDto, int idOwner) throws Exception;
    //chats 1 a 1
    ChannelResponseDto crearCanalDirecto(int user1Id, int user2Id) throws Exception;

    void invitarMiembro(InviteMemberRequestDto requestDto, int ownerId) throws Exception;
    void responderInvitacion(RespondToInviteRequestDto requestDto, int userId) throws Exception;
    List<ChannelResponseDto> getPendingInvitationsForUser(int userId);
    List<ChannelResponseDto> obtenerCanalesPorUsuario(int userId);


    List<ChannelResponseDto> obtenerTodosLosCanales();

    Map<ChannelResponseDto, List<UserResponseDto>> obtenerCanalesConMiembros();

}