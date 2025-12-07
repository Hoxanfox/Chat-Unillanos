package dto.canales;

import dto.featureContactos.DTOContacto;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO que representa una invitación a un canal.
 * Contiene toda la información necesaria para mostrar y gestionar la invitación.
 */
public class DTOInvitacionCanal implements Serializable {

    private final String invitacionId;
    private final String channelId;
    private final String channelName;
    private final String channelType;
    private final DTOContacto invitador;
    private final String estado; // "PENDIENTE", "ACEPTADA", "RECHAZADA"
    private final LocalDateTime fechaCreacion;

    public DTOInvitacionCanal(String invitacionId, String channelId, String channelName,
                              String channelType, DTOContacto invitador, String estado,
                              LocalDateTime fechaCreacion) {
        this.invitacionId = invitacionId;
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelType = channelType;
        this.invitador = invitador;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    // Getters
    public String getInvitacionId() {
        return invitacionId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getChannelType() {
        return channelType;
    }

    public DTOContacto getInvitador() {
        return invitador;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    @Override
    public String toString() {
        return "DTOInvitacionCanal{" +
                "invitacionId='" + invitacionId + '\'' +
                ", channelId='" + channelId + '\'' +
                ", channelName='" + channelName + '\'' +
                ", invitador=" + (invitador != null ? invitador.getNombre() : "null") +
                ", estado='" + estado + '\'' +
                '}';
    }
}

