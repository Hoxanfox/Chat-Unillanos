package com.arquitectura.fachada.mensaje;

import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Fachada especializada para operaciones relacionadas con mensajes.
 */
public interface IMensajeFachada {

    /**
     * Envía un mensaje broadcast a todos los usuarios (solo admin).
     * @param contenido Contenido del mensaje
     * @param adminId ID del administrador
     * @throws Exception si el usuario no es admin o hay error al enviar
     */
    void enviarMensajeBroadcast(String contenido, UUID adminId) throws Exception;

    /**
     * Envía un mensaje de texto a un canal.
     * @param requestDto Datos del mensaje
     * @param autorId ID del autor del mensaje
     * @return Datos del mensaje enviado
     * @throws Exception si hay error al enviar
     */
    MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, UUID autorId) throws Exception;

    /**
     * Envía un mensaje de audio a un canal.
     * @param requestDto Datos del mensaje con audio
     * @param autorId ID del autor del mensaje
     * @return Datos del mensaje enviado
     * @throws Exception si hay error al enviar
     */
    MessageResponseDto enviarMensajeAudio(SendMessageRequestDto requestDto, UUID autorId) throws Exception;

    /**
     * Obtiene los mensajes de un canal específico.
     * @param canalId ID del canal
     * @param userId ID del usuario que solicita los mensajes
     * @return Lista de mensajes del canal
     * @throws Exception si el usuario no tiene permisos o el canal no existe
     */
    List<MessageResponseDto> obtenerMensajesDeCanal(UUID canalId, UUID userId) throws Exception;

    /**
     * Obtiene el historial de mensajes privados entre dos usuarios.
     * @param remitenteId ID del usuario que solicita el historial
     * @param destinatarioId ID del contacto con quien tiene la conversación
     * @return Lista de MessageResponseDto con el historial privado
     * @throws Exception en caso de error o falta de permisos
     */
    List<MessageResponseDto> obtenerHistorialPrivado(UUID remitenteId, UUID destinatarioId) throws Exception;

    /**
     * Guarda un archivo de audio en el sistema.
     * @param fileName Nombre del archivo
     * @param base64Data Datos del audio en base64
     * @param autorId ID del autor
     * @return Ruta relativa del archivo guardado
     * @throws IOException si hay error al guardar el archivo
     */
    String guardarArchivoDeAudio(String fileName, String base64Data, UUID autorId) throws IOException;

    /**
     * Obtiene todas las transcripciones de mensajes de audio.
     * @return Lista de transcripciones
     */
    List<TranscriptionResponseDto> obtenerTranscripciones();
}
