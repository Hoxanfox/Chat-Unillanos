package com.arquitectura.fachada.mensaje;

import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;
import com.arquitectura.logicaMensajes.IMessageService;
import com.arquitectura.logicaMensajes.transcripcionAudio.IAudioTranscriptionService;
import com.arquitectura.utils.file.IFileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Implementación de la fachada de mensajes.
 * Coordina las operaciones relacionadas con mensajes del sistema.
 */
@Component
public class MensajeFachadaImpl implements IMensajeFachada {

    private final IMessageService messageService;
    private final IAudioTranscriptionService transcriptionService;
    private final IFileStorageService fileStorageService;

    @Autowired
    public MensajeFachadaImpl(IMessageService messageService,
                              IAudioTranscriptionService transcriptionService,
                              IFileStorageService fileStorageService) {
        this.messageService = messageService;
        this.transcriptionService = transcriptionService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void enviarMensajeBroadcast(String contenido, UUID adminId) throws Exception {
        messageService.enviarMensajeBroadcast(contenido, adminId);
    }

    @Override
    public MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, UUID autorId) throws Exception {
        return messageService.enviarMensajeTexto(requestDto, autorId);
    }

    @Override
    public MessageResponseDto enviarMensajeAudio(SendMessageRequestDto requestDto, UUID autorId) throws Exception {
        return messageService.enviarMensajeAudio(requestDto, autorId);
    }

    @Override
    public List<MessageResponseDto> obtenerMensajesDeCanal(UUID canalId, UUID userId) throws Exception {
        return messageService.obtenerMensajesPorCanal(canalId, userId);
    }

    @Override
    public List<MessageResponseDto> obtenerHistorialPrivado(UUID remitenteId, UUID destinatarioId) throws Exception {
        return messageService.obtenerHistorialPrivado(remitenteId, destinatarioId);
    }

    @Override
    public String guardarArchivoDeAudio(String fileName, String base64Data, UUID autorId) throws IOException {
        // 1. Decodificar los datos de Base64 a un array de bytes
        byte[] audioBytes = Base64.getDecoder().decode(base64Data);

        // 2. Crear un nombre de archivo único para el servidor
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        String newFileName = autorId + "_" + System.currentTimeMillis() + fileExtension;

        // 3. Usar el FileStorageService para guardar los bytes y devolver la ruta
        return fileStorageService.storeFile(audioBytes, newFileName, "audio_files");
    }

    @Override
    public List<TranscriptionResponseDto> obtenerTranscripciones() {
        return transcriptionService.getAllTranscriptions();
    }
}

