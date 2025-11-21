package com.arquitectura.logicaMensajes.strategies;

import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.domain.Channel;
import com.arquitectura.domain.Message;
import com.arquitectura.domain.TextMessage;
import com.arquitectura.domain.User;
import com.arquitectura.persistence.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TextMessageStrategy implements IMessageStrategy {

    private final MessageRepository messageRepository;

    @Autowired
    public TextMessageStrategy(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public Message procesarYGuardar(SendMessageRequestDto requestDto, User autor, Channel canal) {
        // Lógica específica de texto: crear entidad TextMessage
        TextMessage nuevoMensaje = new TextMessage(autor, canal, requestDto.getContent());
        
        // Guardar y retornar
        return messageRepository.save(nuevoMensaje);
    }

    @Override
    public boolean soportaTipo(String messageType) {
        return "TEXT".equalsIgnoreCase(messageType);
    }
}