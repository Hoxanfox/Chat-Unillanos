package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/Hoxanfox/Chat-Unillanos/ApiGateway/internal/service"
)

type GatewayHandler struct {
	service *service.AggregatorService
}

func NewGatewayHandler(s *service.AggregatorService) *GatewayHandler {
	return &GatewayHandler{service: s}
}

func (h *GatewayHandler) GetLogs(w http.ResponseWriter, r *http.Request) {
	data := h.service.AggregateData("/api/logs")
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

func respondJSON(w http.ResponseWriter, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(data)
}
