package models

import "time"

type Content struct {
	ID           uint      `json:"id" gorm:"primaryKey"`
	CategoryID   uint      `json:"category_id"`
	CategoryName string    `json:"category_name" gorm:"-"`
	Title        string    `json:"title" gorm:"not null"`
	Body         string    `json:"body" gorm:"type:text;not null"`
	Source       string    `json:"source"`
	SourceURL    string    `json:"source_url"`
	ReadTimeSecs int       `json:"read_time_secs" gorm:"default:15"`
	Tags         string    `json:"tags" gorm:"type:text"`
	ImageURL     string    `json:"image_url"`
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
