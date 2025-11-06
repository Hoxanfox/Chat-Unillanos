package com.arquitectura.events;

import java.util.UUID;

/**
 * Evento publicado cuando un peer se conecta a la red P2P
 */
public class PeerConnectedEvent {
    private final UUID peerId;
    private final String peerIp;
    private final int peerPort;

    public PeerConnectedEvent(UUID peerId, String peerIp, int peerPort) {
        this.peerId = peerId;
        this.peerIp = peerIp;
        this.peerPort = peerPort;
    }

    public UUID getPeerId() {
        return peerId;
    }

    public String getPeerIp() {
        return peerIp;
    }

    public int getPeerPort() {
        return peerPort;
    }
}

