package com.arquitectura.DTO.Mensajes;

import java.util.UUID;

public class SendMessageRequestDto {
    private UUID channelId;
    private String messageType; // "TEXT" o "AUDIO"
    private String content; // Contenido del texto o URL del audio

    // Constructor vacío
    public SendMessageRequestDto() {}

    // Constructor con campos
    public SendMessageRequestDto(UUID channelId, String messageType, String content) {
        this.channelId = channelId;
        this.messageType = messageType;
        this.content = content;
    }

    // Getters y Setters
    public UUID getChannelId() { return channelId; }
    public void setChannelId(UUID channelId) { this.channelId = channelId; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}