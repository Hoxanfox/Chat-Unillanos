package com.arquitectura.domain;

import com.arquitectura.domain.enums.EstadoPeer;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa un peer (servidor) en la red P2P.
 * Cada peer mantiene información sobre otros servidores conocidos.
 */
@Entity
@Table(name = "peers")
public class Peer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID peerId;

    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    @Column(name = "puerto", nullable = false)
    private int puerto;

    @Enumerated(EnumType.STRING)
    @Column(name = "conectado", nullable = false, length = 20)
    private EstadoPeer conectado;

    @Column(name = "ultimo_latido")
    private LocalDateTime ultimoLatido;

    @Column(name = "nombre_servidor", length = 100)
    private String nombreServidor;

    // Constructores
    public Peer() {
        this.conectado = EstadoPeer.DESCONOCIDO;
    }

    public Peer(String ip) {
        this.ip = ip;
        this.conectado = EstadoPeer.DESCONOCIDO;
    }

    public Peer(String ip, int puerto) {
        this.ip = ip;
        this.puerto = puerto;
        this.conectado = EstadoPeer.DESCONOCIDO;
        this.ultimoLatido = LocalDateTime.now();
    }

    public Peer(String ip, int puerto, String nombreServidor) {
        this.ip = ip;
        this.puerto = puerto;
        this.nombreServidor = nombreServidor;
        this.conectado = EstadoPeer.DESCONOCIDO;
        this.ultimoLatido = LocalDateTime.now();
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

    public EstadoPeer getConectado() {
        return conectado;
    }

    public void setConectado(EstadoPeer conectado) {
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

    // Métodos de utilidad
    
    /**
     * Marca el peer como online y actualiza el timestamp del último latido
     */
    public void marcarComoOnline() {
        this.conectado = EstadoPeer.ONLINE;
        this.ultimoLatido = LocalDateTime.now();
    }

    /**
     * Marca el peer como offline
     */
    public void marcarComoOffline() {
        this.conectado = EstadoPeer.OFFLINE;
    }

    /**
     * Actualiza el timestamp del último latido recibido
     */
    public void actualizarLatido() {
        this.ultimoLatido = LocalDateTime.now();
        if (this.conectado != EstadoPeer.ONLINE) {
            this.conectado = EstadoPeer.ONLINE;
        }
    }

    /**
     * Verifica si el peer está activo (ONLINE)
     */
    public boolean estaActivo() {
        return this.conectado == EstadoPeer.ONLINE;
    }

    /**
     * Verifica si el peer ha excedido el timeout de heartbeat
     * @param timeoutSegundos Tiempo máximo sin latido en segundos
     * @return true si ha excedido el timeout
     */
    public boolean haExcedidoTimeout(long timeoutSegundos) {
        if (ultimoLatido == null) {
            return true;
        }
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limiteTimeout = ultimoLatido.plusSeconds(timeoutSegundos);
        return ahora.isAfter(limiteTimeout);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "peerId=" + peerId +
                ", ip='" + ip + '\'' +
                ", puerto=" + puerto +
                ", conectado=" + conectado +
                ", ultimoLatido=" + ultimoLatido +
                ", nombreServidor='" + nombreServidor + '\'' +
                '}';
    }
}