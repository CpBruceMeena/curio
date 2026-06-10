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
	// Only migrate the categories table.
	// The contents table is now a VIEW over per-category tables.
	// Run scripts/migrate_per_category.sql to set up the new structure.
	err := DB.AutoMigrate(
		&models.Category{},
		&models.Feedback{},
		&models.Puzzle{},
	)
	if err != nil {
		log.Fatalf("Failed to migrate database: %v", err)
	}
	fmt.Println("Database migrated successfully")
}

// ContentTableName returns the per-category content table name using the stable
// ContentTableID. Falls back to categoryID if ContentTableID is 0.
func ContentTableName(contentTableID, categoryID uint) string {
	if contentTableID > 0 {
		return fmt.Sprintf("contents_%d", contentTableID)
	}
	return fmt.Sprintf("contents_%d", categoryID)
}

// ArchiveTableName returns the archive table name using the stable
// ContentTableID. Falls back to categoryID if ContentTableID is 0.
func ArchiveTableName(contentTableID, categoryID uint) string {
	if contentTableID > 0 {
		return fmt.Sprintf("archive_%d", contentTableID)
	}
	return fmt.Sprintf("archive_%d", categoryID)
}
