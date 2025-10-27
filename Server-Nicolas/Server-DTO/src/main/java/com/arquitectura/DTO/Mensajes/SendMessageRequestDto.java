package com.arquitectura.DTO.Mensajes;

public class SendMessageRequestDto {
    private int channelId;
    private String messageType; // "TEXT" o "AUDIO"
    private String content; // Contenido del texto o URL del audio

    // Constructor vacío
    public SendMessageRequestDto() {}

    // Constructor con campos
    public SendMessageRequestDto(int channelId, String messageType, String content) {
        this.channelId = channelId;
        this.messageType = messageType;
        this.content = content;
    }

    // Getters y Setters
    public int getChannelId() { return channelId; }
    public void setChannelId(int channelId) { this.channelId = channelId; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}