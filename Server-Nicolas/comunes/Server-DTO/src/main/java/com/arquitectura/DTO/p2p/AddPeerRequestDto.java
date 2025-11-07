package com.arquitectura.DTO.p2p;

/**
 * DTO para la solicitud de agregar un nuevo peer a la red P2P.
 * Contiene la información necesaria para identificar y conectar con el peer.
 */
public class AddPeerRequestDto {
    
    private String ip;
    private int puerto;
    private String nombreServidor; // Opcional

    // Constructores
    public AddPeerRequestDto() {
    }

    public AddPeerRequestDto(String ip, int puerto) {
        this.ip = ip;
        this.puerto = puerto;
    }

    public AddPeerRequestDto(String ip, int puerto, String nombreServidor) {
        this.ip = ip;
        this.puerto = puerto;
        this.nombreServidor = nombreServidor;
    }

    // Getters y Setters
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPuerto() {
        return puerto;
    }

    public void setPuerto(int puerto) {
        this.puerto = puerto;
    }

    public String getNombreServidor() {
        return nombreServidor;
    }

    public void setNombreServidor(String nombreServidor) {
        this.nombreServidor = nombreServidor;
    }

    @Override
    public String toString() {
        return "AddPeerRequestDto{" +
                "ip='" + ip + '\'' +
                ", puerto=" + puerto +
                ", nombreServidor='" + nombreServidor + '\'' +
                '}';
    }
}
