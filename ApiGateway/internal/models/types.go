package models

// Peer represents a node in the P2P network
type Peer struct {
	ID             string `json:"id"`
	IP             string `json:"ip"`
	Puerto         int    `json:"puerto"`
	PuertoServidor int    `json:"puertoServidor"`
	Estado         string `json:"estado"`
}

// PeerResponse represents a generic response from a peer
type PeerResponse struct {
	PeerID    string      `json:"peer_id"`
	PeerIP    string      `json:"peer_ip"`
	Contenido interface{} `json:"contenido"` // Flexible content to match whatever the peer sends
}
