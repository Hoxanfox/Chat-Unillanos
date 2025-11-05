package com.arquitectura.logicaMensajes.transcripcionAudio;

import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;

import java.util.List;

public interface IAudioTranscriptionService{
    List<TranscriptionResponseDto> getAllTranscriptions();
}
