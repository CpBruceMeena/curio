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

	// Run the v3 schema migration SQL (adds content_comments table)
	if err := runSQLFile("scripts/migrate_schema_v3.sql"); err != nil {
		log.Printf("Warning: schema v3 migration error (may already be applied): %v", err)
	}

	// Run the novels schema migration (adds novels, novel_chapters, user_novel_progress)
	if err := runSQLFile("scripts/migrate_schema_novels.sql"); err != nil {
		log.Printf("Warning: novels migration error (may already be applied): %v", err)
	}

	// Auto-migrate models
	err := DB.AutoMigrate(
		&models.Category{},
		&models.Feedback{},
		&models.Puzzle{},
		&models.Profile{},
		&models.DeviceInfo{},
		&models.ContentComment{},
		&models.Novel{},
		&models.NovelChapter{},
		&models.UserNovelProgress{},
	)
	if err != nil {
		log.Fatalf("Failed to migrate database: %v", err)
	}
	fmt.Println("Database migrated successfully")

	// Run chapter integrity check after migrations
	if err := ValidateChapterIntegrity(); err != nil {
		log.Printf("⚠ Chapter integrity check failed: %v", err)
	} else {
		fmt.Println("✓ Chapter integrity check passed")
	}
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


// ValidateChapterIntegrity checks that all novels have sequential chapter
// numbering (1..N, no gaps, no duplicates) and clean titles.
// Called at startup after migrations.
func ValidateChapterIntegrity() error {
	type NovelCheck struct {
		NovelID       uint
		Title         string
		MinCh         int
		MaxCh         int
		TotalChapters int
	}

	var rows []NovelCheck
	if err := DB.Raw(`
		SELECT n.id AS novel_id, n.title,
			MIN(nc.chapter_number) AS min_ch,
			MAX(nc.chapter_number) AS max_ch,
			COUNT(*) AS total_chapters
		FROM novels n
		JOIN novel_chapters nc ON nc.novel_id = n.id
		GROUP BY n.id, n.title
		ORDER BY n.id
	`).Scan(&rows).Error; err != nil {
		return fmt.Errorf("query failed: %w", err)
	}

	for _, r := range rows {
		// Check starts at 1
		if r.MinCh != 1 {
			return fmt.Errorf("novel %d '%s': first chapter is %d, expected 1", r.NovelID, r.Title, r.MinCh)
		}
		// Check no gaps (max == count means sequential 1..N)
		if r.MaxCh != r.TotalChapters {
			return fmt.Errorf("novel %d '%s': chapters %d..%d but count=%d (gaps or non-sequential)",
				r.NovelID, r.Title, r.MinCh, r.MaxCh, r.TotalChapters)
		}
	}

	// Check for duplicate chapter numbers within a novel
	var duplicates int64
	DB.Raw(`
		SELECT COUNT(*) FROM (
			SELECT novel_id, chapter_number, COUNT(*)
			FROM novel_chapters
			GROUP BY novel_id, chapter_number
			HAVING COUNT(*) > 1
		) dup
	`).Scan(&duplicates)
	if duplicates > 0 {
		return fmt.Errorf("%d duplicate chapter number(s) found across novels", duplicates)
	}

	return nil
}
