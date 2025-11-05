package com.unillanos.server.dto;

/**
 * DTO que contiene información del estado de un mensaje.
 * Incluye fechas de envío, entrega y lectura.
 */
public class DTOEstadoMensaje {
    
    private String mensajeId;
    private String estado; // ENVIADO, ENTREGADO, LEIDO
    private String fechaEnvio;
    private String fechaEntrega;
    private String fechaLectura;

    // Constructor por defecto
    public DTOEstadoMensaje() {}

    // Constructor con parámetros
    public DTOEstadoMensaje(String mensajeId, String estado, String fechaEnvio, 
                           String fechaEntrega, String fechaLectura) {
        this.mensajeId = mensajeId;
        this.estado = estado;
        this.fechaEnvio = fechaEnvio;
        this.fechaEntrega = fechaEntrega;
        this.fechaLectura = fechaLectura;
    }

    // Getters y Setters
    public String getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(String mensajeId) {
        this.mensajeId = mensajeId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(String fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public String getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(String fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public String getFechaLectura() {
        return fechaLectura;
    }

    public void setFechaLectura(String fechaLectura) {
        this.fechaLectura = fechaLectura;
    }

    @Override
    public String toString() {
        return "DTOEstadoMensaje{" +
                "mensajeId='" + mensajeId + '\'' +
                ", estado='" + estado + '\'' +
                ", fechaEnvio='" + fechaEnvio + '\'' +
                ", fechaEntrega='" + fechaEntrega + '\'' +
                ", fechaLectura='" + fechaLectura + '\'' +
                '}';
    }
}
