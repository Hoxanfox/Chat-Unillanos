package com.arquitectura.fachada.canal;

import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.canales.CreateChannelRequestDto;
import com.arquitectura.DTO.canales.InviteMemberRequestDto;
import com.arquitectura.DTO.canales.RespondToInviteRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fachada especializada para operaciones relacionadas con canales.
 */
public interface ICanalFachada {

    /**
     * Crea un nuevo canal.
     * @param requestDto Datos del canal a crear
     * @param ownerId ID del propietario del canal
     * @return Datos del canal creado
     * @throws Exception si hay error en la creación
     */
    ChannelResponseDto crearCanal(CreateChannelRequestDto requestDto, UUID ownerId) throws Exception;

    /**
     * Crea un canal directo entre dos usuarios.
     * @param user1Id ID del primer usuario
     * @param user2Id ID del segundo usuario
     * @return Datos del canal directo creado
     * @throws Exception si hay error en la creación
     */
    ChannelResponseDto crearCanalDirecto(UUID user1Id, UUID user2Id) throws Exception;

    /**
     * Obtiene todos los canales con sus miembros.
     * @return Mapa con canales y sus listas de miembros
     */
    Map<ChannelResponseDto, List<UserResponseDto>> obtenerCanalesConMiembros();

    /**
     * Obtiene los canales de un usuario específico.
     * @param userId ID del usuario
     * @return Lista de canales del usuario
     */
    List<ChannelResponseDto> obtenerCanalesPorUsuario(UUID userId);

    /**
     * Invita un miembro a un canal.
     * @param requestDto Datos de la invitación
     * @param ownerId ID del propietario que invita
     * @throws Exception si hay error en la invitación
     */
    void invitarMiembro(InviteMemberRequestDto requestDto, UUID ownerId) throws Exception;

    /**
     * Responde a una invitación de canal.
     * @param requestDto Respuesta a la invitación
     * @param userId ID del usuario que responde
     * @throws Exception si hay error al responder
     */
    void responderInvitacion(RespondToInviteRequestDto requestDto, UUID userId) throws Exception;

    /**
     * Obtiene las invitaciones pendientes de un usuario.
     * @param userId ID del usuario
     * @return Lista de canales con invitaciones pendientes
     */
    List<ChannelResponseDto> getPendingInvitationsForUser(UUID userId);

    /**
     * Agrega un miembro directamente a un canal.
     * @param inviteMemberRequestDto Datos del miembro a agregar
     * @param userId ID del usuario administrador
     * @throws Exception si hay error al agregar el miembro
     */
    void agregarMiembroACanal(InviteMemberRequestDto inviteMemberRequestDto, UUID userId) throws Exception;

    /**
     * Obtiene todos los canales registrados.
     * @return Lista de todos los canales
     */
    List<ChannelResponseDto> obtenerTodosLosCanales();

    /**
     * Obtiene la lista de miembros de un canal.
     * @param canalId El ID del canal
     * @param solicitanteId El ID del usuario que solicita la lista
     * @return Lista de usuarios que son miembros del canal
     * @throws Exception si el canal no existe o el solicitante no es miembro
     */
    List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId) throws Exception;
}
