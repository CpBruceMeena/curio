package models

import (
	"time"
	"gorm.io/gorm"
)

// Novel represents a long-form book (public domain / uploaded EPUB/PDF).
type Novel struct {
	ID            uint           `json:"id" gorm:"primaryKey"`
	Title         string         `json:"title" gorm:"not null"`
	Author        string         `json:"author" gorm:"not null;default:''"`
	CoverImageURL string         `json:"cover_image_url" gorm:"default:''"`
	Description   string         `json:"description" gorm:"type:text;default:''"`
	Source        string         `json:"source" gorm:"default:'gutenberg'"` // gutenberg | epub | pdf | upload
	SourceURL     string         `json:"source_url" gorm:"default:''"`
	TotalChapters int            `json:"total_chapters" gorm:"not null;default:0"`
	Language      string         `json:"language" gorm:"default:'en'"`
	CategoryID    uint           `json:"category_id"`
	Likes         int            `json:"likes" gorm:"default:0"`
	CreatedAt     time.Time      `json:"created_at"`
	DeletedAt     gorm.DeletedAt `json:"-" gorm:"index"`
}

// NovelChapter stores one chapter of a novel, pre-split and cleaned.
type NovelChapter struct {
	ID            uint      `json:"id" gorm:"primaryKey"`
	NovelID       uint      `json:"novel_id" gorm:"not null;index"`
	ChapterNumber int       `json:"chapter_number" gorm:"not null"`
	Title         string    `json:"title" gorm:"not null;default:''"`
	Body          string    `json:"body" gorm:"type:text;not null"`
	ReadTimeSecs  int       `json:"read_time_secs" gorm:"not null;default:0"`
	CreatedAt     time.Time `json:"created_at"`
}

// UserNovelProgress tracks a user's reading progress per novel (device-based).
type UserNovelProgress struct {
	ID           uint      `json:"id" gorm:"primaryKey"`
	DeviceID     string    `json:"device_id" gorm:"index;not null"`
	NovelID      uint      `json:"novel_id" gorm:"not null"`
	LastChapter  int       `json:"last_chapter" gorm:"not null;default:1"`
	LastPosition int       `json:"last_position" gorm:"not null;default:0"`
	Completed    bool      `json:"completed" gorm:"not null;default:false"`
	Bookmarked   bool      `json:"bookmarked" gorm:"not null;default:false"`
	UpdatedAt    time.Time `json:"updated_at"`
}

// ── Response Types ─────────────────────────────────────────────

type NovelsListResponse struct {
	Novels []Novel `json:"novels"`
	Total  int64   `json:"total"`
	Page   int     `json:"page"`
	Limit  int     `json:"limit"`
}

type NovelDetailResponse struct {
	Novel    Novel         `json:"novel"`
	Chapters []NovelChapter `json:"chapters"`
	Progress *UserNovelProgress `json:"progress,omitempty"`
}

type ProgressUpdateRequest struct {
	DeviceID     string `json:"device_id" binding:"required"`
	LastChapter  int    `json:"last_chapter"`
	LastPosition int    `json:"last_position"`
	Completed    *bool  `json:"completed,omitempty"`
	Bookmarked   *bool  `json:"bookmarked,omitempty"`
}
