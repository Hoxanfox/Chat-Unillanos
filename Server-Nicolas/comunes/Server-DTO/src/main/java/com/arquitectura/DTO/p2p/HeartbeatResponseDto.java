package com.arquitectura.DTO.p2p;

/**
 * DTO de respuesta para un heartbeat.
 * Indica al peer cuándo debe enviar el próximo heartbeat.
 */
public class HeartbeatResponseDto {
    
    private long proximoLatidoMs; // Milisegundos hasta el próximo heartbeat
    private String mensaje;

    // Constructores
    public HeartbeatResponseDto() {
    }

    public HeartbeatResponseDto(long proximoLatidoMs) {
        this.proximoLatidoMs = proximoLatidoMs;
        this.mensaje = "Latido recibido correctamente";
    }

    public HeartbeatResponseDto(long proximoLatidoMs, String mensaje) {
        this.proximoLatidoMs = proximoLatidoMs;
        this.mensaje = mensaje;
    }

    // Getters y Setters
    public long getProximoLatidoMs() {
        return proximoLatidoMs;
    }

    public void setProximoLatidoMs(long proximoLatidoMs) {
        this.proximoLatidoMs = proximoLatidoMs;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    @Override
    public String toString() {
        return "HeartbeatResponseDto{" +
                "proximoLatidoMs=" + proximoLatidoMs +
                ", mensaje='" + mensaje + '\'' +
                '}';
    }
}
