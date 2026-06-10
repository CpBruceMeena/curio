package models

import (
	"time"

	"gorm.io/gorm"
)

type Profile struct {
	ID        uint           `json:"id" gorm:"primaryKey"`
	DeviceID  string         `json:"device_id" gorm:"uniqueIndex;not null;default:''"`
	Name      string         `json:"name" gorm:"default:''"`
	Age       int            `json:"age" gorm:"default:0"`
	Gender    string         `json:"gender" gorm:"default:''"`
	Likes     string         `json:"likes" gorm:"type:text;default:''"`
	Dislikes  string         `json:"dislikes" gorm:"type:text;default:''"`
	Interests string         `json:"interests" gorm:"type:text;default:''"`
	CreatedAt time.Time      `json:"created_at"`
	UpdatedAt time.Time      `json:"updated_at"`
	DeletedAt gorm.DeletedAt `json:"-" gorm:"index"`
}

type ProfileRequest struct {
	Name     string `json:"name"`
	Age      int    `json:"age"`
	Gender   string `json:"gender"`
	Likes    string `json:"likes"`
	Dislikes string `json:"dislikes"`
}
