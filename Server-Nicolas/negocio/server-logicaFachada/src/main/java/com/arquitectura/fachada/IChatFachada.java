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
import com.arquitectura.DTO.peers.AddPeerRequestDto;
import com.arquitectura.DTO.peers.HeartbeatRequestDto;
import com.arquitectura.DTO.peers.HeartbeatResponseDto;
import com.arquitectura.DTO.peers.PeerResponseDto;
import com.arquitectura.DTO.peers.RetransmitRequestDto;
import com.arquitectura.DTO.peers.RetransmitResponseDto;
import com.arquitectura.DTO.peers.UpdatePeerListRequestDto;
import com.arquitectura.DTO.peers.UpdatePeerListResponseDto;
import com.arquitectura.utils.chunkManager.FileUploadResponse;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IChatFachada {

    // --- Métodos de Usuario ---
    void registrarUsuario(UserRegistrationRequestDto requestDto, String ipAddress) throws Exception;
    Optional<UserResponseDto> buscarUsuarioPorUsername(String username);
    List<UserResponseDto> obtenerTodosLosUsuarios();
    UserResponseDto autenticarUsuario(LoginRequestDto requestDto, String ipAddress) throws Exception;
    List<UserResponseDto> getUsersByIds(Set<UUID> userIds);
    List<UserResponseDto> obtenerUsuariosConectados();
    void cambiarEstadoUsuario(UUID userId, boolean nuevoEstado) throws Exception;
    List<UserResponseDto> listarContactos(UUID excludeUserId);


    // --- Métodos de Peer ---
    List<PeerResponseDto> listarPeersDisponibles(UUID excludePeerId) throws Exception;
    HeartbeatResponseDto reportarLatido(HeartbeatRequestDto requestDto) throws Exception;
    PeerResponseDto añadirPeer(AddPeerRequestDto requestDto) throws Exception;
    PeerResponseDto verificarEstadoPeer(UUID peerId) throws Exception;
    RetransmitResponseDto retransmitirPeticion(RetransmitRequestDto requestDto) throws Exception;
    UpdatePeerListResponseDto actualizarListaPeers(UpdatePeerListRequestDto requestDto) throws Exception;

    // --- Métodos de Canal ---
    ChannelResponseDto crearCanal(CreateChannelRequestDto requestDto, UUID ownerId) throws Exception;
    ChannelResponseDto crearCanalDirecto(UUID user1Id, UUID user2Id) throws Exception;
    Map<ChannelResponseDto, List<UserResponseDto>> obtenerCanalesConMiembros();
    List<ChannelResponseDto> obtenerCanalesPorUsuario(UUID userId);
    void invitarMiembro(InviteMemberRequestDto requestDto, UUID ownerId) throws Exception;
    void responderInvitacion(RespondToInviteRequestDto requestDto, UUID userId) throws Exception;
    List<ChannelResponseDto> getPendingInvitationsForUser(UUID userId);
    void agregarMiembroACanal(InviteMemberRequestDto inviteMemberRequestDto, UUID userId) throws Exception;
    List<ChannelResponseDto> obtenerTodosLosCanales();
    
    /**
     * Obtiene la lista de miembros de un canal.
     * @param canalId El ID del canal.
     * @param solicitanteId El ID del usuario que solicita la lista.
     * @return Lista de usuarios que son miembros del canal.
     * @throws Exception si el canal no existe o el solicitante no es miembro.
     */
    List<UserResponseDto> obtenerMiembrosDeCanal(UUID canalId, UUID solicitanteId) throws Exception;

    // --- MÉTODOS DE MENSAJE (ACTUALIZADOS) ---
    void enviarMensajeBroadcast(String contenido, UUID adminId) throws Exception;
    MessageResponseDto enviarMensajeTexto(SendMessageRequestDto requestDto, UUID autorId) throws Exception;
    MessageResponseDto enviarMensajeAudio(SendMessageRequestDto requestDto, UUID autorId) throws Exception;
    List<MessageResponseDto> obtenerMensajesDeCanal(UUID canalId, UUID userId) throws Exception;

    /**
     * Obtiene el historial de mensajes privados entre dos usuarios.
     * @param remitenteId ID del usuario que solicita el historial
     * @param destinatarioId ID del contacto con quien tiene la conversación
     * @return Lista de MessageResponseDto con el historial privado
     * @throws Exception en caso de error o falta de permisos
     */
    List<MessageResponseDto> obtenerHistorialPrivado(UUID remitenteId, UUID destinatarioId) throws Exception;

    String guardarArchivoDeAudio(String fileName, String base64Data, UUID autorId) throws IOException;
    List<TranscriptionResponseDto> obtenerTranscripciones();

    //Metodos de Utils
    String getFileAsBase64(String relativePath)throws IOException;
    String getLogContents() throws IOException;
    // --- MÉTODOS DE TRANSFERENCIA DE ARCHIVOS ---

    String startUpload(DTOStartUpload dto) throws Exception;
    void processChunk(DTOUploadChunk dto) throws Exception;
    FileUploadResponse endUpload(DTOEndUpload dto, UUID autorId, String subDirectory) throws Exception;
    DTODownloadInfo startDownload(String fileId) throws Exception;
    byte[] getChunk(String downloadId, int chunkNumber) throws Exception;

    // --- MÉTODOS P2P (PEER-TO-PEER) ---
    
    /**
     * Agrega un nuevo peer a la red P2P.
     * @param ip Dirección IP del peer
     * @param puerto Puerto del peer
     * @return DTO con la información del peer agregado
     * @throws Exception si hay error al agregar el peer
     */
    PeerResponseDto agregarPeer(String ip, int puerto) throws Exception;
    
    /**
     * Agrega un nuevo peer con nombre de servidor.
     * @param ip Dirección IP del peer
     * @param puerto Puerto del peer
     * @param nombreServidor Nombre descriptivo del servidor
     * @return DTO con la información del peer agregado
     * @throws Exception si hay error al agregar el peer
     */
    PeerResponseDto agregarPeer(String ip, int puerto, String nombreServidor) throws Exception;
    
    /**
     * Lista todos los peers disponibles en la red.
     * @return Lista de DTOs con información de todos los peers
     */
    List<PeerResponseDto> listarPeersDisponibles();
    
    /**
     * Lista solo los peers que están activos (ONLINE).
     * @return Lista de DTOs con información de peers activos
     */
    List<PeerResponseDto> listarPeersActivos();
    
    /**
     * Reporta un latido (heartbeat) de un peer.
     * @param peerId ID del peer que reporta el latido
     * @throws Exception si el peer no existe
     */
    void reportarLatido(UUID peerId) throws Exception;
    
    /**
     * Reporta un latido con información completa del peer.
     * @param peerId ID del peer
     * @param ip IP del peer
     * @param puerto Puerto del peer
     * @throws Exception si hay error al procesar el latido
     */
    void reportarLatido(UUID peerId, String ip, int puerto) throws Exception;
    
    /**
     * Obtiene el intervalo configurado para heartbeats en milisegundos.
     * @return Intervalo de heartbeat en ms
     */
    long obtenerIntervaloHeartbeat();
    
    /**
     * Retransmite una petición a otro peer en la red.
     * @param peerDestinoId ID del peer destino
     * @param peticionOriginal Petición original a retransmitir
     * @return Respuesta del peer destino
     * @throws Exception si hay error en la retransmisión
     */
    DTOResponse retransmitirPeticion(UUID peerDestinoId, DTORequest peticionOriginal) throws Exception;

}