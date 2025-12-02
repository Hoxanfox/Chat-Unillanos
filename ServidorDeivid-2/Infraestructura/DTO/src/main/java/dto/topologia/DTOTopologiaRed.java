package dto.topologia;

import dto.cliente.DTOSesionCliente;
import dto.p2p.DTOPeerDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO que representa la topología completa de un peer en la red.
 * Incluye información del peer y sus clientes conectados.
 * Se envía a través de la red P2P para sincronizar la vista global.
 */
public class DTOTopologiaRed implements Serializable {
    private static final long serialVersionUID = 1L;

    // Información del peer que envía
    private String idPeer;
    private String ipPeer;
    private int puertoPeer;
    private String estadoPeer;
    private long timestamp; // Momento de creación de la topología

    // Lista de clientes conectados a este peer
    private List<DTOSesionCliente> clientesConectados;

    // Metadatos
    private int numeroClientes;

    public DTOTopologiaRed() {
        this.clientesConectados = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    public DTOTopologiaRed(String idPeer, String ipPeer, int puertoPeer, String estadoPeer) {
        this.idPeer = idPeer;
        this.ipPeer = ipPeer;
        this.puertoPeer = puertoPeer;
        this.estadoPeer = estadoPeer;
        this.clientesConectados = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.numeroClientes = 0;
    }

    /**
     * Crea una topología a partir de un peer
     */
    public static DTOTopologiaRed desdeDetallesPeer(DTOPeerDetails peer) {
        return new DTOTopologiaRed(
            peer.getId(),
            peer.getIp(),
            peer.getPuerto(),
            peer.getEstado()
        );
    }

    // Getters y Setters
    public String getIdPeer() {
        return idPeer;
    }

    public void setIdPeer(String idPeer) {
        this.idPeer = idPeer;
    }

    public String getIpPeer() {
        return ipPeer;
    }

    public void setIpPeer(String ipPeer) {
        this.ipPeer = ipPeer;
    }

    public int getPuertoPeer() {
        return puertoPeer;
    }

    public void setPuertoPeer(int puertoPeer) {
        this.puertoPeer = puertoPeer;
    }

    public String getEstadoPeer() {
        return estadoPeer;
    }

    public void setEstadoPeer(String estadoPeer) {
        this.estadoPeer = estadoPeer;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<DTOSesionCliente> getClientesConectados() {
        return new ArrayList<>(clientesConectados);
    }

    public void setClientesConectados(List<DTOSesionCliente> clientes) {
        this.clientesConectados = clientes != null ? new ArrayList<>(clientes) : new ArrayList<>();
        this.numeroClientes = this.clientesConectados.size();
    }

    public void agregarCliente(DTOSesionCliente cliente) {
        if (cliente != null) {
            this.clientesConectados.add(cliente);
            this.numeroClientes = this.clientesConectados.size();
        }
    }

    public int getNumeroClientes() {
        return numeroClientes;
    }

    @Override
    public String toString() {
        return String.format("TopologiaRed[Peer=%s, IP=%s:%d, Estado=%s, Clientes=%d, Timestamp=%d]",
            idPeer, ipPeer, puertoPeer, estadoPeer, numeroClientes, timestamp);
    }
}

