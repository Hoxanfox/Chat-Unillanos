package com.arquitectura.events;

import org.springframework.context.ApplicationEvent;

public class ForceDisconnectEvent extends ApplicationEvent {

    private final int userIdToDisconnect;

    public ForceDisconnectEvent(Object source, int userIdToDisconnect) {
        super(source);
        this.userIdToDisconnect = userIdToDisconnect;
    }

    public int getUserIdToDisconnect() {
        return userIdToDisconnect;
    }
}