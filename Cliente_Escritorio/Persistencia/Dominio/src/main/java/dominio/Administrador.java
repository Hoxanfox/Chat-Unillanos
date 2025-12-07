package dominio;

import java.util.UUID;

/**
 * Entidad de Dominio: Administrador
 * Relación entre un usuario y un canal donde es administrador.
 */
public class Administrador {
    private UUID idAdministrador;
    private UUID idUsuario;
    private UUID idCanal;

    public Administrador() {
    }

    public Administrador(UUID idAdministrador, UUID idUsuario, UUID idCanal) {
        this.idAdministrador = idAdministrador;
        this.idUsuario = idUsuario;
        this.idCanal = idCanal;
    }

    // Getters y Setters
    public UUID getIdAdministrador() {
        return idAdministrador;
    }

    public void setIdAdministrador(UUID idAdministrador) {
        this.idAdministrador = idAdministrador;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(UUID idUsuario) {
        this.idUsuario = idUsuario;
    }

    public UUID getIdCanal() {
        return idCanal;
    }

    public void setIdCanal(UUID idCanal) {
        this.idCanal = idCanal;
    }

    @Override
    public String toString() {
        return "Administrador{" +
                "idAdministrador=" + idAdministrador +
                ", idUsuario=" + idUsuario +
                ", idCanal=" + idCanal +
                '}';
    }
}

