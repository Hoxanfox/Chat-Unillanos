package dominio;

import java.util.UUID;

/**
 * Entidad de Dominio: Canal Invitación
 * Relación entre un canal y una invitación.
 */
public class CanalInvitacion {
    private UUID idCanalInvitacion;
    private UUID idCanal;
    private UUID idInvitacion;

    public CanalInvitacion() {
    }

    public CanalInvitacion(UUID idCanalInvitacion, UUID idCanal, UUID idInvitacion) {
        this.idCanalInvitacion = idCanalInvitacion;
        this.idCanal = idCanal;
        this.idInvitacion = idInvitacion;
    }

    // Getters y Setters
    public UUID getIdCanalInvitacion() {
        return idCanalInvitacion;
    }

    public void setIdCanalInvitacion(UUID idCanalInvitacion) {
        this.idCanalInvitacion = idCanalInvitacion;
    }

    public UUID getIdCanal() {
        return idCanal;
    }

    public void setIdCanal(UUID idCanal) {
        this.idCanal = idCanal;
    }

    public UUID getIdInvitacion() {
        return idInvitacion;
    }

    public void setIdInvitacion(UUID idInvitacion) {
        this.idInvitacion = idInvitacion;
    }

    @Override
    public String toString() {
        return "CanalInvitacion{" +
                "idCanalInvitacion=" + idCanalInvitacion +
                ", idCanal=" + idCanal +
                ", idInvitacion=" + idInvitacion +
                '}';
    }
}

