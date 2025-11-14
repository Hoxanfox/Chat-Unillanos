package com.arquitectura.transporte.peer.comm;

import com.arquitectura.controlador.peer.IPeerHandler;
import com.arquitectura.transporte.peer.PeerOutgoingConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Encapsula la lógica de envío de mensajes y consultas de estado sobre conexiones.
 */
public class PeerCommunicator {
    private static final Logger log = LoggerFactory.getLogger(PeerCommunicator.class);

    private final Map<UUID, IPeerHandler> activePeerConnections;
    private final Map<UUID, PeerOutgoingConnection> outgoingConnections;

    public PeerCommunicator(Map<UUID, IPeerHandler> activePeerConnections,
                            Map<UUID, PeerOutgoingConnection> outgoingConnections) {
        this.activePeerConnections = activePeerConnections;
        this.outgoingConnections = outgoingConnections;
    }

    public boolean sendToPeer(UUID peerId, String message) {
        IPeerHandler handler = activePeerConnections.get(peerId);
        if (handler != null && handler.isConnected()) {
            handler.sendMessage(message);
            return true;
        }

        PeerOutgoingConnection outgoing = outgoingConnections.get(peerId);
        if (outgoing != null && outgoing.isConnected()) {
            outgoing.sendMessage(message);
            return true;
        }

        log.warn("No se pudo enviar mensaje a peer {}: no conectado", peerId);
        return false;
    }

    public void broadcastToAllPeers(String message) {
        log.info("Broadcasting mensaje a {} peers", activePeerConnections.size());
        activePeerConnections.values().forEach(handler -> {
            if (handler.isConnected()) handler.sendMessage(message);
        });
        outgoingConnections.values().forEach(conn -> {
            if (conn.isConnected()) conn.sendMessage(message);
        });
    }

    public boolean isConnectedToPeer(UUID peerId) {
        IPeerHandler handler = activePeerConnections.get(peerId);
        if (handler != null && handler.isConnected()) return true;
        PeerOutgoingConnection outgoing = outgoingConnections.get(peerId);
        return outgoing != null && outgoing.isConnected();
    }

    public Set<UUID> getConnectedPeerIds() {
        Set<UUID> connected = new HashSet<>(activePeerConnections.keySet());
        outgoingConnections.forEach((id, conn) -> {
            if (conn.isConnected()) connected.add(id);
        });
        return connected;
    }

    public int getActivePeerCount() {
        return activePeerConnections.size() + (int) outgoingConnections.values().stream().filter(PeerOutgoingConnection::isConnected).count();
    }

}

