package main

import (
	"fmt"
	"log"

	"github.com/curio/backend/config"
	"github.com/curio/backend/data"
	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
)

func main() {
	cfg := config.Load()
	database.Connect(cfg)
	database.Migrate()

	// Seed categories
	fmt.Println("Seeding categories...")
	for _, cat := range data.Categories {
		var existing models.Category
		result := database.DB.Where("name = ?", cat.Name).First(&existing)
		if result.Error != nil {
			database.DB.Create(&cat)
			fmt.Printf("  Created category: %s\n", cat.Name)
		} else {
			fmt.Printf("  Category exists: %s\n", cat.Name)
		}
	}

	// Seed content
	fmt.Println("Seeding content...")
	seeded := 0
	for _, content := range data.SeedContent {
		var existing models.Content
		result := database.DB.Where("title = ?", content.Title).First(&existing)
		if result.Error != nil {
			database.DB.Create(&content)
			seeded++
		}
	}
	fmt.Printf("Seeded %d new content items\n", seeded)
	log.Println("Seed complete!")
}
