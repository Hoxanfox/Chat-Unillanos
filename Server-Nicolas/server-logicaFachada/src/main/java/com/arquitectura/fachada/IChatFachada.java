package com.arquitectura.fachada;

import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;
import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.canales.CreateChannelRequestDto;
import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.DTO.canales.InviteMemberRequestDto;
import com.arquitectura.DTO.canales.RespondToInviteRequestDto;
import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface IChatFachada {

    // --- Métodos de Usuario ---
    UserResponseDto registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress) throws Exception;
    Optional<UserResponseDto> buscarUsuarioPorUsername(String username);
    List<UserResponseDto> obtenerTodosLosUsuarios();
    UserResponseDto autenticarUsuario(LoginRequestDto requestDto, String ipAddress) throws Exception;
    List<UserResponseDto> getUsersByIds(Set<Integer> userIds);
    List<UserResponseDto> obtenerUsuariosConectados();

    // --- Métodos de Canal ---
    ChannelResponseDto crearCanal(CreateChannelRequestDto requestDto, int ownerId) throws Exception;
    ChannelResponseDto crearCanalDirecto(int user1Id, int user2Id) throws Exception;
    Map<ChannelResponseDto, List<UserResponseDto>> obtenerCanalesConMiembros();
    List<ChannelResponseDto> obtenerCanalesPorUsuario(int userId);
    void invitarMiembro(InviteMemberRequestDto requestDto, int ownerId) throws Exception;
    void responderInvitacion(RespondToInviteRequestDto requestDto, int userId) throws Exception;
    List<ChannelResponseDto> getPendingInvitationsForUser(int userId);
    void agregarMiembroACanal(InviteMemberRequestDto inviteMemberRequestDto, int userId) throws Exception;
    List<ChannelResponseDto> obtenerTodosLosCanales();

    // --- MÉTODOS DE MENSAJE (ACTUALIZADOS) ---
    // Se reemplazan los dos métodos anteriores por uno solo que usa DTO
    void enviarMensajeBroadcast(String contenido, int adminId) throws Exception;
    MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, int autorId) throws Exception;
    MessageResponseDto enviarMensajeAudio(SendMessageRequestDto requestDto, int autorId) throws Exception;
    List<MessageResponseDto> obtenerMensajesDeCanal(int canalId, int userId) throws Exception;
    List<TranscriptionResponseDto> obtenerTranscripciones();

    //Metodos de Utils
    String getFileAsBase64(String relativePath)throws IOException;
    String guardarArchivoDeAudio(String fileName, String base64Data, int autorId) throws Exception;

    // --- MÉTODOS PARA INFORMES ---
    String getLogContents() throws IOException;

}