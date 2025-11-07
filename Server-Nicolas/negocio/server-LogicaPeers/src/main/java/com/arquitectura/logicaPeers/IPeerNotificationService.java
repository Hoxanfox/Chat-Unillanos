package com.arquitectura.logicaPeers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;

import java.util.UUID;

/**
 * Interfaz para el servicio de notificaciones entre peers.
 * Maneja la comunicación transparente entre servidores para mensajes, canales e invitaciones.
 */
public interface IPeerNotificationService {

    /**
     * Notifica a un peer sobre un nuevo mensaje destinado a un usuario en ese servidor.
     * @param peerDestinoId ID del servidor destino
     * @param mensaje DTO del mensaje a enviar
     * @return true si la notificación fue exitosa
     */
    boolean notificarNuevoMensaje(UUID peerDestinoId, MessageResponseDto mensaje);

    /**
     * Notifica a un peer sobre una invitación a canal para un usuario en ese servidor.
     * @param peerDestinoId ID del servidor destino
     * @param canalId ID del canal
     * @param usuarioInvitadoId ID del usuario invitado
     * @param usuarioInvitadorId ID del usuario que invita
     * @return true si la notificación fue exitosa
     */
    boolean notificarInvitacionCanal(UUID peerDestinoId, UUID canalId,
                                     UUID usuarioInvitadoId, UUID usuarioInvitadorId);

    /**
     * Notifica a un peer sobre la aceptación de una invitación a canal.
     * @param peerDestinoId ID del servidor destino
     * @param canalId ID del canal
     * @param usuarioId ID del usuario que aceptó
     * @return true si la notificación fue exitosa
     */
    boolean notificarAceptacionInvitacion(UUID peerDestinoId, UUID canalId, UUID usuarioId);

    /**
     * Solicita a un peer la información de un usuario.
     * @param peerDestinoId ID del servidor destino
     * @param usuarioId ID del usuario a buscar
     * @return DTO del usuario o null si no se encontró
     */
    UserResponseDto solicitarInfoUsuario(UUID peerDestinoId, UUID usuarioId);

    /**
     * Solicita a un peer la información de un canal.
     * @param peerDestinoId ID del servidor destino
     * @param canalId ID del canal a buscar
     * @return DTO del canal o null si no se encontró
     */
    ChannelResponseDto solicitarInfoCanal(UUID peerDestinoId, UUID canalId);

    /**
     * Envía una petición genérica a otro peer y espera respuesta.
     * @param peerDestinoId ID del servidor destino
     * @param peticion Petición a enviar
     * @return Respuesta del peer
     * @throws Exception si hay error en la comunicación
     */
    DTOResponse enviarPeticionAPeer(UUID peerDestinoId, DTORequest peticion) throws Exception;

    /**
     * Verifica si un usuario pertenece a un servidor específico.
     * @param usuarioId ID del usuario
     * @param peerId ID del servidor
     * @return true si el usuario pertenece a ese servidor
     */
    boolean usuarioPerteneceAPeer(UUID usuarioId, UUID peerId);

    /**
     * Obtiene el ID del servidor al que pertenece un usuario.
     * @param usuarioId ID del usuario
     * @return ID del servidor o null si no se encontró
     */
    UUID obtenerPeerDeUsuario(UUID usuarioId);

    /**
     * Obtiene el ID del servidor al que pertenece un canal.
     * @param canalId ID del canal
     * @return ID del servidor o null si no se encontró
     */
    UUID obtenerPeerDeCanal(UUID canalId);
}

