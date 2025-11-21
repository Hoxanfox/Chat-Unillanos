package com.arquitectura.logicaUsuarios.listeners;

import com.arquitectura.events.UserRegisteredEvent;
import com.arquitectura.utils.mail.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class UserEmailListener {

    private final EmailService emailService;

    @Autowired
    public UserEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async // Ejecutar en hilo separado para no bloquear
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        emailService.enviarCredenciales(event.getEmail(), event.getUsername(), event.getRawPassword());
    }
}