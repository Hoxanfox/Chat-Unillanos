package dominio;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de Dominio: Invitación
 * Representa una invitación a un canal.
 */
public class Invitacion {
    private UUID idInvitacion;
    private boolean estado;
    private LocalDateTime fechaEnvio;

    public Invitacion() {
    }

    public Invitacion(UUID idInvitacion, boolean estado, LocalDateTime fechaEnvio) {
        this.idInvitacion = idInvitacion;
        this.estado = estado;
        this.fechaEnvio = fechaEnvio;
    }

    // Getters y Setters
    public UUID getIdInvitacion() {
        return idInvitacion;
    }

    public void setIdInvitacion(UUID idInvitacion) {
        this.idInvitacion = idInvitacion;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    @Override
    public String toString() {
        return "Invitacion{" +
                "idInvitacion=" + idInvitacion +
                ", estado=" + estado +
                ", fechaEnvio=" + fechaEnvio +
                '}';
    }
}

