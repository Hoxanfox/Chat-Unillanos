package com.arquitectura.events;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class ForceDisconnectEvent extends ApplicationEvent {

    private final UUID userIdToDisconnect;

    public ForceDisconnectEvent(Object source, UUID userIdToDisconnect) {
        super(source);
        this.userIdToDisconnect = userIdToDisconnect;
    }

    public UUID getUserIdToDisconnect() {
        return userIdToDisconnect;
    }
}