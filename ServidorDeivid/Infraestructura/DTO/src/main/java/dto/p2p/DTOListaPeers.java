package dto.p2p;

import java.util.List;

/**
 * DTO contenedor para enviar una lista de peers.
 */
public class DTOListaPeers {
    private List<DTOPeerDetails> listaPeers;

    public DTOListaPeers(List<DTOPeerDetails> listaPeers) {
        this.listaPeers = listaPeers;
    }

    public List<DTOPeerDetails> getListaPeers() {
        return listaPeers;
    }
}
