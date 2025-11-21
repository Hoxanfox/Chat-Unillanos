package com.arquitectura.logicaMensajes;

import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.*;
import com.arquitectura.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Clase responsable de mapear entidades del dominio a DTOs de respuesta.
 * Separación de responsabilidades: esta clase se encarga únicamente de las transformaciones.
 */
@Component
public class MessageMapper {

    private final UserRepository userRepository;

    @Autowired
    public MessageMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Mapea un objeto TranscripcionAudio a TranscriptionResponseDto
     */
    public TranscriptionResponseDto mapToTranscriptionDto(TranscripcionAudio transcripcion) {
        UserResponseDto authorDto = new UserResponseDto(
                transcripcion.getMensaje().getAuthor().getUserId(),
                transcripcion.getMensaje().getAuthor().getUsername(),
                transcripcion.getMensaje().getAuthor().getEmail(),
                transcripcion.getMensaje().getAuthor().getPhotoAddress(),
                transcripcion.getMensaje().getAuthor().getFechaRegistro()
        );

        return new TranscriptionResponseDto(
                transcripcion.getId(),
                transcripcion.getTextoTranscrito(),
                transcripcion.getFechaProcesamiento(),
                authorDto,
                transcripcion.getMensaje().getChannel().getChannelId()
        );
    }

    /**
     * Mapea un objeto Message (y sus subtipos) a MessageResponseDto
     */
    public MessageResponseDto mapToMessageResponseDto(Message message) {
        // Load the author entity explicitly to avoid "no Session" lazy init errors
        User authorEntity = null;
        if (message.getAuthor() != null && message.getAuthor().getUserId() != null) {
            authorEntity = userRepository.findById(message.getAuthor().getUserId()).orElse(null);
        }

        UserResponseDto authorDto = new UserResponseDto(
                authorEntity != null ? authorEntity.getUserId() : null,
                authorEntity != null ? authorEntity.getUsername() : null,
                authorEntity != null ? authorEntity.getEmail() : null,
                authorEntity != null ? authorEntity.getPhotoAddress() : null,
                authorEntity != null ? authorEntity.getFechaRegistro() : null
        );

        String messageType = "";
        String content = "";

        if (message instanceof TextMessage) {
            messageType = "TEXT";
            content = ((TextMessage) message).getContent();
        } else if (message instanceof AudioMessage) {
            messageType = "AUDIO";
            content = ((AudioMessage) message).getAudioUrl();
        }

        return new MessageResponseDto(
                message.getIdMensaje(),
                message.getChannel().getChannelId(),
                authorDto,
                message.getTimestamp(),
                messageType,
                content
        );
    }
}

