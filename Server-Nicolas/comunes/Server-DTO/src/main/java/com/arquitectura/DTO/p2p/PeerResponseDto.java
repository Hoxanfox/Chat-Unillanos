package com.arquitectura.DTO.p2p;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta que contiene la información de un peer.
 * Se utiliza para enviar información sobre peers al cliente o a otros peers.
 */
public class PeerResponseDto {
    
    private UUID peerId;
    private String ip;
    private int puerto;
    private String conectado; // "ONLINE", "OFFLINE", "DESCONOCIDO"
    private LocalDateTime ultimoLatido;
    private String nombreServidor;

    // Constructores
    public PeerResponseDto() {
    }

    public PeerResponseDto(UUID peerId, String ip, int puerto, String conectado) {
        this.peerId = peerId;
        this.ip = ip;
        this.puerto = puerto;
        this.conectado = conectado;
    }

    public PeerResponseDto(UUID peerId, String ip, int puerto, String conectado, 
                          LocalDateTime ultimoLatido, String nombreServidor) {
        this.peerId = peerId;
        this.ip = ip;
        this.puerto = puerto;
        this.conectado = conectado;
        this.ultimoLatido = ultimoLatido;
        this.nombreServidor = nombreServidor;
    }

    // Getters y Setters
    public UUID getPeerId() {
        return peerId;
    }

    public void setPeerId(UUID peerId) {
        this.peerId = peerId;
    }

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

    public String getConectado() {
        return conectado;
    }

    public void setConectado(String conectado) {
        this.conectado = conectado;
    }

    public LocalDateTime getUltimoLatido() {
        return ultimoLatido;
    }

    public void setUltimoLatido(LocalDateTime ultimoLatido) {
        this.ultimoLatido = ultimoLatido;
    }

    public String getNombreServidor() {
        return nombreServidor;
    }

    public void setNombreServidor(String nombreServidor) {
        this.nombreServidor = nombreServidor;
    }

    @Override
    public String toString() {
        return "PeerResponseDto{" +
                "peerId=" + peerId +
                ", ip='" + ip + '\'' +
                ", puerto=" + puerto +
                ", conectado='" + conectado + '\'' +
                ", ultimoLatido=" + ultimoLatido +
                ", nombreServidor='" + nombreServidor + '\'' +
                '}';
    }
}
