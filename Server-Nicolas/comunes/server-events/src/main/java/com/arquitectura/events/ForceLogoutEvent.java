package com.arquitectura.events;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Evento que se dispara cuando el servidor necesita forzar el logout de un usuario.
 * Por ejemplo, cuando un administrador desconecta a un usuario o cuando se detecta
 * una sesión duplicada.
 */
public class ForceLogoutEvent extends ApplicationEvent {
    
    private final UUID userId;
    private final UUID peerId;
    private final String motivo;

    /**
     * Crea un nuevo evento de forzar logout.
     * @param source El objeto que originó el evento
     * @param userId ID del usuario que debe cerrar sesión
     * @param peerId ID del peer del usuario (puede ser null)
     * @param motivo Motivo del logout forzado
     */
    public ForceLogoutEvent(Object source, UUID userId, UUID peerId, String motivo) {
        super(source);
        this.userId = userId;
        this.peerId = peerId;
        this.motivo = motivo;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getPeerId() {
        return peerId;
    }

    public String getMotivo() {
        return motivo;
    }
}

