package com.arquitectura.logicaMensajes.strategies;

import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.domain.AudioMessage;
import com.arquitectura.domain.Channel;
import com.arquitectura.domain.Message;
import com.arquitectura.domain.User;
import com.arquitectura.logicaMensajes.transcripcionAudio.AudioTranscriptionService;
import com.arquitectura.persistence.repository.MessageRepository;
import com.arquitectura.utils.file.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@Component
public class AudioMessageStrategy implements IMessageStrategy {

    private final MessageRepository messageRepository;
    private final AudioTranscriptionService transcriptionService;

    @Autowired
    public AudioMessageStrategy(MessageRepository messageRepository,
                                AudioTranscriptionService transcriptionService) {
        this.messageRepository = messageRepository;
        this.transcriptionService = transcriptionService;
    }

    @Override
    public Message procesarYGuardar(SendMessageRequestDto requestDto, User autor, Channel canal) throws Exception {
        String audioFilePath = requestDto.getContent();

        // Validamos que no llegue vacío por seguridad
        if (audioFilePath == null || audioFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta del archivo de audio es requerida.");
        }

        // Crear entidad AudioMessage directamente con la ruta
        AudioMessage nuevoMensaje = new AudioMessage(autor, canal, audioFilePath);

        // Guardar en BD
        AudioMessage mensajeGuardado = messageRepository.save(nuevoMensaje);

        // Transcribir en segundo plano
        // (Esto se mantiene igual, es una buena práctica hacerlo async)
        Executors.newSingleThreadExecutor().submit(() -> {
            transcriptionService.transcribeAndSave(mensajeGuardado, audioFilePath);
        });

        return mensajeGuardado;
    }

    @Override
    public boolean soportaTipo(String messageType) {
        return "AUDIO".equalsIgnoreCase(messageType);
    }
}