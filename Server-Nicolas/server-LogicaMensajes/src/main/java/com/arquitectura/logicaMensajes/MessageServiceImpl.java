package com.arquitectura.logicaMensajes;

import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.*;
import com.arquitectura.domain.enums.EstadoMembresia;
import com.arquitectura.events.BroadcastMessageEvent;
import com.arquitectura.events.NewMessageEvent;
import com.arquitectura.logicaMensajes.transcripcionAudio.AudioTranscriptionService;
import com.arquitectura.persistence.repository.*;
import com.arquitectura.utils.file.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements IMessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final MembresiaCanalRepository membresiaCanalRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FileStorageService fileStorageService;
    private final AudioTranscriptionService transcriptionService;
    private final TranscripcionAudioRepository transcripcionAudioRepository;

    @Autowired
    public MessageServiceImpl(MessageRepository messageRepository, UserRepository userRepository, ChannelRepository channelRepository, MembresiaCanalRepository membresiaCanalRepository, ApplicationEventPublisher eventPublisher, FileStorageService fileStorageService, AudioTranscriptionService transcriptionService, TranscripcionAudioRepository transcripcionAudioRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.membresiaCanalRepository = membresiaCanalRepository;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
        this.transcriptionService = transcriptionService;
        this.transcripcionAudioRepository = transcripcionAudioRepository;
    }

    @Override
    @Transactional
    public MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, int autorId) throws Exception {
        User autor = userRepository.findById(autorId)
                .orElseThrow(() -> new Exception("El autor con ID " + autorId + " no existe."));
        
        Channel canal = channelRepository.findById(requestDto.getChannelId())
                .orElseThrow(() -> new Exception("El canal con ID " + requestDto.getChannelId() + " no existe."));

        TextMessage nuevoMensaje = new TextMessage(autor, canal, requestDto.getContent());
        Message mensajeGuardado = messageRepository.save(nuevoMensaje);
        MessageResponseDto responseDto = getMessageResponseDto(mensajeGuardado);

        return responseDto;
    }

    @Override
    @Transactional
    public MessageResponseDto enviarMensajeAudio(SendMessageRequestDto requestDto, int autorId) throws Exception {
        User autor = userRepository.findById(autorId)
                .orElseThrow(() -> new Exception("El autor con ID " + autorId + " no existe."));

        Channel canal = channelRepository.findById(requestDto.getChannelId())
                .orElseThrow(() -> new Exception("El canal con ID " + requestDto.getChannelId() + " no existe."));

        // 1. Recibimos el payload: "nombreArchivo;datosEnBase64"
        String payload = requestDto.getContent();
        String[] parts = payload.split(";", 2);
        if (parts.length != 2) {
            throw new Exception("Formato de payload de audio incorrecto.");
        }

        String fileName = parts[0];
        String base64Data = parts[1];

        // 2. Decodificamos los datos de Base64 a un array de bytes
        byte[] audioBytes = Base64.getDecoder().decode(base64Data);

        // 3. Creamos un nombre de archivo único para guardarlo en el servidor
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        String newFileName = autorId + "_" + System.currentTimeMillis() + fileExtension;

        // 4. Usamos el nuevo método del FileStorageService para guardar los bytes
        String storedAudioPath = fileStorageService.storeFile(audioBytes, newFileName, "audio_files");

        // 5. El resto de la lógica para guardar en la BD y transcribir no cambia
        AudioMessage nuevoMensaje = new AudioMessage(autor, canal, storedAudioPath);
        AudioMessage mensajeGuardado = (AudioMessage) messageRepository.save(nuevoMensaje);

        Executors.newSingleThreadExecutor().submit(() -> {
            transcriptionService.transcribeAndSave(mensajeGuardado, storedAudioPath);
        });

        return getMessageResponseDto(mensajeGuardado);
    }

    private MessageResponseDto getMessageResponseDto(Message mensajeGuardado) {
        MessageResponseDto responseDto = mapToMessageResponseDto(mensajeGuardado);
        List<Integer> memberIds = membresiaCanalRepository.findAllByCanal_ChannelIdAndEstado(responseDto.getChannelId(), EstadoMembresia.ACTIVO)
                .stream()
                .map(membresia -> membresia.getUsuario().getUserId())
                .collect(Collectors.toList());
        eventPublisher.publishEvent(new NewMessageEvent(this, responseDto, memberIds));
        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDto> obtenerMensajesPorCanal(int canalId, int userId) throws Exception {
        // 1. Lógica de seguridad (verificar que el usuario es miembro del canal).
        //    Esto no cambia y está perfecto como lo tienes.
        MembresiaCanalId membresiaId = new MembresiaCanalId(canalId, userId);
        if (!membresiaCanalRepository.existsById(membresiaId)) {
            throw new Exception("Acceso denegado. No eres miembro de este canal.");
        }

        // 2. Llamamos al nuevo método del repositorio que trae los mensajes Y sus autores.
        List<Message> messages = messageRepository.findByChannelIdWithAuthors(canalId);

        // 3. La conversión a DTO ahora es 100% segura y no causará un error de "no Session".
        return messages.stream()
                .map(this::mapToMessageResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void enviarMensajeBroadcast(String contenido, int adminId) throws Exception {
        // Asumimos que el canal de broadcast tiene un ID conocido, por ejemplo, 1
        final int BROADCAST_CHANNEL_ID = 1;

        Channel canal = channelRepository.findById(BROADCAST_CHANNEL_ID)
                .orElseThrow(() -> new Exception("El canal de broadcast no existe."));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new Exception("El usuario administrador no existe."));

        TextMessage broadcastMessage = new TextMessage(admin, canal, contenido);

        // 1. Guardamos el mensaje en la BD
        messageRepository.save(broadcastMessage);

        // 2. Publicamos un evento. La capa de lógica no sabe quién lo escuchará.
        String formattedMessage = "BROADCAST;" + admin.getUsername() + ";" + contenido;
        eventPublisher.publishEvent(new BroadcastMessageEvent(this, formattedMessage));
    }
    @Override
    @Transactional(readOnly = true)
    public List<TranscriptionResponseDto> getAllTranscriptions() {
        // --- ¡AQUÍ ESTÁ LA SIMPLIFICACIÓN! ---
        // Volvemos a usar el método simple. Ahora funcionará porque
        // las entidades se encargarán de cargar toda la información.
        List<TranscripcionAudio> transcripciones = transcripcionAudioRepository.findAll();

        return transcripciones.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private TranscriptionResponseDto mapToDto(TranscripcionAudio transcripcion) {
        // Este método ahora funcionará sin error.
        UserResponseDto authorDto = new UserResponseDto(
                transcripcion.getMensaje().getAuthor().getUserId(),
                transcripcion.getMensaje().getAuthor().getUsername(),
                transcripcion.getMensaje().getAuthor().getEmail(),
                transcripcion.getMensaje().getAuthor().getPhotoAddress()
        );

        return new TranscriptionResponseDto(
                transcripcion.getId(),
                transcripcion.getTextoTranscrito(),
                transcripcion.getFechaProcesamiento(),
                authorDto,
                transcripcion.getMensaje().getChannel().getChannelId()
        );
    }




    private MessageResponseDto mapToMessageResponseDto(Message message) {
        // Mapea el autor a su DTO correspondiente para no exponer la entidad User
        UserResponseDto authorDto = new UserResponseDto(
                message.getAuthor().getUserId(),
                message.getAuthor().getUsername(),
                message.getAuthor().getEmail(),
                message.getAuthor().getPhotoAddress()
        );

        String messageType = "";
        String content = "";

        // Determina el tipo y contenido basado en la clase hija
        if (message instanceof TextMessage) {
            messageType = "TEXT";
            content = ((TextMessage) message).getContent();
        } else if (message instanceof AudioMessage) {
            messageType = "AUDIO";
            content = ((AudioMessage) message).getAudioUrl();
        }

        return new MessageResponseDto(
                message.getIdMensaje(),
                message.getChannel().getChannelId(),
                authorDto,
                message.getTimestamp(),
                messageType,
                content
        );
    }
}