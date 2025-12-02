package service

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/Hoxanfox/Chat-Unillanos/ApiGateway/internal/models"
)

type LogCollector struct {
	logs          []models.PeerResponse
	activePeers   map[string]bool
	subscribers   map[chan models.PeerResponse]bool
	mu            sync.RWMutex
	maxLogs       int
}

func NewLogCollector() *LogCollector {
	return &LogCollector{
		logs:        make([]models.PeerResponse, 0),
		activePeers: make(map[string]bool),
		subscribers: make(map[chan models.PeerResponse]bool),
		maxLogs:     1000, // Keep last 1000 logs in memory
	}
}

func (c *LogCollector) Subscribe() chan models.PeerResponse {
	c.mu.Lock()
	defer c.mu.Unlock()
	ch := make(chan models.PeerResponse, 100) // Buffer to prevent blocking
	c.subscribers[ch] = true
	return ch
}

func (c *LogCollector) Unsubscribe(ch chan models.PeerResponse) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if _, ok := c.subscribers[ch]; ok {
		delete(c.subscribers, ch)
		close(ch)
	}
}

func (c *LogCollector) GetLogs() []models.PeerResponse {
	c.mu.RLock()
	defer c.mu.RUnlock()
	// Return a copy to avoid race conditions
	result := make([]models.PeerResponse, len(c.logs))
	copy(result, c.logs)
	return result
}

func (c *LogCollector) Start() {
	go c.discoveryLoop()
}

func (c *LogCollector) discoveryLoop() {
	ticker := time.NewTicker(5 * time.Second)
	for range ticker.C {
		peers, err := c.getPeers()
		if err != nil {
			fmt.Printf("Error discovering peers: %v\n", err)
			continue
		}

		for _, peer := range peers {
			if peer.Estado == "ONLINE" {
				c.mu.Lock()
				if !c.activePeers[peer.ID] {
					c.activePeers[peer.ID] = true
					go c.connectToPeer(peer)
				}
				c.mu.Unlock()
			}
		}
	}
}

func (c *LogCollector) connectToPeer(peer models.Peer) {
	defer func() {
		c.mu.Lock()
		delete(c.activePeers, peer.ID)
		c.mu.Unlock()
		fmt.Printf("Disconnected from peer %s\n", peer.ID)
	}()

	targetIP := peer.IP
	if targetIP == "localhost" || targetIP == "127.0.0.1" || targetIP == "::1" {
		targetIP = "host.docker.internal"
	}
	
	peerPortStr := os.Getenv("PEER_API_PORT")
	if peerPortStr == "" {
		peerPortStr = "7000"
	}

	url := fmt.Sprintf("http://%s:%s/api/logs/stream", targetIP, peerPortStr)
	fmt.Printf("Connecting SSE to %s (%s)\n", peer.ID, url)

	resp, err := http.Get(url)
	if err != nil {
		fmt.Printf("Failed to connect to %s: %v\n", url, err)
		return
	}
	defer resp.Body.Close()

	scanner := bufio.NewScanner(resp.Body)
	for scanner.Scan() {
		line := scanner.Text()
		if strings.HasPrefix(line, "data:") {
			data := strings.TrimPrefix(line, "data:")
			var logEntry interface{}
			if err := json.Unmarshal([]byte(data), &logEntry); err == nil {
				// Wrap in PeerResponse
				entry := models.PeerResponse{
					PeerID:    peer.ID,
					PeerIP:    peer.IP,
					Contenido: logEntry,
				}
				c.addLog(entry)
			}
		}
	}
}

func (c *LogCollector) addLog(entry models.PeerResponse) {
	c.mu.Lock()
	defer c.mu.Unlock()
	
	// Prepend (newest first)
	c.logs = append([]models.PeerResponse{entry}, c.logs...)
	
	if len(c.logs) > c.maxLogs {
		c.logs = c.logs[:c.maxLogs]
	}

	// Broadcast to subscribers
	for ch := range c.subscribers {
		select {
		case ch <- entry:
		default:
			// Drop message if subscriber is too slow
		}
	}
}

// Reusing getPeers logic (simplified)
func (c *LogCollector) getPeers() ([]models.Peer, error) {
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
		return nil, fmt.Errorf("status %d", resp.StatusCode)
	}

	var peers []models.Peer
	if err := json.NewDecoder(resp.Body).Decode(&peers); err != nil {
		return nil, err
	}

	return peers, nil
}
