package com.arquitectura.logicaCanales;

import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.domain.Channel;
import com.arquitectura.domain.MembresiaCanal;
import com.arquitectura.domain.MembresiaCanalId;
import com.arquitectura.domain.User;
import com.arquitectura.domain.enums.EstadoMembresia;
import com.arquitectura.events.UserInvitedEvent;
import com.arquitectura.persistence.repository.MembresiaCanalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Servicio helper para manejar invitaciones cross-server (P2P).
 * Gestiona la notificación de invitaciones a usuarios en otros servidores.
 */
@Service
public class ChannelInvitationP2PService {

    private final MembresiaCanalRepository membresiaCanalRepository;
    private final ApplicationEventPublisher eventPublisher;
    private com.arquitectura.logicaPeers.IPeerNotificationService peerNotificationService;

    @Autowired
    public ChannelInvitationP2PService(
            MembresiaCanalRepository membresiaCanalRepository,
            ApplicationEventPublisher eventPublisher) {
        this.membresiaCanalRepository = membresiaCanalRepository;
        this.eventPublisher = eventPublisher;
    }

    @Autowired(required = false)
    public void setPeerNotificationService(com.arquitectura.logicaPeers.IPeerNotificationService peerNotificationService) {
        this.peerNotificationService = peerNotificationService;
    }

    /**
     * Procesa una invitación, determinando si es local o requiere notificación P2P.
     *
     * @param channel El canal al que se invita
     * @param userToInvite El usuario invitado
     * @param inviterId El ID del usuario que invita
     * @return true si se procesó correctamente
     */
    public boolean procesarInvitacion(Channel channel, User userToInvite, UUID inviterId) {
        MembresiaCanalId membresiaId = new MembresiaCanalId(
            channel.getChannelId(),
            userToInvite.getUserId()
        );

        // Crear la invitación pendiente en BD
        MembresiaCanal nuevaInvitacion = new MembresiaCanal(
            membresiaId,
            userToInvite,
            channel,
            EstadoMembresia.PENDIENTE
        );
        membresiaCanalRepository.save(nuevaInvitacion);

        System.out.println("→ [ChannelInvitation] Invitación guardada en BD: " +
                         userToInvite.getUsername() + " al canal " + channel.getName());

        // Verificar si el usuario está en otro servidor
        if (esUsuarioRemoto(userToInvite)) {
            notificarInvitacionAPeer(channel, userToInvite, inviterId);
        } else {
            // Usuario local - publicar evento normal
            ChannelResponseDto channelDto = mapToChannelResponseDto(channel);
            eventPublisher.publishEvent(
                new UserInvitedEvent(this, userToInvite.getUserId(), channelDto)
            );
            System.out.println("✓ [ChannelInvitation] Evento local publicado");
        }

        return true;
    }

    /**
     * Verifica si un usuario está en otro servidor (es remoto).
     */
    private boolean esUsuarioRemoto(User user) {
        if (peerNotificationService == null) {
            return false; // Si no hay servicio P2P, asumimos que es local
        }

        UUID peerDelUsuario = peerNotificationService.obtenerPeerDeUsuario(user.getUserId());

        if (peerDelUsuario == null) {
            return false; // Usuario sin peer asignado = local
        }

        // Obtener el peer local
        try {
            com.arquitectura.logicaPeers.IPeerService peerService =
                (com.arquitectura.logicaPeers.IPeerService) org.springframework.context.ApplicationContextProvider
                    .getApplicationContext().getBean("peerServiceP2P");

            UUID localPeerId = peerService.obtenerPeerActualId();

            // Si el peer del usuario es diferente al local, es remoto
            return !peerDelUsuario.equals(localPeerId);

        } catch (Exception e) {
            System.err.println("✗ [ChannelInvitation] Error al verificar peer: " + e.getMessage());
            return false;
        }
    }

    /**
     * Notifica a un peer remoto sobre una invitación a canal.
     */
    private void notificarInvitacionAPeer(Channel channel, User userToInvite, UUID inviterId) {
        if (peerNotificationService == null) {
            System.err.println("✗ [ChannelInvitation] Servicio P2P no disponible");
            return;
        }

        UUID peerDelUsuario = peerNotificationService.obtenerPeerDeUsuario(userToInvite.getUserId());

        if (peerDelUsuario == null) {
            System.err.println("✗ [ChannelInvitation] No se pudo determinar el peer del usuario");
            return;
        }

        System.out.println("→ [ChannelInvitation] Notificando invitación a peer " + peerDelUsuario);
        System.out.println("  Canal: " + channel.getName() + " (" + channel.getChannelId() + ")");
        System.out.println("  Usuario invitado: " + userToInvite.getUsername());

        // Notificar de forma asíncrona para no bloquear
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            boolean exito = peerNotificationService.notificarInvitacionCanal(
                peerDelUsuario,
                channel.getChannelId(),
                userToInvite.getUserId(),
                inviterId
            );

            if (exito) {
                System.out.println("✓ [ChannelInvitation] Invitación notificada exitosamente a peer " + peerDelUsuario);
            } else {
                System.err.println("✗ [ChannelInvitation] Fallo al notificar invitación a peer " + peerDelUsuario);
            }
        });
    }

    /**
     * Procesa la aceptación de una invitación y notifica al peer del canal si es necesario.
     */
    public void procesarAceptacionInvitacion(Channel channel, UUID userId) {
        System.out.println("→ [ChannelInvitation] Procesando aceptación de invitación");
        System.out.println("  Usuario: " + userId);
        System.out.println("  Canal: " + channel.getName());

        // Verificar si el canal está en otro servidor
        if (esCanalRemoto(channel) && peerNotificationService != null) {
            UUID peerDelCanal = peerNotificationService.obtenerPeerDeCanal(channel.getChannelId());

            if (peerDelCanal != null) {
                System.out.println("→ [ChannelInvitation] Notificando aceptación al peer del canal: " + peerDelCanal);

                // Notificar al servidor donde está el canal
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    boolean exito = peerNotificationService.notificarAceptacionInvitacion(
                        peerDelCanal,
                        channel.getChannelId(),
                        userId
                    );

                    if (exito) {
                        System.out.println("✓ [ChannelInvitation] Aceptación notificada exitosamente");
                    } else {
                        System.err.println("✗ [ChannelInvitation] Fallo al notificar aceptación");
                    }
                });
            }
        }
    }

    /**
     * Verifica si un canal está en otro servidor.
     */
    private boolean esCanalRemoto(Channel canal) {
        if (peerNotificationService == null || canal.getPeerId() == null) {
            return false;
        }

        try {
            com.arquitectura.logicaPeers.IPeerService peerService =
                (com.arquitectura.logicaPeers.IPeerService) org.springframework.context.ApplicationContextProvider
                    .getApplicationContext().getBean("peerServiceP2P");

            UUID localPeerId = peerService.obtenerPeerActualId();
            return !canal.getPeerId().getPeerId().equals(localPeerId);

        } catch (Exception e) {
            return false;
        }
    }

    private ChannelResponseDto mapToChannelResponseDto(Channel channel) {
        com.arquitectura.DTO.usuarios.UserResponseDto ownerDto = new com.arquitectura.DTO.usuarios.UserResponseDto(
            channel.getOwner().getUserId(),
            channel.getOwner().getUsername(),
            channel.getOwner().getEmail(),
            channel.getOwner().getPhotoAddress(),
            channel.getOwner().getFechaRegistro()
        );

        UUID peerId = channel.getPeerId() != null ? channel.getPeerId().getPeerId() : null;

        return new ChannelResponseDto(
            channel.getChannelId(),
            channel.getName(),
            channel.getTipo().toString(),
            ownerDto,
            peerId
        );
    }
}

