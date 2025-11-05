package com.arquitectura.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("AUDIO")
public class AudioMessage extends Message {
    private String audioUrl;

    public AudioMessage() {
        super();
    }

    // Constructor actualizado
    public AudioMessage(User author, Channel channel, String audioUrl) {
        super(author, channel);
        this.audioUrl = audioUrl;
    }
    
    // ... getters y setters no cambian ...
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
}