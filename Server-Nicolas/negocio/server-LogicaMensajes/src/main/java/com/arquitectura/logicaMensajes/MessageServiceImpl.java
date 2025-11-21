// language: java
// File: src/main/java/com/arquitectura/logicaMensajes/MessageServiceImpl.java
package com.arquitectura.logicaMensajes;

import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;
import com.arquitectura.domain.*;
import com.arquitectura.domain.enums.EstadoMembresia;
import com.arquitectura.events.BroadcastMessageEvent;
import com.arquitectura.events.NewMessageEvent;
import com.arquitectura.logicaCanales.IChannelService;
import com.arquitectura.logicaMensajes.strategies.IMessageStrategy;
import com.arquitectura.persistence.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements IMessageService {

    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final MembresiaCanalRepository membresiaCanalRepository;
    private final TranscripcionAudioRepository transcripcionAudioRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final IChannelService channelService;
    private final List<IMessageStrategy> messageStrategies;
    private final MessageMapper messageMapper;

    @Autowired
    public MessageServiceImpl(
            UserRepository userRepository,
            ChannelRepository channelRepository,
            MessageRepository messageRepository,
            MembresiaCanalRepository membresiaCanalRepository,
            TranscripcionAudioRepository transcripcionAudioRepository,
            ApplicationEventPublisher eventPublisher,
            IChannelService channelService,
            List<IMessageStrategy> messageStrategies,
            MessageMapper messageMapper) {
        this.messageRepository = messageRepository;
        this.transcripcionAudioRepository = transcripcionAudioRepository;
        this.messageStrategies = messageStrategies;
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.membresiaCanalRepository = membresiaCanalRepository;
        this.eventPublisher = eventPublisher;
        this.channelService = channelService;
        this.messageMapper = messageMapper;
    }

    @Override
    @Transactional
    public MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, UUID autorId) throws Exception {
        requestDto.setMessageType("TEXT"); // Aseguramos el tipo
        return procesarEnvioMensaje(requestDto, autorId);
    }
    @Override
    @Transactional
    public MessageResponseDto enviarMensajeAudio(SendMessageRequestDto requestDto, UUID autorId) throws Exception {
        requestDto.setMessageType("AUDIO"); // Aseguramos el tipo
        return procesarEnvioMensaje(requestDto, autorId);
    }

    private MessageResponseDto procesarEnvioMensaje(SendMessageRequestDto requestDto, UUID autorId) throws Exception {
        // 1. Validaciones Comunes (Usuario y Canal)
        // Usamos findByIdWithPeer para optimizar y evitar problemas de lazy loading
        User autor = userRepository.findByIdWithPeer(autorId)
                .orElseThrow(() -> new Exception("El autor con ID " + autorId + " no existe."));

        Channel canal = channelRepository.findById(requestDto.getChannelId())
                .orElseThrow(() -> new Exception("El canal con ID " + requestDto.getChannelId() + " no existe."));

        // 2. Selección de Estrategia (Polimorfismo)
        IMessageStrategy strategy = messageStrategies.stream()
                .filter(s -> s.soportaTipo(requestDto.getMessageType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de mensaje no soportado: " + requestDto.getMessageType()));

        // 3. Ejecución de la Estrategia
        Message mensajeGuardado = strategy.procesarYGuardar(requestDto, autor, canal);

        // 4. Notificación y Retorno (Lógica común)
        return notificarYRetornar(mensajeGuardado);
    }
    private MessageResponseDto notificarYRetornar(Message mensajeGuardado) {
        MessageResponseDto responseDto = messageMapper.mapToMessageResponseDto(mensajeGuardado);

        // Obtener destinatarios para el evento Push
        List<UUID> memberIds = membresiaCanalRepository
                .findAllByCanal_ChannelIdAndEstado(responseDto.getChannelId(), EstadoMembresia.ACTIVO)
                .stream()
                .map(membresia -> membresia.getUsuario().getUserId())
                .collect(Collectors.toList());

        eventPublisher.publishEvent(new NewMessageEvent(this, responseDto, memberIds));
        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDto> obtenerMensajesPorCanal(UUID canalId, UUID userId) throws Exception {
        // 1. Lógica de seguridad (verificar que el usuario es miembro del canal).
        MembresiaCanalId membresiaId = new MembresiaCanalId(canalId, userId);
        if (!membresiaCanalRepository.existsById(membresiaId)) {
            throw new Exception("Acceso denegado. No eres miembro de este canal.");
        }
        // Usamos el método optimizado del repositorio
        return messageRepository.findByChannelIdWithAuthors(canalId).stream()
                .map(messageMapper::mapToMessageResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDto> obtenerHistorialPrivado(UUID remitenteId, UUID destinatarioId) throws Exception {
        // 1. Obtener o crear el canal directo (esto mantiene la lógica en ChannelService)
        Channel canalDirecto = channelService.obtenerOCrearCanalDirecto(remitenteId, destinatarioId);
        // 2. Verificar que el remitente es miembro del canal
        MembresiaCanalId membresiaId = new MembresiaCanalId(canalDirecto.getChannelId(), remitenteId);
        if (!membresiaCanalRepository.existsById(membresiaId)) {
            throw new Exception("Acceso denegado. No tienes permiso para ver este historial.");
        }
        // . Convertir a DTO y retornar
        return messageRepository.findByChannelIdWithAuthors(canalDirecto.getChannelId()).stream()
                .map(messageMapper::mapToMessageResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void enviarMensajeBroadcast(String contenido, UUID adminId) throws Exception {
        // Asumimos que el canal de broadcast tiene un ID conocido
        final UUID BROADCAST_CHANNEL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

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
        List<TranscripcionAudio> transcripciones = transcripcionAudioRepository.findAll();

        return transcripciones.stream()
                .map(messageMapper::mapToTranscriptionDto)
                .collect(Collectors.toList());
    }


}
