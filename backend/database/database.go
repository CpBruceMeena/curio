package database

import (
	"fmt"
	"log"
	"os"
	"path/filepath"
	"runtime"

	"github.com/curio/backend/config"
	"github.com/curio/backend/models"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

var DB *gorm.DB

var (
	// __file__ is the source file path; used to locate migration scripts.
	_, __file__, _, _ = runtime.Caller(0)
)

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
	// Run the v2 schema migration SQL (adds meta JSONB, device_id, device_infos)
	if err := runSQLFile("scripts/migrate_schema_v2.sql"); err != nil {
		log.Printf("Warning: schema v2 migration error (may already be applied): %v", err)
	}

	// Auto-migrate models
	err := DB.AutoMigrate(
		&models.Category{},
		&models.Feedback{},
		&models.Puzzle{},
		&models.Profile{},
		&models.DeviceInfo{},
	)
	if err != nil {
		log.Fatalf("Failed to migrate database: %v", err)
	}
	fmt.Println("Database migrated successfully")
}

// runSQLFile reads a SQL file relative to the project root and executes it.
func runSQLFile(relPath string) error {
	// Walk up from the database package directory to find the project root.
	// __file__ is backend/database/database.go, so three levels up is the project root.
	root := filepath.Dir(filepath.Dir(filepath.Dir(__file__)))
	fullPath := filepath.Join(root, relPath)

	data, err := os.ReadFile(fullPath)
	if err != nil {
		return fmt.Errorf("read %s: %w", relPath, err)
	}

	if len(data) == 0 {
		return nil
	}

	tx := DB.Exec(string(data))
	return tx.Error
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
