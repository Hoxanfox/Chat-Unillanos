package com.arquitectura.logicaPeers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.domain.Peer;
import com.arquitectura.domain.User;
import com.arquitectura.domain.Channel;
import com.arquitectura.persistence.repository.PeerRepository;
import com.arquitectura.persistence.repository.UserRepository;
import com.arquitectura.persistence.repository.ChannelRepository;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación del servicio de notificaciones entre peers.
 * Gestiona la comunicación transparente entre servidores.
 */
@Service("peerNotificationService")
public class PeerNotificationServiceImpl implements IPeerNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PeerNotificationServiceImpl.class);
    private static final int CONNECTION_TIMEOUT_MS = 5000;

    private final PeerRepository peerRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final Gson gson;

    @Autowired
    public PeerNotificationServiceImpl(PeerRepository peerRepository,
                                      UserRepository userRepository,
                                      ChannelRepository channelRepository,
                                      Gson gson) {
        this.peerRepository = peerRepository;
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.gson = gson;
        log.info("✓ [PeerNotificationService] Servicio de notificaciones P2P inicializado");
    }

    @Override
    public boolean notificarNuevoMensaje(UUID peerDestinoId, MessageResponseDto mensaje) {
        log.info("→ [PeerNotificationService] Notificando nuevo mensaje al peer {}", peerDestinoId);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("messageId", mensaje.getMessageId().toString());
            payload.put("channelId", mensaje.getChannelId().toString());
            payload.put("authorId", mensaje.getAuthor().getUserId().toString());
            payload.put("authorUsername", mensaje.getAuthor().getUsername());
            payload.put("content", mensaje.getContent());
            payload.put("messageType", mensaje.getMessageType());
            payload.put("timestamp", mensaje.getTimestamp().toString());

            DTORequest request = new DTORequest("notificarMensaje", payload);
            DTOResponse response = enviarPeticionAPeer(peerDestinoId, request);

            boolean exito = response != null && "success".equals(response.getStatus());
            log.info("✓ [PeerNotificationService] Notificación de mensaje: {}", exito ? "exitosa" : "fallida");
            return exito;

        } catch (Exception e) {
            log.error("✗ [PeerNotificationService] Error al notificar mensaje: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean notificarInvitacionCanal(UUID peerDestinoId, UUID canalId,
                                           UUID usuarioInvitadoId, UUID usuarioInvitadorId) {
        log.info("→ [PeerNotificationService] Notificando invitación a canal al peer {}", peerDestinoId);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("canalId", canalId.toString());
            payload.put("usuarioInvitadoId", usuarioInvitadoId.toString());
            payload.put("usuarioInvitadorId", usuarioInvitadorId.toString());

            DTORequest request = new DTORequest("notificarInvitacionCanal", payload);
            DTOResponse response = enviarPeticionAPeer(peerDestinoId, request);

            boolean exito = response != null && "success".equals(response.getStatus());
            log.info("✓ [PeerNotificationService] Notificación de invitación: {}", exito ? "exitosa" : "fallida");
            return exito;

        } catch (Exception e) {
            log.error("✗ [PeerNotificationService] Error al notificar invitación: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean notificarAceptacionInvitacion(UUID peerDestinoId, UUID canalId, UUID usuarioId) {
        log.info("→ [PeerNotificationService] Notificando aceptación de invitación al peer {}", peerDestinoId);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("canalId", canalId.toString());
            payload.put("usuarioId", usuarioId.toString());

            DTORequest request = new DTORequest("notificarAceptacionInvitacion", payload);
            DTOResponse response = enviarPeticionAPeer(peerDestinoId, request);

            boolean exito = response != null && "success".equals(response.getStatus());
            log.info("✓ [PeerNotificationService] Notificación de aceptación: {}", exito ? "exitosa" : "fallida");
            return exito;

        } catch (Exception e) {
            log.error("✗ [PeerNotificationService] Error al notificar aceptación: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public UserResponseDto solicitarInfoUsuario(UUID peerDestinoId, UUID usuarioId) {
        log.info("→ [PeerNotificationService] Solicitando info de usuario {} al peer {}", usuarioId, peerDestinoId);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("usuarioId", usuarioId.toString());

            DTORequest request = new DTORequest("obtenerInfoUsuario", payload);
            DTOResponse response = enviarPeticionAPeer(peerDestinoId, request);

            if (response != null && "success".equals(response.getStatus()) && response.getData() != null) {
                // Convertir el data a UserResponseDto
                String jsonData = gson.toJson(response.getData());
                UserResponseDto user = gson.fromJson(jsonData, UserResponseDto.class);
                log.info("✓ [PeerNotificationService] Info de usuario obtenida");
                return user;
            }

        } catch (Exception e) {
            log.error("✗ [PeerNotificationService] Error al solicitar info de usuario: {}", e.getMessage());
        }

        return null;
    }

    @Override
    public ChannelResponseDto solicitarInfoCanal(UUID peerDestinoId, UUID canalId) {
        log.info("→ [PeerNotificationService] Solicitando info de canal {} al peer {}", canalId, peerDestinoId);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("canalId", canalId.toString());

            DTORequest request = new DTORequest("obtenerInfoCanal", payload);
            DTOResponse response = enviarPeticionAPeer(peerDestinoId, request);

            if (response != null && "success".equals(response.getStatus()) && response.getData() != null) {
                // Convertir el data a ChannelResponseDto
                String jsonData = gson.toJson(response.getData());
                ChannelResponseDto channel = gson.fromJson(jsonData, ChannelResponseDto.class);
                log.info("✓ [PeerNotificationService] Info de canal obtenida");
                return channel;
            }

        } catch (Exception e) {
            log.error("✗ [PeerNotificationService] Error al solicitar info de canal: {}", e.getMessage());
        }

        return null;
    }

    @Override
    public DTOResponse enviarPeticionAPeer(UUID peerDestinoId, DTORequest peticion) throws Exception {
        log.debug("→ [PeerNotificationService] Enviando petición '{}' al peer {}", peticion.getAction(), peerDestinoId);

        // Obtener información del peer destino
        Peer peerDestino = peerRepository.findById(peerDestinoId)
                .orElseThrow(() -> new Exception("Peer destino no encontrado: " + peerDestinoId));

        if (!peerDestino.estaActivo()) {
            throw new Exception("El peer destino no está activo: " + peerDestinoId);
        }

        // Establecer conexión TCP con el peer
        try (Socket socket = new Socket(peerDestino.getIp(), peerDestino.getPuerto())) {
            socket.setSoTimeout(CONNECTION_TIMEOUT_MS);

            // Enviar la petición
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String requestJson = gson.toJson(peticion);
            log.debug("  Enviando: {}", requestJson);
            out.println(requestJson);

            // Leer la respuesta
            String responseLine = in.readLine();
            if (responseLine == null) {
                throw new Exception("No se recibió respuesta del peer");
            }

            log.debug("  Recibido: {}", responseLine);
            DTOResponse response = gson.fromJson(responseLine, DTOResponse.class);

            log.debug("✓ [PeerNotificationService] Respuesta recibida con status: {}", response.getStatus());
            return response;

        } catch (Exception e) {
            log.error("✗ [PeerNotificationService] Error en comunicación con peer: {}", e.getMessage());
            throw new Exception("Error al comunicarse con peer " + peerDestinoId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean usuarioPerteneceAPeer(UUID usuarioId, UUID peerId) {
        Optional<User> userOpt = userRepository.findById(usuarioId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Peer userPeer = user.getPeerId();
            return userPeer != null && userPeer.getPeerId().equals(peerId);
        }
        return false;
    }

    @Override
    public UUID obtenerPeerDeUsuario(UUID usuarioId) {
        Optional<User> userOpt = userRepository.findById(usuarioId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Peer peer = user.getPeerId();
            return peer != null ? peer.getPeerId() : null;
        }
        return null;
    }

    @Override
    public UUID obtenerPeerDeCanal(UUID canalId) {
        Optional<Channel> channelOpt = channelRepository.findById(canalId);
        if (channelOpt.isPresent()) {
            Channel channel = channelOpt.get();
            Peer peer = channel.getPeerId();
            return peer != null ? peer.getPeerId() : null;
        }
        return null;
    }
}
