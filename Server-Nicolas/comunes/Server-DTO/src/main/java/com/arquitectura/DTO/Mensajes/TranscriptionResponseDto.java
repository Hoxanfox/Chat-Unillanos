package com.arquitectura.DTO.Mensajes;

import com.arquitectura.DTO.usuarios.UserResponseDto;
import java.time.LocalDateTime;
import java.util.UUID;

public class TranscriptionResponseDto {
    private UUID messageId;
    private String transcribedText;
    private LocalDateTime processedDate;
    private UserResponseDto author;
    private UUID channelId;

    // Constructor, Getters y Setters...
    public TranscriptionResponseDto(UUID messageId, String transcribedText, LocalDateTime processedDate, UserResponseDto author, UUID channelId) {
        this.messageId = messageId;
        this.transcribedText = transcribedText;
        this.processedDate = processedDate;
        this.author = author;
        this.channelId = channelId;
    }
    
    // Getters
    public UUID getMessageId() { return messageId; }
    public String getTranscribedText() { return transcribedText; }
    public LocalDateTime getProcessedDate() { return processedDate; }
    public UserResponseDto getAuthor() { return author; }
    public UUID getChannelId() { return channelId; }
}