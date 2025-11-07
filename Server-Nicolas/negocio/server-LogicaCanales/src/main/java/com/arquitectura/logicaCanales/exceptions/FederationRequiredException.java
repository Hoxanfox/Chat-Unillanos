package com.arquitectura.logicaCanales.exceptions;

import java.util.UUID;

/**
 * Excepción que indica que se necesita federación P2P para completar una operación.
 * Esta excepción contiene información sobre qué peer debe manejar la petición.
 */
public class FederationRequiredException extends Exception {

    private final UUID targetPeerId;
    private final UUID user1Id;
    private final UUID user2Id;
    private final String action;

    public FederationRequiredException(String message, UUID targetPeerId, UUID user1Id, UUID user2Id, String action) {
        super(message);
        this.targetPeerId = targetPeerId;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.action = action;
    }

    public UUID getTargetPeerId() {
        return targetPeerId;
    }

    public UUID getUser1Id() {
        return user1Id;
    }

    public UUID getUser2Id() {
        return user2Id;
    }

    public String getAction() {
        return action;
    }
}

