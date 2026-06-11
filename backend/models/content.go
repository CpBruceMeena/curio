package models

import (
	"encoding/json"
	"time"

	"gorm.io/gorm"
)

type Content struct {
	ID           uint      `json:"id" gorm:"primaryKey"`
	CategoryID   uint      `json:"category_id"`
	CategoryName string    `json:"category_name" gorm:"-"`
	Title        string    `json:"title" gorm:"not null"`
	Body         string    `json:"body" gorm:"type:text;not null"`
	Description  string    `json:"description" gorm:"default:''"`
	Poet         string    `json:"poet" gorm:"default:''"`
	Source       string    `json:"source"`
	SourceURL    string    `json:"source_url"`
	ReadTimeSecs int       `json:"read_time_secs" gorm:"default:15"`
	Tags         string    `json:"tags" gorm:"type:text"`
	Likes        int       `json:"likes" gorm:"default:0"`
	CreatedAt    time.Time `json:"created_at"`
}

// Puzzle represents an interactive puzzle (sudoku, math, logic, word)
type Puzzle struct {
	ID           uint             `json:"id" gorm:"primaryKey"`
	PuzzleType   string           `json:"puzzle_type" gorm:"not null;index"`
	CategoryID   uint             `json:"category_id" gorm:"not null;index"`
	Title        string           `json:"title" gorm:"not null"`
	Question     string           `json:"question" gorm:"type:text;not null"`
	Answer       string           `json:"-" gorm:"type:text;not null"`
	AnswerType   string           `json:"answer_type" gorm:"not null;default:'text'"`
	Options      string           `json:"options,omitempty" gorm:"type:text"`
	Hint         string           `json:"hint" gorm:"type:text;default:''"`
	Explanation  string           `json:"explanation" gorm:"type:text;default:''"`
	Difficulty   int              `json:"difficulty" gorm:"default:1"`
	Likes        int              `json:"likes" gorm:"default:0"`
	Meta         json.RawMessage  `json:"meta" gorm:"type:jsonb;default:'{}'"`
	CreatedAt    time.Time        `json:"created_at"`
}

type Category struct {
	ID             uint             `json:"id" gorm:"primaryKey"`
	Name           string           `json:"name" gorm:"uniqueIndex;not null"`
	Icon           string           `json:"icon"`
	ColorHex       string           `json:"color_hex"`
	Priority       int              `json:"priority" gorm:"default:0"`
	ContentTableID int              `json:"content_table_id" gorm:"default:0"`
	L1Category     string           `json:"l1_category" gorm:"default:''"`
	Meta           json.RawMessage  `json:"meta" gorm:"type:jsonb;default:'{}'"`
}

type Feedback struct {
	ID        uint             `json:"id" gorm:"primaryKey"`
	DeviceID  string           `json:"device_id" gorm:"index;default:''"`
	Message   string           `json:"message" gorm:"type:text;not null"`
	Meta      json.RawMessage  `json:"meta" gorm:"type:jsonb;default:'{}'"`
	CreatedAt time.Time        `json:"created_at"`
	DeletedAt gorm.DeletedAt   `json:"-" gorm:"index"`
}

type FeedResponse struct {
	Content   []Content `json:"content"`
	Page      int       `json:"page"`
	PageSize  int       `json:"page_size"`
	Total     int64     `json:"total"`
	HasMore   bool      `json:"has_more"`
}

type ContentDetail struct {
	Content
	RelatedContent []Content `json:"related_content,omitempty"`
}
