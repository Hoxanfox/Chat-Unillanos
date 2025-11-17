package dto.p2p;

import java.util.List;

/**
 * DTO que representa la respuesta a una petición de lista de peers.
 */
public final class DTOPeerListResponse {
    private String requestId;
    private List<DTOPeer> peers;
    private Integer count;

    public DTOPeerListResponse() {}

    public String getRequestId() { return requestId; }
    public List<DTOPeer> getPeers() { return peers; }
    public Integer getCount() { return count; }

    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setPeers(List<DTOPeer> peers) { this.peers = peers; }
    public void setCount(Integer count) { this.count = count; }
}

