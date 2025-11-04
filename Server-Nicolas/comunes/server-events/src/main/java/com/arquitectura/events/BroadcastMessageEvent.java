package com.arquitectura.events;

import org.springframework.context.ApplicationEvent;

public class BroadcastMessageEvent extends ApplicationEvent {

    private final String formattedMessage;

    /**
     * Crea un nuevo evento de mensaje de broadcast.
     * @param source El objeto que originó el evento (usualmente 'this').
     * @param formattedMessage El mensaje ya formateado para ser enviado a los clientes.
     */
    public BroadcastMessageEvent(Object source, String formattedMessage) {
        super(source);
        this.formattedMessage = formattedMessage;
    }

    public String getFormattedMessage() {
        return formattedMessage;
    }
}