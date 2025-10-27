package com.arquitectura.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("TEXT") // Valor que se guardará en la columna 'message_type'
public class TextMessage extends Message {
    private String content;

    public TextMessage() {
        super();
    }

    // Constructor actualizado
    public TextMessage(User author, Channel channel, String content) {
        super(author, channel);
        this.content = content;
    }
    
    // ... getters y setters no cambian ...
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}