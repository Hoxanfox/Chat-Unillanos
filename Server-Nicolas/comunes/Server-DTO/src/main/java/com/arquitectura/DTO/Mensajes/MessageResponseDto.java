package com.arquitectura.DTO.Mensajes;

import com.arquitectura.DTO.usuarios.UserResponseDto;

import java.time.LocalDateTime;
import java.util.UUID;

public class MessageResponseDto {
    private UUID messageId;
    private UUID channelId;
    private UserResponseDto author; // Objeto DTO con la info pública del autor
    private LocalDateTime timestamp;
    private String messageType;
    private String content;
    private String channelType; // DIRECTO, GRUPO, BROADCAST

    // Constructor vacío
    public MessageResponseDto() {}

    // Constructor con campos
    public MessageResponseDto(UUID messageId, UUID channelId, UserResponseDto author, LocalDateTime timestamp, String messageType, String content) {
        this.messageId = messageId;
        this.channelId = channelId;
        this.author = author;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.content = content;
    }

    // Constructor completo con channelType
    public MessageResponseDto(UUID messageId, UUID channelId, UserResponseDto author, LocalDateTime timestamp, String messageType, String content, String channelType) {
        this.messageId = messageId;
        this.channelId = channelId;
        this.author = author;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.content = content;
        this.channelType = channelType;
    }

    // Getters y Setters
    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    public UUID getChannelId() { return channelId; }
    public void setChannelId(UUID channelId) { this.channelId = channelId; }
    public UserResponseDto getAuthor() { return author; }
    public void setAuthor(UserResponseDto author) { this.author = author; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getChannelType() { return channelType; }
    public void setChannelType(String channelType) { this.channelType = channelType; }
}