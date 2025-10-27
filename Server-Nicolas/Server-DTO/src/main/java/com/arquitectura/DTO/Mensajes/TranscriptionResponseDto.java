package com.arquitectura.DTO.Mensajes;

import com.arquitectura.DTO.usuarios.UserResponseDto;
import java.time.LocalDateTime;

public class TranscriptionResponseDto {
    private Long messageId;
    private String transcribedText;
    private LocalDateTime processedDate;
    private UserResponseDto author;
    private int channelId;

    // Constructor, Getters y Setters...
    public TranscriptionResponseDto(Long messageId, String transcribedText, LocalDateTime processedDate, UserResponseDto author, int channelId) {
        this.messageId = messageId;
        this.transcribedText = transcribedText;
        this.processedDate = processedDate;
        this.author = author;
        this.channelId = channelId;
    }
    
    // Getters
    public Long getMessageId() { return messageId; }
    public String getTranscribedText() { return transcribedText; }
    public LocalDateTime getProcessedDate() { return processedDate; }
    public UserResponseDto getAuthor() { return author; }
    public int getChannelId() { return channelId; }
}