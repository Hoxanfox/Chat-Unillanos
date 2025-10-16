package com.unillanos.server.dto.response;

import java.util.List;

/**
 * DTO para la respuesta de historial de canal.
 * Estructura que espera recibir el cliente.
 */
public class DTOResponseHistorialCanal {
    private List<DTOMensajeCanalResponse> mensajes;
    private boolean hayMasMensajes;
    private int totalMensajes;
    private String canalId;
    private String nombreCanal;

    public DTOResponseHistorialCanal() {}

    public DTOResponseHistorialCanal(List<DTOMensajeCanalResponse> mensajes, boolean hayMasMensajes, 
                                   int totalMensajes, String canalId, String nombreCanal) {
        this.mensajes = mensajes;
        this.hayMasMensajes = hayMasMensajes;
        this.totalMensajes = totalMensajes;
        this.canalId = canalId;
        this.nombreCanal = nombreCanal;
    }

    // Getters y Setters
    public List<DTOMensajeCanalResponse> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<DTOMensajeCanalResponse> mensajes) {
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

    public String getCanalId() {
        return canalId;
    }

    public void setCanalId(String canalId) {
        this.canalId = canalId;
    }

    public String getNombreCanal() {
        return nombreCanal;
    }

    public void setNombreCanal(String nombreCanal) {
        this.nombreCanal = nombreCanal;
    }
}
