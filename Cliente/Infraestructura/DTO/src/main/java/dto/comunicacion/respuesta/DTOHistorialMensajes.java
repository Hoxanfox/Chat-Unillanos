package dto.comunicacion.respuesta;

import dto.vistaContactoChat.DTOMensaje;
import java.util.List;

/**
 * DTO para la respuesta de 'solicitarHistorialPrivado'.
 * Representa la estructura completa de datos que envía el servidor.
 */
public class DTOHistorialMensajes {
    private List<DTOMensaje> mensajes;
    private boolean tieneMas;
    private int totalMensajes;
    private String contactoId;
    private String contactoNombre;

    // Constructor vacío para Gson
    public DTOHistorialMensajes() {}

    public DTOHistorialMensajes(List<DTOMensaje> mensajes, boolean tieneMas, 
                                int totalMensajes, String contactoId, String contactoNombre) {
        this.mensajes = mensajes;
        this.tieneMas = tieneMas;
        this.totalMensajes = totalMensajes;
        this.contactoId = contactoId;
        this.contactoNombre = contactoNombre;
    }

    public List<DTOMensaje> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<DTOMensaje> mensajes) {
        this.mensajes = mensajes;
    }

    public boolean isTieneMas() {
        return tieneMas;
    }

    public void setTieneMas(boolean tieneMas) {
        this.tieneMas = tieneMas;
    }

    public int getTotalMensajes() {
        return totalMensajes;
    }

    public void setTotalMensajes(int totalMensajes) {
        this.totalMensajes = totalMensajes;
    }

    public String getContactoId() {
        return contactoId;
    }

    public void setContactoId(String contactoId) {
        this.contactoId = contactoId;
    }

    public String getContactoNombre() {
        return contactoNombre;
    }

    public void setContactoNombre(String contactoNombre) {
        this.contactoNombre = contactoNombre;
    }

    @Override
    public String toString() {
        return "DTOHistorialMensajes{" +
                "totalMensajes=" + totalMensajes +
                ", tieneMas=" + tieneMas +
                ", contactoId='" + contactoId + '\'' +
                ", contactoNombre='" + contactoNombre + '\'' +
                '}';
    }
}

