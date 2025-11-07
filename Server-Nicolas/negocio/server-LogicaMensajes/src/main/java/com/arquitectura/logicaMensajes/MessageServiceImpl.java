// language: java
// File: src/main/java/com/arquitectura/logicaMensajes/MessageServiceImpl.java
package com.arquitectura.logicaMensajes;

import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;
import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.*;
import com.arquitectura.domain.enums.EstadoMembresia;
import com.arquitectura.events.BroadcastMessageEvent;
import com.arquitectura.events.NewMessageEvent;
import com.arquitectura.logicaCanales.IChannelService;
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
import java.util.UUID;
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
    private final IChannelService channelService;
    private final com.arquitectura.logicaPeers.IPeerNotificationService peerNotificationService;

    @Autowired
    public MessageServiceImpl(MessageRepository messageRepository,
                              UserRepository userRepository,
                              ChannelRepository channelRepository,
                              MembresiaCanalRepository membresiaCanalRepository,
                              ApplicationEventPublisher eventPublisher,
                              FileStorageService fileStorageService,
                              AudioTranscriptionService transcriptionService,
                              TranscripcionAudioRepository transcripcionAudioRepository,
                              IChannelService channelService,
                              com.arquitectura.logicaPeers.IPeerNotificationService peerNotificationService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.membresiaCanalRepository = membresiaCanalRepository;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
        this.transcriptionService = transcriptionService;
        this.transcripcionAudioRepository = transcripcionAudioRepository;
        this.channelService = channelService;
        this.peerNotificationService = peerNotificationService;
    }

    @Override
    @Transactional
    public MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, UUID autorId) throws Exception {

        // NO USES ESTE:
        // User autor = userRepository.findById(autorId)
        //        .orElseThrow(() -> new Exception("El autor..."));

        // USA ESTE NUEVO MÉTODO:
        User autor = userRepository.findByIdWithPeer(autorId)
                .orElseThrow(() -> new Exception("El autor con ID " + autorId + " no existe."));

        // ... (el resto de tu método sigue igual)
        Channel canal = channelRepository.findById(requestDto.getChannelId())
                .orElseThrow(() -> new Exception("El canal..."));

        TextMessage nuevoMensaje = new TextMessage(autor, canal, requestDto.getContent());
        Message mensajeGuardado = messageRepository.save(nuevoMensaje);

        // Esta línea ahora es segura, porque autor.getPeerId() ya está cargado
        MessageResponseDto responseDto = getMessageResponseDto(mensajeGuardado);

        return responseDto;
    }

    @Override
    @Transactional
    public MessageResponseDto enviarMensajeAudio(SendMessageRequestDto requestDto, UUID autorId) throws Exception {
        User autor = userRepository.findById(autorId)
                .orElseThrow(() -> new Exception("El autor con ID " + autorId + " no existe."));

        Channel canal = channelRepository.findById(requestDto.getChannelId())
                .orElseThrow(() -> new Exception("El canal con ID " + requestDto.getChannelId() + " no existe."));

        String payload = requestDto.getContent();
        String storedAudioPath;

        // Detectar si el payload es una ruta de archivo ya subido o datos en base64
        if (payload.contains(";")) {
            // Formato antiguo: "nombreArchivo;datosEnBase64"
            String[] parts = payload.split(";", 2);
            if (parts.length != 2) {
                throw new Exception("Formato de payload de audio incorrecto.");
            }

            String fileName = parts[0];
            String base64Data = parts[1];

            // Decodificar los datos de Base64 a un array de bytes
            byte[] audioBytes = Base64.getDecoder().decode(base64Data);

            // Crear un nombre de archivo único para guardarlo en el servidor
            String fileExtension = fileName.substring(fileName.lastIndexOf("."));
            String newFileName = autorId + "_" + System.currentTimeMillis() + fileExtension;

            // Guardar el archivo
            storedAudioPath = fileStorageService.storeFile(audioBytes, newFileName, "audio_files");
        } else {
            // Formato nuevo: ruta de archivo ya subido (ej: "audio_files/uuid_timestamp.wav")
            storedAudioPath = payload;
        }

        // Guardar el mensaje de audio en la base de datos
        AudioMessage nuevoMensaje = new AudioMessage(autor, canal, storedAudioPath);
        AudioMessage mensajeGuardado = (AudioMessage) messageRepository.save(nuevoMensaje);

        // Transcribir el audio en segundo plano
        Executors.newSingleThreadExecutor().submit(() -> {
            transcriptionService.transcribeAndSave(mensajeGuardado, storedAudioPath);
        });

        return getMessageResponseDto(mensajeGuardado);
    }

    private MessageResponseDto getMessageResponseDto(Message mensajeGuardado) {
        MessageResponseDto responseDto = mapToMessageResponseDto(mensajeGuardado);
        List<UUID> memberIds = membresiaCanalRepository.findAllByCanal_ChannelIdAndEstado(responseDto.getChannelId(), EstadoMembresia.ACTIVO)
                .stream()
                .map(membresia -> membresia.getUsuario().getUserId())
                .collect(Collectors.toList());

        // Publicar evento local para usuarios conectados en este servidor
        eventPublisher.publishEvent(new NewMessageEvent(this, responseDto, memberIds));

        // --- NOTIFICACIÓN P2P: Notificar a peers que tengan usuarios miembros del canal ---
        notificarMensajeAPeers(responseDto, memberIds);

        return responseDto;
    }

    /**
     * Notifica a otros servidores sobre un nuevo mensaje si tienen usuarios miembros del canal.
     */
    private void notificarMensajeAPeers(MessageResponseDto mensaje, List<UUID> memberIds) {
        try {
            // Agrupar miembros por servidor (peer)
            java.util.Map<UUID, List<UUID>> miembrosPorPeer = new java.util.HashMap<>();

            for (UUID memberId : memberIds) {
                UUID peerDelMiembro = peerNotificationService.obtenerPeerDeUsuario(memberId);
                if (peerDelMiembro != null) {
                    miembrosPorPeer.computeIfAbsent(peerDelMiembro, k -> new java.util.ArrayList<>())
                                   .add(memberId);
                }
            }

            // Obtener el peer local (este servidor)
            com.arquitectura.logicaPeers.IPeerService peerService =
                (com.arquitectura.logicaPeers.IPeerService) org.springframework.context.ApplicationContextProvider
                    .getApplicationContext().getBean("peerServiceP2P");
            UUID localPeerId = peerService.obtenerPeerActualId();

            // Notificar a cada peer remoto (excepto el local)
            for (java.util.Map.Entry<UUID, List<UUID>> entry : miembrosPorPeer.entrySet()) {
                UUID peerId = entry.getKey();

                // No notificar al servidor local
                if (peerId.equals(localPeerId)) {
                    continue;
                }

                System.out.println("→ [MessageService] Notificando mensaje a peer " + peerId +
                                 " con " + entry.getValue().size() + " miembros");

                // Notificar al peer de forma asíncrona para no bloquear
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    boolean exito = peerNotificationService.notificarNuevoMensaje(peerId, mensaje);
                    if (exito) {
                        System.out.println("✓ [MessageService] Mensaje notificado exitosamente a peer " + peerId);
                    } else {
                        System.err.println("✗ [MessageService] Fallo al notificar mensaje a peer " + peerId);
                    }
                });
            }

        } catch (Exception e) {
            System.err.println("✗ [MessageService] Error al notificar mensaje a peers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDto> obtenerMensajesPorCanal(UUID canalId, UUID userId) throws Exception {
        // 1. Lógica de seguridad (verificar que el usuario es miembro del canal).
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
    @Transactional(readOnly = true)
    public List<MessageResponseDto> obtenerHistorialPrivado(UUID remitenteId, UUID destinatarioId) throws Exception {
        // 1. Obtener o crear el canal directo (esto mantiene la lógica en ChannelService)
        Channel canalDirecto = channelService.obtenerOCrearCanalDirecto(remitenteId, destinatarioId);

        // 2. Verificar que el remitente es miembro del canal
        MembresiaCanalId membresiaId = new MembresiaCanalId(canalDirecto.getChannelId(), remitenteId);
        if (!membresiaCanalRepository.existsById(membresiaId)) {
            throw new Exception("Acceso denegado. No tienes permiso para ver este historial.");
        }

        // 3. Obtener los mensajes del canal directo
        List<Message> messages = messageRepository.findByChannelIdWithAuthors(canalDirecto.getChannelId());

        // 4. Convertir a DTO y retornar
        return messages.stream()
                .map(this::mapToMessageResponseDto)
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
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private TranscriptionResponseDto mapToDto(TranscripcionAudio transcripcion) {
        UserResponseDto authorDto = new UserResponseDto(
                transcripcion.getMensaje().getAuthor().getUserId(),
                transcripcion.getMensaje().getAuthor().getUsername(),
                transcripcion.getMensaje().getAuthor().getEmail(),
                transcripcion.getMensaje().getAuthor().getPhotoAddress(),
                transcripcion.getMensaje().getAuthor().getFechaRegistro()
        );

        return new TranscriptionResponseDto(
                transcripcion.getId(),
                transcripcion.getTextoTranscrito(),
                transcripcion.getFechaProcesamiento(),
                authorDto,
                transcripcion.getMensaje().getChannel().getChannelId()
        );
    }

    // language: java
    private MessageResponseDto mapToMessageResponseDto(Message message) {
        // Load the author entity explicitly to avoid "no Session" lazy init errors
        User authorEntity = null;
        if (message.getAuthor() != null && message.getAuthor().getUserId() != null) {
            authorEntity = userRepository.findById(message.getAuthor().getUserId()).orElse(null);
        }

        UserResponseDto authorDto = new UserResponseDto(
                authorEntity != null ? authorEntity.getUserId() : null,
                authorEntity != null ? authorEntity.getUsername() : null,
                authorEntity != null ? authorEntity.getEmail() : null,
                authorEntity != null ? authorEntity.getPhotoAddress() : null,
                authorEntity != null ? authorEntity.getFechaRegistro() : null
        );

        String messageType = "";
        String content = "";

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

    /**
     * Guarda un mensaje recibido de otro servidor P2P en la base de datos local.
     * Este método NO publica eventos locales porque el mensaje ya fue procesado en el servidor de origen.
     */
    @Override
    @Transactional
    public void guardarMensajeRemoto(UUID messageId, UUID channelId, UUID authorId,
                                     String content, String messageType,
                                     java.time.LocalDateTime timestamp) throws Exception {

        System.out.println("→ [MessageService] Guardando mensaje remoto en BD local");
        System.out.println("  MessageID: " + messageId);
        System.out.println("  Canal: " + channelId);
        System.out.println("  Autor: " + authorId);
        System.out.println("  Tipo: " + messageType);

        // 1. Verificar que el canal existe localmente
        Channel canal = channelRepository.findById(channelId)
                .orElseThrow(() -> new Exception("Canal no encontrado: " + channelId));

        // 2. Buscar el usuario autor
        User autor = userRepository.findById(authorId).orElse(null);

        // Si el usuario no existe localmente, crear un registro temporal (usuario remoto)
        if (autor == null) {
            System.out.println("  → Usuario remoto no existe localmente, creando registro temporal");
            autor = crearUsuarioRemoto(authorId);
        }

        // 3. Crear el mensaje según el tipo
        Message mensaje;
        if ("AUDIO".equals(messageType)) {
            mensaje = new AudioMessage(autor, canal, content);
        } else {
            mensaje = new TextMessage(autor, canal, content);
        }

        // 4. Establecer el ID y timestamp originales para mantener consistencia
        mensaje.setIdMensaje(messageId);  // ✅ CORREGIDO: era setMessageId
        mensaje.setTimestamp(timestamp);

        // 5. Guardar en la base de datos
        messageRepository.save(mensaje);

        System.out.println("✓ [MessageService] Mensaje remoto guardado exitosamente en BD");
    }

    /**
     * Crea un registro temporal de usuario para autores remotos.
     * Este usuario representa a alguien que está en otro servidor.
     */
    private User crearUsuarioRemoto(UUID authorId) {
        User usuarioRemoto = new User();
        usuarioRemoto.setUserId(authorId);
        usuarioRemoto.setUsername("usuario_remoto_" + authorId.toString().substring(0, 8));
        usuarioRemoto.setEmail("remoto_" + authorId.toString().substring(0, 8) + "@remote.server");
        usuarioRemoto.setHashedPassword("REMOTE_USER_NO_PASSWORD");
        usuarioRemoto.setConectado(false);

        // Guardar el usuario temporal en BD
        return userRepository.save(usuarioRemoto);
    }

}
