package com.arquitectura.events;

import java.util.UUID;

/**
 * Evento para retransmitir una respuesta al peer de origen
 */
public class RetransmitToOriginPeerEvent {
    private final UUID originPeerId;
    private final Object response;

    public RetransmitToOriginPeerEvent(UUID originPeerId, Object response) {
        this.originPeerId = originPeerId;
        this.response = response;
    }

    public UUID getOriginPeerId() {
        return originPeerId;
    }

    public Object getResponse() {
        return response;
    }
}
