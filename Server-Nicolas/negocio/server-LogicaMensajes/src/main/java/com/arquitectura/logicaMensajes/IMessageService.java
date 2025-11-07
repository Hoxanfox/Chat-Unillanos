package com.arquitectura.logicaMensajes;

import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;
import com.arquitectura.domain.Message;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface IMessageService {

    /**
     * Guarda un nuevo mensaje de texto en un canal.
     * @param requestDto El DTO que contiene la información del mensaje a enviar.
     * @param autorId El ID del usuario que envía el mensaje.
     * @return El mensaje de texto guardado.
     * @throws Exception si el autor o el canal no existen.
     */
    MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, UUID autorId) throws Exception;

    /**
     * Guarda un nuevo mensaje de audio en un canal.
     * @param requestDto El DTO que contiene la información del mensaje a enviar.
     * @param autorId El ID del usuario que envía el mensaje.
     * @return El mensaje de audio guardado.
     * @throws Exception si el autor o el canal no existen.
     */
    MessageResponseDto enviarMensajeAudio(SendMessageRequestDto requestDto, UUID autorId) throws Exception;

    /**
     * Obtiene el historial de mensajes de un canal específico.
     * @param canalId El ID del canal.
     * @return Una lista con los mensajes del canal.
     */
    List<MessageResponseDto> obtenerMensajesPorCanal(UUID canalId, UUID userId) throws Exception;

    /**
     * Obtiene el historial de mensajes privados entre dos usuarios.
     * @param remitenteId ID del usuario que solicita el historial
     * @param destinatarioId ID del contacto con quien tiene la conversación
     * @return Lista de MessageResponseDto con el historial privado
     * @throws Exception en caso de error
     */
    List<MessageResponseDto> obtenerHistorialPrivado(UUID remitenteId, UUID destinatarioId) throws Exception;

    void enviarMensajeBroadcast(String contenido, UUID adminId) throws Exception;

    List<TranscriptionResponseDto> getAllTranscriptions();
    
    /**
     * Guarda un mensaje recibido de otro servidor P2P en la base de datos local.
     * NO publica eventos locales (el mensaje ya fue procesado en el servidor de origen).
     * 
     * @param messageId ID único del mensaje (mismo que en servidor origen)
     * @param channelId ID del canal
     * @param authorId ID del usuario autor del mensaje
     * @param content Contenido del mensaje (texto o ruta de archivo de audio)
     * @param messageType Tipo de mensaje ("TEXT" o "AUDIO")
     * @param timestamp Timestamp original del mensaje
     * @throws Exception si hay error al guardar
     */
    void guardarMensajeRemoto(UUID messageId, UUID channelId, UUID authorId,
                             String content, String messageType,
                             java.time.LocalDateTime timestamp) throws Exception;
}