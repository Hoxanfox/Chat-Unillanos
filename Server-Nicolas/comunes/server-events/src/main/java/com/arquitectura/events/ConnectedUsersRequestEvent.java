package com.arquitectura.events;

import org.springframework.context.ApplicationEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class ConnectedUsersRequestEvent extends ApplicationEvent {

    private final Set<UUID> connectedUserIds;

    public ConnectedUsersRequestEvent(Object source) {
        super(source);
        this.connectedUserIds = new HashSet<>();
    }

    public Set<UUID> getResponseContainer() {
        return connectedUserIds;
    }
}