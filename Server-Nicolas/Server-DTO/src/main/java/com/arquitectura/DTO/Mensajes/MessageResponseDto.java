package com.arquitectura.DTO.Mensajes;

import com.arquitectura.DTO.usuarios.UserResponseDto;

import java.time.LocalDateTime;

public class MessageResponseDto {
    private Long messageId;
    private int channelId;
    private UserResponseDto author; // Objeto DTO con la info pública del autor
    private LocalDateTime timestamp;
    private String messageType;
    private String content;

    // Constructor vacío
    public MessageResponseDto() {}

    // Constructor con campos
    public MessageResponseDto(Long messageId, int channelId, UserResponseDto author, LocalDateTime timestamp, String messageType, String content) {
        this.messageId = messageId;
        this.channelId = channelId;
        this.author = author;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.content = content;
    }

    // Getters y Setters
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public int getChannelId() { return channelId; }
    public void setChannelId(int channelId) { this.channelId = channelId; }
    public UserResponseDto getAuthor() { return author; }
    public void setAuthor(UserResponseDto author) { this.author = author; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}