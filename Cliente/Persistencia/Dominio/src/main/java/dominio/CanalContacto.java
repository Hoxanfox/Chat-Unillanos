package dominio;

import java.util.UUID;

/**
 * Entidad de Dominio: Canal Contacto
 * Relación entre un canal y un contacto.
 */
public class CanalContacto {
    private UUID idCanalContacto;
    private UUID idCanal;
    private UUID idContacto;

    public CanalContacto() {
    }

    public CanalContacto(UUID idCanalContacto, UUID idCanal, UUID idContacto) {
        this.idCanalContacto = idCanalContacto;
        this.idCanal = idCanal;
        this.idContacto = idContacto;
    }

    // Getters y Setters
    public UUID getIdCanalContacto() {
        return idCanalContacto;
    }

    public void setIdCanalContacto(UUID idCanalContacto) {
        this.idCanalContacto = idCanalContacto;
    }

    public UUID getIdCanal() {
        return idCanal;
    }

    public void setIdCanal(UUID idCanal) {
        this.idCanal = idCanal;
    }

    public UUID getIdContacto() {
        return idContacto;
    }

    public void setIdContacto(UUID idContacto) {
        this.idContacto = idContacto;
    }

    @Override
    public String toString() {
        return "CanalContacto{" +
                "idCanalContacto=" + idCanalContacto +
                ", idCanal=" + idCanal +
                ", idContacto=" + idContacto +
                '}';
    }
}


