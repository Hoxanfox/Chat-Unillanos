package handlers

import (
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/Hoxanfox/Chat-Unillanos/ApiGateway/internal/service"
)

type GatewayHandler struct {
	service   *service.AggregatorService
	collector *service.LogCollector
}

func NewGatewayHandler(s *service.AggregatorService, c *service.LogCollector) *GatewayHandler {
	return &GatewayHandler{service: s, collector: c}
}

func (h *GatewayHandler) GetLogs(w http.ResponseWriter, r *http.Request) {
	// Use the collector to get aggregated logs from memory (SSE source)
	data := h.collector.GetLogs()
	respondJSON(w, data)
}

func (h *GatewayHandler) GetStats(w http.ResponseWriter, r *http.Request) {
	data := h.service.AggregateData("/api/logs/stats")
	respondJSON(w, data)
}

func (h *GatewayHandler) GetHealth(w http.ResponseWriter, r *http.Request) {
	data := h.service.AggregateData("/api/logs/health")
	respondJSON(w, data)
}

func (h *GatewayHandler) GetNetworkPeers(w http.ResponseWriter, r *http.Request) {
	peers, err := h.service.GetPeers()
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadGateway)
		return
	}
	respondJSON(w, peers)
}

func (h *GatewayHandler) StreamLogs(w http.ResponseWriter, r *http.Request) {
	// Set headers for SSE (Common for both modes)
	w.Header().Set("Content-Type", "text/event-stream")
	w.Header().Set("Cache-Control", "no-cache")
	w.Header().Set("Connection", "keep-alive")
	w.Header().Set("Access-Control-Allow-Origin", "*")

	flusher, ok := w.(http.Flusher)
	if !ok {
		http.Error(w, "Streaming not supported", http.StatusInternalServerError)
		return
	}

	peerIP := r.URL.Query().Get("peer")
	
	if peerIP == "" {
		// --- AGGREGATED STREAMING (FROM MEMORY) ---
		
		// 1. Send existing logs first (History) - Send Oldest first so frontend prepends correctly
		existingLogs := h.collector.GetLogs()
		for i := len(existingLogs) - 1; i >= 0; i-- {
			log := existingLogs[i]
			jsonData, err := json.Marshal(log) 
			if err == nil {
				fmt.Fprintf(w, "data: %s\n\n", jsonData)
			}
		}
		flusher.Flush()

		// 2. Subscribe to new logs
		ch := h.collector.Subscribe()
		defer h.collector.Unsubscribe(ch)

		// 3. Stream new logs
		notify := r.Context().Done()
		for {
			select {
			case <-notify:
				return
			case log := <-ch:
				jsonData, err := json.Marshal(log)
				if err == nil {
					fmt.Fprintf(w, "data: %s\n\n", jsonData)
					flusher.Flush()
				}
			}
		}

	} else {
		// --- PROXY STREAMING (DIRECT TO PEER) ---
		
		// Construct target URL
		targetURL := "http://" + peerIP + ":7000/api/logs/stream"

		req, err := http.NewRequest("GET", targetURL, nil)
		if err != nil {
			// Can't write http.Error because headers are already set? 
			// Actually we set headers at top. If this fails, we might send text/event-stream error?
			// It's better to log and return.
			fmt.Printf("Failed to create request: %v\n", err)
			return
		}

		client := &http.Client{}
		resp, err := client.Do(req)
		if err != nil {
			fmt.Printf("Failed to connect to peer: %v\n", err)
			return
		}
		defer resp.Body.Close()

		buf := make([]byte, 1024)
		for {
			n, err := resp.Body.Read(buf)
			if n > 0 {
				w.Write(buf[:n])
				flusher.Flush()
			}
			if err != nil {
				break
			}
		}
	}
}

func respondJSON(w http.ResponseWriter, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(data)
}
