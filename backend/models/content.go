package models

import (
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

type Category struct {
	ID       uint   `json:"id" gorm:"primaryKey"`
	Name     string `json:"name" gorm:"uniqueIndex;not null"`
	Icon     string `json:"icon"`
	ColorHex string `json:"color_hex"`
	Priority int    `json:"priority" gorm:"default:0"`
}

type Feedback struct {
	ID        uint           `json:"id" gorm:"primaryKey"`
	Message   string         `json:"message" gorm:"type:text;not null"`
	CreatedAt time.Time      `json:"created_at"`
	DeletedAt gorm.DeletedAt `json:"-" gorm:"index"`
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
