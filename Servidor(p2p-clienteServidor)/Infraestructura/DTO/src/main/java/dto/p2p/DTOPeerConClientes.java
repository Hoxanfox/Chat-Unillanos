package dto.p2p;

import dto.cliente.DTOSesionCliente;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO Enriquecido que combina información del Peer con sus clientes conectados.
 * Útil para mostrar en la interfaz gráfica la topología completa de la red.
 */
public class DTOPeerConClientes {
    private DTOPeerDetails peer;
    private List<DTOSesionCliente> clientesConectados;
    private int numeroClientes;

    public DTOPeerConClientes(DTOPeerDetails peer) {
        this.peer = peer;
        this.clientesConectados = new ArrayList<>();
        this.numeroClientes = 0;
    }

    public DTOPeerConClientes(DTOPeerDetails peer, List<DTOSesionCliente> clientes) {
        this.peer = peer;
        this.clientesConectados = clientes != null ? new ArrayList<>(clientes) : new ArrayList<>();
        this.numeroClientes = this.clientesConectados.size();
    }

    // Getters
    public DTOPeerDetails getPeer() {
        return peer;
    }

    public List<DTOSesionCliente> getClientesConectados() {
        return new ArrayList<>(clientesConectados);
    }

    public int getNumeroClientes() {
        return numeroClientes;
    }

    // Setters
    public void setClientesConectados(List<DTOSesionCliente> clientes) {
        this.clientesConectados = clientes != null ? new ArrayList<>(clientes) : new ArrayList<>();
        this.numeroClientes = this.clientesConectados.size();
    }

    public void agregarCliente(DTOSesionCliente cliente) {
        if (cliente != null && !clientesConectados.contains(cliente)) {
            this.clientesConectados.add(cliente);
            this.numeroClientes++;
        }
    }

    // Métodos de utilidad para la UI
    public boolean esLocal() {
        return "LOCAL".equalsIgnoreCase(peer.getId());
    }

    public String getIdPeer() {
        return peer.getId();
    }

    public String getIpPeer() {
        return peer.getIp();
    }

    public int getPuertoPeer() {
        return peer.getPuerto();
    }

    public String getEstadoPeer() {
        return peer.getEstado();
    }

    @Override
    public String toString() {
        return String.format("Peer[%s - %s:%d] con %d clientes conectados",
            peer.getId(), peer.getIp(), peer.getPuerto(), numeroClientes);
    }
}

