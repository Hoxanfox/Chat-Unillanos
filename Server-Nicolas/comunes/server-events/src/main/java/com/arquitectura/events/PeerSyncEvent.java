package com.arquitectura.events;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Evento disparado cuando se recibe una sincronización P2P de cambio de estado de usuario.
 * Este evento se usa para propagar cambios de usuarios entre peers a los clientes conectados.
 */
public class PeerSyncEvent extends ApplicationEvent {

    private final UUID usuarioId;
    private final String nuevoEstado;

    public PeerSyncEvent(Object source, UUID usuarioId, String nuevoEstado) {
        super(source);
        this.usuarioId = usuarioId;
        this.nuevoEstado = nuevoEstado;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getNuevoEstado() {
        return nuevoEstado;
    }
}

