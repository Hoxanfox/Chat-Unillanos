package com.arquitectura.events;

import java.util.UUID;

/**
 * Evento publicado cuando un peer se desconecta de la red P2P
 */
public class PeerDisconnectedEvent {
    private final UUID peerId;
    private final String razon;

    public PeerDisconnectedEvent(UUID peerId, String razon) {
        this.peerId = peerId;
        this.razon = razon;
    }

    public UUID getPeerId() {
        return peerId;
    }

    public String getRazon() {
        return razon;
    }
}

