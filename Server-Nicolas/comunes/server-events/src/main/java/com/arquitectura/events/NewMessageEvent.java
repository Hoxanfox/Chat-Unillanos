package com.arquitectura.events;

import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.UUID;

public class NewMessageEvent extends ApplicationEvent {
    private final MessageResponseDto messageDto;
    private final List<UUID> recipientUserIds;

    public NewMessageEvent(Object source, MessageResponseDto messageDto, List<UUID> recipientUserIds) {
        super(source);
        this.messageDto = messageDto;
        this.recipientUserIds = recipientUserIds;
    }

    public MessageResponseDto getMessageDto() {
        return messageDto;
    }
    public List<UUID> getRecipientUserIds() {
        return recipientUserIds;
    }
}
