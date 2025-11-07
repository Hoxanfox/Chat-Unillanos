package com.arquitectura.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para transferir información de peers a la capa de presentación (GUI).
 * Este DTO es específico para el monitoreo y visualización en la interfaz gráfica.
 */
public class PeerDTO {
    
    private UUID id;
    private String ip;
    private int puerto;
    private String conectado; // ONLINE, OFFLINE, DESCONOCIDO
    private LocalDateTime ultimoLatido;
    private String nombreServidor;

    // Constructores
    
    public PeerDTO() {
    }

    public PeerDTO(UUID id, String ip, int puerto, String conectado, 
                   LocalDateTime ultimoLatido, String nombreServidor) {
        this.id = id;
        this.ip = ip;
        this.puerto = puerto;
        this.conectado = conectado;
        this.ultimoLatido = ultimoLatido;
        this.nombreServidor = nombreServidor;
    }

    // Getters y Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
        return "PeerDTO{" +
                "id=" + id +
                ", ip='" + ip + '\'' +
                ", puerto=" + puerto +
                ", conectado=" + conectado +
                ", ultimoLatido=" + ultimoLatido +
                ", nombreServidor='" + nombreServidor + '\'' +
                '}';
    }
}
