package com.arquitectura.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transcripciones_audio")
public class TranscripcionAudio {

    @Id
    @Column(name = "id_mensaje")
    private Long id; // Debe ser del mismo tipo que el ID del mensaje

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



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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