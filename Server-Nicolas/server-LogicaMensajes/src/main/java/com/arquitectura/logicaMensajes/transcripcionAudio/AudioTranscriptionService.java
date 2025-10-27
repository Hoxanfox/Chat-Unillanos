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

    private final TranscripcionAudioRepository transcripcionRepository;
    private Model voskModel;

    @Autowired
    public AudioTranscriptionService(TranscripcionAudioRepository transcripcionRepository) {
        this.transcripcionRepository = transcripcionRepository;
        try {
            this.voskModel = new Model(VOSK_MODEL_PATH);
        } catch (IOException e) {
            System.err.println("ERROR CRÍTICO: No se pudo cargar el modelo de Vosk. Asegúrate de que la carpeta '" + VOSK_MODEL_PATH + "' existe junto al JAR.");
            // e.printStackTrace(); // Descomentar para depuración
        }
    }

    public void transcribeAndSave(AudioMessage audioMessage, String audioFilePath) {
        if (voskModel == null) {
            System.err.println("Modelo de Vosk no cargado. Abortando transcripción.");
            return;
        }

        File audioFile = new File(audioFilePath);
        if (!audioFile.exists()) {
            System.err.println("Archivo de audio a transcribir no existe: " + audioFilePath);
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
                System.out.println("Transcripción guardada para mensaje ID " + audioMessage.getIdMensaje() + ": " + textoTranscribido);
            } else {
                System.out.println("Vosk no devolvió texto para el mensaje ID " + audioMessage.getIdMensaje());
            }

        } catch (Exception e) {
            System.err.println("Error durante la transcripción con Vosk: " + e.getMessage());
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
                authorEntity.getPhotoAddress()
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