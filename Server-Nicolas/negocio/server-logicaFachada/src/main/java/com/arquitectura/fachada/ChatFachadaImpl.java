package com.arquitectura.fachada;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;
import com.arquitectura.DTO.archivos.DTODownloadInfo;
import com.arquitectura.DTO.archivos.DTOEndUpload;
import com.arquitectura.DTO.archivos.DTOStartUpload;
import com.arquitectura.DTO.archivos.DTOUploadChunk;
import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.canales.CreateChannelRequestDto;
import com.arquitectura.DTO.Mensajes.SendMessageRequestDto;
import com.arquitectura.DTO.canales.InviteMemberRequestDto;
import com.arquitectura.DTO.canales.RespondToInviteRequestDto;
import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.events.ConnectedUsersRequestEvent;
import com.arquitectura.logicaCanales.IChannelService;
import com.arquitectura.logicaMensajes.IMessageService;
import com.arquitectura.logicaMensajes.transcripcionAudio.IAudioTranscriptionService;
import com.arquitectura.logicaPeers.IPeerService;
import com.arquitectura.logicaUsuarios.IUserService;
import com.arquitectura.utils.chunkManager.FileChunkManager;
import com.arquitectura.utils.chunkManager.FileUploadResponse;
import com.arquitectura.utils.file.IFileStorageService;
import com.arquitectura.utils.logs.ILogService;
import com.arquitectura.utils.logs.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class ChatFachadaImpl implements IChatFachada {

    private final IUserService userService;
    private final IChannelService channelService;
    private final IMessageService messageService;
    private final IAudioTranscriptionService transcriptionService;
    private final IFileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final FileChunkManager fileChunkManager;
    private final ILogService logService;
    private final IPeerService peerService;

    @Autowired
    public ChatFachadaImpl(IUserService userService, IChannelService channelService, IMessageService messageService, IAudioTranscriptionService transcriptionService, IFileStorageService fileStorageService, ApplicationEventPublisher eventPublisher, FileChunkManager fileChunkManager, ILogService logService, IPeerService peerService) {
        this.userService = userService;
        this.channelService = channelService;
        this.messageService = messageService;
        this.transcriptionService = transcriptionService;
        this.fileStorageService = fileStorageService;
        this.eventPublisher = eventPublisher;
        this.fileChunkManager = fileChunkManager;
        this.logService = logService;
        this.peerService = peerService;
    }

    // Metodos de usuario
    @Override
    public void registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress) throws Exception {
        userService.registrarUsuario(requestDto, ipAddress);
    }

    @Override
    public Optional<UserResponseDto> buscarUsuarioPorUsername(String username) {
        return userService.buscarPorUsername(username);
    }

    @Override
    public List<UserResponseDto> obtenerTodosLosUsuarios() {
        return userService.obtenerTodosLosUsuarios();
    }

    @Override
    public UserResponseDto autenticarUsuario(LoginRequestDto requestDto, String ipAddress) throws Exception {
        return userService.autenticarUsuario(requestDto, ipAddress);
    }
    @Override
    public List<UserResponseDto> getUsersByIds(Set<UUID> userIds) {
        return userService.getUsersByIds(userIds);
    }

    @Override
    public List<UserResponseDto> obtenerUsuariosConectados() {
        ConnectedUsersRequestEvent requestEvent = new ConnectedUsersRequestEvent(this);
        eventPublisher.publishEvent(requestEvent);
        Set<UUID> connectedUserIds = requestEvent.getResponseContainer();
        if (connectedUserIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userService.getUsersByIds(connectedUserIds);
    }

    @Override
    public void cambiarEstadoUsuario(UUID userId, boolean nuevoEstado) throws Exception {
        userService.cambiarEstadoUsuario(userId, nuevoEstado);
    }

    @Override
    public List<UserResponseDto> listarContactos(UUID excludeUserId) {
        return userService.listarContactos(excludeUserId);
    }

    // --- MÉTODOS DE Canales ---
    @Override
    public ChannelResponseDto crearCanal(CreateChannelRequestDto requestDto, UUID ownerId) throws Exception {
        return channelService.crearCanal(requestDto, ownerId);
    }
    @Override
    public ChannelResponseDto crearCanalDirecto(UUID user1Id, UUID user2Id) throws Exception {
        return channelService.crearCanalDirecto(user1Id, user2Id);
    }

    @Override
    public void agregarMiembroACanal(InviteMemberRequestDto inviteMemberRequestDto, UUID userId) throws Exception {
        channelService.invitarMiembro(inviteMemberRequestDto, userId);
    }

    @Override
    public List<ChannelResponseDto> obtenerTodosLosCanales() {
        return channelService.obtenerTodosLosCanales();
    }
    @Override
    public Map<ChannelResponseDto, List<UserResponseDto>> obtenerCanalesConMiembros() {
        return channelService.obtenerCanalesConMiembros();
    }
    @Override
    public List<ChannelResponseDto> obtenerCanalesPorUsuario(UUID userId) {
        // La fachada simplemente delega la llamada al servicio de canales.
        return channelService.obtenerCanalesPorUsuario(userId);
    }
    @Override
    public void invitarMiembro(InviteMemberRequestDto requestDto, UUID ownerId) throws Exception {
        channelService.invitarMiembro(requestDto, ownerId);
    }
    @Override
    public void responderInvitacion(RespondToInviteRequestDto requestDto, UUID userId) throws Exception {
        channelService.responderInvitacion(requestDto, userId);
    }
    @Override
    public List<ChannelResponseDto> getPendingInvitationsForUser(UUID userId) {
        return channelService.getPendingInvitationsForUser(userId);
    }

    @Override
    public List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId) throws Exception {
        return channelService.obtenerMiembrosDeCanal(canalId, solicitanteId);
    }

    
    // ---Metodos de Mensajes---
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
    public void enviarMensajeBroadcast(String contenido, UUID adminId) throws Exception {
        messageService.enviarMensajeBroadcast(contenido, adminId);
    }
    @Override
    public List<TranscriptionResponseDto> obtenerTranscripciones() {
        return transcriptionService.getAllTranscriptions();
    }

    @Override
    public String getFileAsBase64(String relativePath) {
        try {
            return fileStorageService.readFileAsBase64(relativePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    // --- MÉTODOS PARA INFORMES ---
    @Override
    public String getLogContents() throws IOException {
        return logService.getLogContents();
    }
    // --- IMPLEMENTACIÓN DE TRANSFERENCIA DE ARCHIVOS ---
    @Override
    public String startUpload(DTOStartUpload dto) throws Exception {
        // Simplemente delegamos al manager
        return fileChunkManager.startUpload(dto);
    }

    @Override
    public void processChunk(DTOUploadChunk dto) throws Exception {
        // Delegamos al manager
        fileChunkManager.processChunk(dto);
    }

    @Override
    public FileUploadResponse endUpload(DTOEndUpload dto, UUID autorId, String subDirectory) throws Exception {
        // Delegamos al manager
        return fileChunkManager.endUpload(dto, autorId, subDirectory);
    }

    @Override
    public DTODownloadInfo startDownload(String fileId) throws Exception {
        // Delegamos al manager
        return fileChunkManager.startDownload(fileId);
    }

    @Override
    public byte[] getChunk(String downloadId, int chunkNumber) throws Exception {
        // Delegamos al manager
        return fileChunkManager.getChunk(downloadId, chunkNumber);
    }

    // --- IMPLEMENTACIÓN DE MÉTODOS P2P ---
    
    @Override
    public PeerResponseDto agregarPeer(String ip, int puerto) throws Exception {
        return peerService.agregarPeer(ip, puerto);
    }
    
    @Override
    public PeerResponseDto agregarPeer(String ip, int puerto, String nombreServidor) throws Exception {
        return peerService.agregarPeer(ip, puerto, nombreServidor);
    }
    
    @Override
    public List<PeerResponseDto> listarPeersDisponibles() {
        return peerService.listarPeersDisponibles();
    }
    
    @Override
    public List<PeerResponseDto> listarPeersActivos() {
        return peerService.listarPeersActivos();
    }
    
    @Override
    public void reportarLatido(UUID peerId) throws Exception {
        peerService.reportarLatido(peerId);
    }
    
    @Override
    public void reportarLatido(UUID peerId, String ip, int puerto) throws Exception {
        peerService.reportarLatido(peerId, ip, puerto);
    }
    
    @Override
    public long obtenerIntervaloHeartbeat() {
        return peerService.obtenerIntervaloHeartbeat();
    }
    
    @Override
    public DTOResponse retransmitirPeticion(UUID peerDestinoId, DTORequest peticionOriginal) throws Exception {
        return peerService.retransmitirPeticion(peerDestinoId, peticionOriginal);
    }

}