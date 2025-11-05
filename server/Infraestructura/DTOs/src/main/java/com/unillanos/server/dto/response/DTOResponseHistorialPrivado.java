package com.unillanos.server.dto.response;

import java.util.List;

/**
 * DTO para la respuesta de historial privado.
 * Estructura que espera recibir el cliente.
 */
public class DTOResponseHistorialPrivado {
    private List<DTOMensajePrivadoResponse> mensajes;
    private boolean hayMasMensajes;
    private int totalMensajes;
    private String contactoId;
    private String nombreContacto;

    public DTOResponseHistorialPrivado() {}

    public DTOResponseHistorialPrivado(List<DTOMensajePrivadoResponse> mensajes, boolean hayMasMensajes, 
                                     int totalMensajes, String contactoId, String nombreContacto) {
        this.mensajes = mensajes;
        this.hayMasMensajes = hayMasMensajes;
        this.totalMensajes = totalMensajes;
        this.contactoId = contactoId;
        this.nombreContacto = nombreContacto;
    }

    // Getters y Setters
    public List<DTOMensajePrivadoResponse> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<DTOMensajePrivadoResponse> mensajes) {
        this.mensajes = mensajes;
    }

    public boolean isHayMasMensajes() {
        return hayMasMensajes;
    }

    public void setHayMasMensajes(boolean hayMasMensajes) {
        this.hayMasMensajes = hayMasMensajes;
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

    public String getNombreContacto() {
        return nombreContacto;
    }

    public void setNombreContacto(String nombreContacto) {
        this.nombreContacto = nombreContacto;
    }
}
