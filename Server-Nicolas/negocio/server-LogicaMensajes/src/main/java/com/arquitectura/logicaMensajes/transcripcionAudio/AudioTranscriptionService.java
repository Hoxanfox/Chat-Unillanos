package com.arquitectura.logicaMensajes.transcripcionAudio;

import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.AudioMessage;
import com.arquitectura.domain.TranscripcionAudio;
import com.arquitectura.domain.User;
import com.arquitectura.persistence.repository.TranscripcionAudioRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AudioTranscriptionService implements IAudioTranscriptionService {

    private static final String VOSK_MODEL_PATH = "vosk-model-small-es-0.42";
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AudioTranscriptionService.class);

    private final TranscripcionAudioRepository transcripcionRepository;
    private Model voskModel;
    private boolean modelAvailable = false;

    @Autowired
    public AudioTranscriptionService(TranscripcionAudioRepository transcripcionRepository) {
        this.transcripcionRepository = transcripcionRepository;
        try {
            File modelDir = new File(VOSK_MODEL_PATH);
            if (!modelDir.exists() || !modelDir.isDirectory()) {
                log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.warn("⚠️  Modelo Vosk no encontrado en: {}", modelDir.getAbsolutePath());
                log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.warn("La transcripción de audio NO estará disponible.");
                log.warn("Para habilitar transcripciones:");
                log.warn("  1. Descarga el modelo desde: https://alphacephei.com/vosk/models");
                log.warn("  2. Descomprime 'vosk-model-small-es-0.42' en la carpeta del servidor");
                log.warn("  3. Reinicia el servidor");
                log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                this.voskModel = null;
                this.modelAvailable = false;
                return;
            }

            this.voskModel = new Model(VOSK_MODEL_PATH);
            this.modelAvailable = true;
            log.info("✓ Modelo Vosk cargado correctamente desde: {}", VOSK_MODEL_PATH);
        } catch (IOException e) {
            log.error("Error al cargar el modelo de Vosk: {}", e.getMessage());
            log.warn("La transcripción de audio NO estará disponible.");
            this.voskModel = null;
            this.modelAvailable = false;
        }
    }

    public void transcribeAndSave(AudioMessage audioMessage, String audioFilePath) {
        if (!modelAvailable || voskModel == null) {
            log.debug("Modelo de Vosk no disponible. Saltando transcripción del mensaje ID: {}",
                     audioMessage.getIdMensaje());
            return;
        }

        File audioFile = new File(audioFilePath);
        if (!audioFile.exists()) {
            log.error("Archivo de audio no existe: {}", audioFilePath);
            return;
        }

        try (InputStream ais = new FileInputStream(audioFile)) {
            // Vosk funciona mejor con una tasa de muestreo de 16000Hz. El audio del cliente debería estar en ese formato.
            Recognizer recognizer = new Recognizer(voskModel, 16000.0f);
            StringBuilder textoFinal = new StringBuilder();
            byte[] b = new byte[4096];
            int nbytes;

            while ((nbytes = ais.read(b)) >= 0) {
                if (recognizer.acceptWaveForm(b, nbytes)) {
                    JSONObject resultJson = new JSONObject(recognizer.getResult());
                    textoFinal.append(resultJson.getString("text")).append(" ");
                }
            }
            JSONObject finalResultJson = new JSONObject(recognizer.getFinalResult());
            textoFinal.append(finalResultJson.getString("text"));

            String textoTranscribido = textoFinal.toString().trim();

            if (!textoTranscribido.isEmpty()) {
                TranscripcionAudio transcripcion = new TranscripcionAudio(audioMessage, textoTranscribido);
                transcripcionRepository.save(transcripcion);
                log.info("Transcripción guardada para mensaje ID {}: {}",
                        audioMessage.getIdMensaje(), textoTranscribido);
            } else {
                log.debug("Vosk no devolvió texto para el mensaje ID {}", audioMessage.getIdMensaje());
            }

        } catch (Exception e) {
            log.error("Error durante la transcripción con Vosk: {}", e.getMessage(), e);
        }
    }
    @Override
    @Transactional(readOnly = true)
    public List<TranscriptionResponseDto> getAllTranscriptions() {
        return transcripcionRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    private TranscriptionResponseDto mapToDto(TranscripcionAudio entity) {
        User authorEntity = entity.getMensaje().getAuthor();
        UserResponseDto authorDto = new UserResponseDto(
                authorEntity.getUserId(),
                authorEntity.getUsername(),
                authorEntity.getEmail(),
                authorEntity.getPhotoAddress(),
                authorEntity.getFechaRegistro()
        );

        return new TranscriptionResponseDto(
                entity.getId(),
                entity.getTextoTranscrito(),
                entity.getFechaProcesamiento(),
                authorDto,
                entity.getMensaje().getChannel().getChannelId()
        );
    }
}