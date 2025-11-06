package com.arquitectura.events;

import java.util.List;
import java.util.UUID;

/**
 * Evento publicado cuando la lista de peers disponibles cambia
 */
public class PeerListUpdatedEvent {
    private final List<UUID> activePeerIds;

    public PeerListUpdatedEvent(List<UUID> activePeerIds) {
        this.activePeerIds = activePeerIds;
    }

    public List<UUID> getActivePeerIds() {
        return activePeerIds;
    }
}

