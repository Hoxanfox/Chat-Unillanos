package main

import (
	"fmt"
	"log"
	"net/http"
	"os"

	"github.com/Hoxanfox/Chat-Unillanos/ApiGateway/internal/handlers"
	"github.com/Hoxanfox/Chat-Unillanos/ApiGateway/internal/service"
)

func enableCORS(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")

		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}

		next(w, r)
	}
}

func main() {
	// Initialize Service
	aggregatorService := service.NewAggregatorService()

	// Initialize Handler
	gatewayHandler := handlers.NewGatewayHandler(aggregatorService)

	// Setup Routes
	http.HandleFunc("/gateway/logs", enableCORS(gatewayHandler.GetLogs))
	http.HandleFunc("/gateway/logs/stats", enableCORS(gatewayHandler.GetStats))
	http.HandleFunc("/gateway/logs/health", enableCORS(gatewayHandler.GetHealth))

	// Start Server
	port := os.Getenv("GATEWAY_PORT")
	if port == "" {
		port = "8080"
	}
	fmt.Printf("API Gateway escuchando en el puerto %s\n", port)
	if err := http.ListenAndServe(":"+port, nil); err != nil {
		log.Fatalf("Error iniciando el servidor: %v", err)
	}
}
