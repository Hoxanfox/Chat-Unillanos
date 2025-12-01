package service

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"sync"
	"time"

	"github.com/Hoxanfox/Chat-Unillanos/ApiGateway/internal/models"
)

type AggregatorService struct{}

func NewAggregatorService() *AggregatorService {
	return &AggregatorService{}
}

func (s *AggregatorService) AggregateData(path string) []models.PeerResponse {
	// 1. Obtener la lista de peers
	peers, err := s.getPeers()
	if err != nil {
		fmt.Printf("Error obteniendo peers: %v. Intentando solo con Seed Peer.\n", err)
		peers = []models.Peer{}
	}

	// Filtrar peers OFFLINE
	var activePeers []models.Peer
	for _, p := range peers {
		if p.Estado == "ONLINE" {
			activePeers = append(activePeers, p)
		}
	}

	// Obtener puerto de peers desde variable de entorno
	peerPortStr := os.Getenv("PEER_API_PORT")
	peerPort := 7000
	if peerPortStr != "" {
		fmt.Sscanf(peerPortStr, "%d", &peerPort)
	}

	// Eliminamos la adición manual del Seed Peer para evitar duplicados si ya está en la lista.
	// Confiamos en la lista devuelta por el Seed Peer.

	fmt.Printf("Iniciando recolección de datos de %d peers activos para path: %s\n", len(activePeers), path)
	var wg sync.WaitGroup
	resultsChannel := make(chan []models.PeerResponse, len(activePeers))

	for _, peer := range activePeers {
		wg.Add(1)
		go func(p models.Peer) {
			defer wg.Done()
			peerData, err := s.fetchPeerData(p, path)
			if err != nil {
				fmt.Printf("Error obteniendo datos de %s (%s): %v\n", p.ID, p.IP, err)
				return
			}
			fmt.Printf("Éxito obteniendo datos de %s (%s): %d entradas\n", p.ID, p.IP, len(peerData))
			resultsChannel <- peerData
		}(peer)
	}

	go func() {
		wg.Wait()
		close(resultsChannel)
	}()

	var allData []models.PeerResponse
	for peerData := range resultsChannel {
		allData = append(allData, peerData...)
	}

	return allData
}

func (s *AggregatorService) getPeers() ([]models.Peer, error) {
	seedURL := os.Getenv("SEED_PEER_URL")
	if seedURL == "" {
		seedURL = "http://host.docker.internal:7000"
	}

	client := &http.Client{Timeout: 2 * time.Second}
	resp, err := client.Get(seedURL + "/api/network/peers")
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("seed peer respondió con estado: %d", resp.StatusCode)
	}

	var peers []models.Peer
	if err := json.NewDecoder(resp.Body).Decode(&peers); err != nil {
		return nil, err
	}

	return peers, nil
}

func (s *AggregatorService) fetchPeerData(peer models.Peer, path string) ([]models.PeerResponse, error) {
	var url string

	// Obtener puerto de peers desde variable de entorno
	peerPortStr := os.Getenv("PEER_API_PORT")
	if peerPortStr == "" {
		peerPortStr = "7000"
	}

	targetIP := peer.IP
	// Fix para Docker: solo si es localhost explícito
	if targetIP == "localhost" || targetIP == "127.0.0.1" || targetIP == "::1" {
		targetIP = "host.docker.internal"
		fmt.Printf("Redirigiendo %s a %s para acceso desde Docker\n", peer.IP, targetIP)
	}

	url = fmt.Sprintf("http://%s:%s%s", targetIP, peerPortStr, path)
	fmt.Printf("Consultando en: %s\n", url)

	client := &http.Client{
		Timeout: 2 * time.Second,
	}

	resp, err := client.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("status %d", resp.StatusCode)
	}

	var rawData interface{}
	if err := json.NewDecoder(resp.Body).Decode(&rawData); err != nil {
		return nil, err
	}

	var entries []models.PeerResponse

	if list, ok := rawData.([]interface{}); ok {
		for _, l := range list {
			entries = append(entries, models.PeerResponse{
				PeerID:    peer.ID,
				PeerIP:    peer.IP,
				Contenido: l,
			})
		}
	} else {
		entries = append(entries, models.PeerResponse{
			PeerID:    peer.ID,
			PeerIP:    peer.IP,
			Contenido: rawData,
		})
	}

	return entries, nil
}
