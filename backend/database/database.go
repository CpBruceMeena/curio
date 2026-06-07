package database

import (
	"fmt"
	"log"

	"github.com/curio/backend/config"
	"github.com/curio/backend/models"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

var DB *gorm.DB

func Connect(cfg *config.Config) {
	var err error
	DB, err = gorm.Open(postgres.Open(cfg.DatabaseURL), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Warn),
	})
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	fmt.Println("Connected to PostgreSQL")
}

func Migrate() {
	err := DB.AutoMigrate(
		&models.Category{},
		&models.Content{},
	)
	if err != nil {
		log.Fatalf("Failed to migrate database: %v", err)
	}
	fmt.Println("Database migrated successfully")
}
