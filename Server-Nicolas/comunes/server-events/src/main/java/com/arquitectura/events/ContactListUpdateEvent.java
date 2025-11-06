package com.arquitectura.events;

import org.springframework.context.ApplicationEvent;

/**
 * Evento disparado cuando la lista de contactos debe actualizarse
 * (por ejemplo, cuando un usuario se conecta o desconecta)
 */
public class ContactListUpdateEvent extends ApplicationEvent {
    
    public ContactListUpdateEvent(Object source) {
        super(source);
    }
}

