package com.arquitectura.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transcripciones_audio")
public class TranscripcionAudio {

    @Id
    @Column(name = "id_mensaje")
    private UUID id; // Cambiado a UUID

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "id_mensaje")
    private Message mensaje;

    @Column(name = "texto_transcrito")
    private String textoTranscrito;

    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;

    public TranscripcionAudio() {
    }

    public TranscripcionAudio( Message mensaje, String textoTranscrito) {
        this.mensaje = mensaje;
        this.textoTranscrito = textoTranscrito;
        if (mensaje != null) {
            this.id = mensaje.getIdMensaje(); // <-- ¡ESTA ES LA LÍNEA CLAVE!
        }
    }



    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Message getMensaje() {
        return mensaje;
    }

    public void setMensaje(Message mensaje) {
        this.mensaje = mensaje;
    }

    public String getTextoTranscrito() {
        return textoTranscrito;
    }

    public void setTextoTranscrito(String textoTranscrito) {
        this.textoTranscrito = textoTranscrito;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }

    @PrePersist
    protected void onProcess() {
        fechaProcesamiento = LocalDateTime.now();
    }
}