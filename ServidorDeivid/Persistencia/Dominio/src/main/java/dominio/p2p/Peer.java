package dominio.p2p;

import java.io.Serializable;
import java.net.Socket;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Peer implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String ip;
    private transient Socket socket; // no se serializa

    public enum Estado { OFFLINE, ONLINE }

    private Estado estado = Estado.OFFLINE;
    private Instant fechaCreacion;

    public Peer() {
        this.id = UUID.randomUUID();
        this.fechaCreacion = Instant.now();
        this.estado = Estado.OFFLINE;
    }

    public Peer(UUID id, String ip, Socket socket, Estado estado, Instant fechaCreacion) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.ip = ip;
        this.socket = socket;
        this.estado = estado == null ? Estado.OFFLINE : estado;
        this.fechaCreacion = fechaCreacion == null ? Instant.now() : fechaCreacion;
    }

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

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return Objects.equals(id, peer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "id=" + id +
                ", ip='" + ip + '\'' +
                ", estado=" + estado +
                '}';
    }
}
