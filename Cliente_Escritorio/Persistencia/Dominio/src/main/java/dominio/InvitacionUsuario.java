package dominio;

import java.util.UUID;

/**
 * Entidad de Dominio: Invitación Usuario
 * Relación entre un usuario y una invitación.
 */
public class InvitacionUsuario {
    private UUID idInvitacionUsuario;
    private UUID idUsuario;
    private UUID idInvitacion;

    public InvitacionUsuario() {
    }

    public InvitacionUsuario(UUID idInvitacionUsuario, UUID idUsuario, UUID idInvitacion) {
        this.idInvitacionUsuario = idInvitacionUsuario;
        this.idUsuario = idUsuario;
        this.idInvitacion = idInvitacion;
    }

    // Getters y Setters
    public UUID getIdInvitacionUsuario() {
        return idInvitacionUsuario;
    }

    public void setIdInvitacionUsuario(UUID idInvitacionUsuario) {
        this.idInvitacionUsuario = idInvitacionUsuario;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(UUID idUsuario) {
        this.idUsuario = idUsuario;
    }

    public UUID getIdInvitacion() {
        return idInvitacion;
    }

    public void setIdInvitacion(UUID idInvitacion) {
        this.idInvitacion = idInvitacion;
    }

    @Override
    public String toString() {
        return "InvitacionUsuario{" +
                "idInvitacionUsuario=" + idInvitacionUsuario +
                ", idUsuario=" + idUsuario +
                ", idInvitacion=" + idInvitacion +
                '}';
    }
}

