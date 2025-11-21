package com.arquitectura.events;

import org.springframework.context.ApplicationEvent;

public class UserRegisteredEvent extends ApplicationEvent {
    private final String email;
    private final String username;
    private final String rawPassword;

    public UserRegisteredEvent(Object source, String email, String username, String rawPassword) {
        super(source);
        this.email = email;
        this.username = username;
        this.rawPassword = rawPassword;
    }

    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getRawPassword() { return rawPassword; }
}