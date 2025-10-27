package com.arquitectura.logicaMensajes;

import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.Mensajes.SendMessageRequestDto; // <-- IMPORT AÑADIDO
import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;
import com.arquitectura.domain.Message;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IMessageService {

    /**
     * Guarda un nuevo mensaje de texto en un canal.
     * @param requestDto El DTO que contiene la información del mensaje a enviar.
     * @param autorId El ID del usuario que envía el mensaje.
     * @return El mensaje de texto guardado.
     * @throws Exception si el autor o el canal no existen.
     */
    MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, int autorId) throws Exception;

    /**
     * Guarda un nuevo mensaje de audio en un canal.
     * @param requestDto El DTO que contiene la información del mensaje a enviar.
     * @param autorId El ID del usuario que envía el mensaje.
     * @return El mensaje de audio guardado.
     * @throws Exception si el autor o el canal no existen.
     */
    MessageResponseDto enviarMensajeAudio(SendMessageRequestDto requestDto, int autorId) throws Exception;

    /**
     * Obtiene el historial de mensajes de un canal específico.
     * @param canalId El ID del canal.
     * @return Una lista con los mensajes del canal.
     */
    List<MessageResponseDto> obtenerMensajesPorCanal(int canalId,int UserId) throws Exception;
    void enviarMensajeBroadcast(String contenido, int adminId) throws Exception;

    List<TranscriptionResponseDto> getAllTranscriptions();
}