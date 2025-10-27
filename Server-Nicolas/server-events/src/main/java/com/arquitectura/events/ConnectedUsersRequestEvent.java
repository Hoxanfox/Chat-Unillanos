package com.arquitectura.events;

import org.springframework.context.ApplicationEvent;
import java.util.HashSet;
import java.util.Set;


public class ConnectedUsersRequestEvent extends ApplicationEvent {

    private final Set<Integer> connectedUserIds;

    public ConnectedUsersRequestEvent(Object source) {
        super(source);
        this.connectedUserIds = new HashSet<>();
    }

    public Set<Integer> getResponseContainer() {
        return connectedUserIds;
    }
}