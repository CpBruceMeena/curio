package main

import (
	"fmt"
	"log"

	"github.com/curio/backend/config"
	"github.com/curio/backend/database"
	"github.com/curio/backend/router"
)

func main() {
	cfg := config.Load()

	// Connect to database
	database.Connect(cfg)

	// Run migrations
	database.Migrate()

	// Set up router
	r := router.Setup()

	addr := fmt.Sprintf(":%s", cfg.ServerPort)
	log.Printf("Curio backend starting on %s", addr)
	if err := r.Run(addr); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}
