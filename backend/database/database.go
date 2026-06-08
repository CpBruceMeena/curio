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
	)
	if err != nil {
		log.Fatalf("Failed to migrate database: %v", err)
	}
	fmt.Println("Database migrated successfully")
}

// ContentTableName returns the per-category content table name for a category.
// Uses ContentTableID for stable table naming (not auto-increment ID).
func ContentTableName(categoryID uint) string {
	var cat models.Category
	if err := DB.First(&cat, categoryID).Error; err != nil {
		// Fallback to category ID for backward compatibility
		return fmt.Sprintf("contents_%d", categoryID)
	}
	if cat.ContentTableID > 0 {
		return fmt.Sprintf("contents_%d", cat.ContentTableID)
	}
	return fmt.Sprintf("contents_%d", categoryID)
}

// ArchiveTableName returns the archive table name for a category.
// Uses ContentTableID for stable table naming (not auto-increment ID).
func ArchiveTableName(categoryID uint) string {
	var cat models.Category
	if err := DB.First(&cat, categoryID).Error; err != nil {
		return fmt.Sprintf("archive_%d", categoryID)
	}
	if cat.ContentTableID > 0 {
		return fmt.Sprintf("archive_%d", cat.ContentTableID)
	}
	return fmt.Sprintf("archive_%d", categoryID)
}
