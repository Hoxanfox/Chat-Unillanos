package com.arquitectura.DTO.peers;

/**
 * DTO para la respuesta de una petición retransmitida
 */
public class RetransmitResponseDto {
    private Object respuestaCliente; // La respuesta que generó el procesamiento de la petición del cliente

    public RetransmitResponseDto() {
    }

    public RetransmitResponseDto(Object respuestaCliente) {
        this.respuestaCliente = respuestaCliente;
    }

    public Object getRespuestaCliente() {
        return respuestaCliente;
    }

    public void setRespuestaCliente(Object respuestaCliente) {
        this.respuestaCliente = respuestaCliente;
    }
}

