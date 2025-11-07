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
import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.DTO.peers.AddPeerRequestDto;
import com.arquitectura.DTO.peers.HeartbeatRequestDto;
import com.arquitectura.DTO.peers.HeartbeatResponseDto;
import com.arquitectura.DTO.peers.RetransmitRequestDto;
import com.arquitectura.DTO.peers.RetransmitResponseDto;
import com.arquitectura.DTO.peers.UpdatePeerListRequestDto;
import com.arquitectura.DTO.peers.UpdatePeerListResponseDto;
import com.arquitectura.events.ConnectedUsersRequestEvent;
import com.arquitectura.logicaCanales.IChannelService;
import com.arquitectura.logicaMensajes.IMessageService;
import com.arquitectura.logicaMensajes.transcripcionAudio.IAudioTranscriptionService;
import com.arquitectura.logicaUsuarios.IUserService;
import com.arquitectura.utils.chunkManager.FileChunkManager;
import com.arquitectura.utils.chunkManager.FileUploadResponse;
import com.arquitectura.utils.file.IFileStorageService;
import com.arquitectura.utils.logs.ILogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final com.arquitectura.logicaUsuarios.IPeerService peerServiceUsuarios;
    private final com.arquitectura.logicaPeers.IPeerService peerServicePeers;

    @Autowired
    public ChatFachadaImpl(IUserService userService, IChannelService channelService, IMessageService messageService, IAudioTranscriptionService transcriptionService, IFileStorageService fileStorageService, ApplicationEventPublisher eventPublisher, FileChunkManager fileChunkManager, ILogService logService, @Qualifier("peerServiceUsuarios") com.arquitectura.logicaUsuarios.IPeerService peerServiceUsuarios, @Qualifier("peerServiceP2P") com.arquitectura.logicaPeers.IPeerService peerServicePeers) {
        this.userService = userService;
        this.channelService = channelService;
        this.messageService = messageService;
        this.transcriptionService = transcriptionService;
        this.fileStorageService = fileStorageService;
        this.eventPublisher = eventPublisher;
        this.fileChunkManager = fileChunkManager;
        this.logService = logService;
        this.peerServiceUsuarios = peerServiceUsuarios;
        this.peerServicePeers = peerServicePeers;
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

    @Override
    public void enviarPedidoLogout(UUID userId, String motivo) throws Exception {
        // Verificar que el usuario existe
        UserResponseDto usuario = userService.getUsersByIds(Set.of(userId)).stream()
                .findFirst()
                .orElseThrow(() -> new Exception("Usuario con ID " + userId + " no encontrado"));

        // Publicar evento para enviar la notificación push de logout
        UUID peerId = usuario.getPeerId();
        eventPublisher.publishEvent(new com.arquitectura.events.ForceLogoutEvent(
                this,
                userId,
                peerId,
                motivo != null ? motivo : "Sesión cerrada por el servidor"
        ));
    }

    // --- MÉTODOS DE PEER ---
    @Override
    public List<com.arquitectura.DTO.peers.PeerResponseDto> listarPeersDisponibles(UUID excludePeerId) throws Exception {
        return peerServiceUsuarios.listarPeersDisponibles(excludePeerId);
    }

    @Override
    public HeartbeatResponseDto reportarLatido(HeartbeatRequestDto requestDto) throws Exception {
        return peerServiceUsuarios.reportarLatido(requestDto);
    }

    @Override
    public com.arquitectura.DTO.peers.PeerResponseDto añadirPeer(AddPeerRequestDto requestDto) throws Exception {
        return peerServiceUsuarios.añadirPeer(requestDto);
    }

    @Override
    public com.arquitectura.DTO.peers.PeerResponseDto verificarEstadoPeer(UUID peerId) throws Exception {
        return peerServiceUsuarios.verificarEstadoPeer(peerId);
    }

    @Override
    public RetransmitResponseDto retransmitirPeticion(RetransmitRequestDto requestDto) throws Exception {
        return peerServiceUsuarios.retransmitirPeticion(requestDto);
    }

    @Override
    public UpdatePeerListResponseDto actualizarListaPeers(UpdatePeerListRequestDto requestDto) throws Exception {
        return peerServiceUsuarios.actualizarListaPeers(requestDto);
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
    public List<MessageResponseDto> obtenerHistorialPrivado(UUID remitenteId, UUID destinatarioId) throws Exception {
        return messageService.obtenerHistorialPrivado(remitenteId, destinatarioId);
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
    public com.arquitectura.DTO.p2p.PeerResponseDto agregarPeer(String ip, int puerto) throws Exception {
        return peerServicePeers.agregarPeer(ip, puerto);
    }
    
    @Override
    public com.arquitectura.DTO.p2p.PeerResponseDto agregarPeer(String ip, int puerto, String nombreServidor) throws Exception {
        return peerServicePeers.agregarPeer(ip, puerto, nombreServidor);
    }
    
    @Override
    public List<com.arquitectura.DTO.p2p.PeerResponseDto> listarPeersDisponibles() {
        return peerServicePeers.listarPeersDisponibles();
    }
    
    @Override
    public List<com.arquitectura.DTO.p2p.PeerResponseDto> listarPeersActivos() {
        return peerServicePeers.listarPeersActivos();
    }
    
    @Override
    public void reportarLatido(UUID peerId) throws Exception {
        peerServicePeers.reportarLatido(peerId);
    }
    
    @Override
    public void reportarLatido(UUID peerId, String ip, int puerto) throws Exception {
        peerServicePeers.reportarLatido(peerId, ip, puerto);
    }
    
    @Override
    public long obtenerIntervaloHeartbeat() {
        return peerServicePeers.obtenerIntervaloHeartbeat();
    }
    
    @Override
    public DTOResponse retransmitirPeticion(UUID peerDestinoId, DTORequest peticionOriginal) throws Exception {
        return peerServicePeers.retransmitirPeticion(peerDestinoId, peticionOriginal);
    }

    @Override
    public byte[] descargarArchivoDesdePeer(UUID peerDestinoId, String fileId) throws Exception {
        return peerServicePeers.descargarArchivoDesdePeer(peerDestinoId, fileId);
    }

}
