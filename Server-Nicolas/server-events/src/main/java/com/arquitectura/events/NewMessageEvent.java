package com.arquitectura.events;

import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class NewMessageEvent extends ApplicationEvent {
    private final MessageResponseDto messageDto;
    private final List<Integer> recipientUserIds;

    public NewMessageEvent(Object source, MessageResponseDto messageDto, List<Integer> recipientUserIds) {
        super(source);
        this.messageDto = messageDto;
        this.recipientUserIds = recipientUserIds;
    }

    public MessageResponseDto getMessageDto() {
        return messageDto;
    }
    public List<Integer> getRecipientUserIds() { // <-- AÑADIR GETTER
        return recipientUserIds;
    }
}
