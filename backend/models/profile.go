package models

import (
	"encoding/json"
	"time"

	"gorm.io/gorm"
)

type Profile struct {
	ID        uint             `json:"id" gorm:"primaryKey"`
	DeviceID  string           `json:"device_id" gorm:"uniqueIndex;not null;default:''"`
	Name      string           `json:"name" gorm:"default:''"`
	Age       int              `json:"age" gorm:"default:0"`
	Gender    string           `json:"gender" gorm:"default:''"`
	Likes     string           `json:"likes" gorm:"type:text;default:''"`
	Dislikes  string           `json:"dislikes" gorm:"type:text;default:''"`
	Interests string           `json:"interests" gorm:"type:text;default:''"`
	Meta      json.RawMessage  `json:"meta" gorm:"type:jsonb;default:'{}'"`
	CreatedAt time.Time        `json:"created_at"`
	UpdatedAt time.Time        `json:"updated_at"`
	DeletedAt gorm.DeletedAt   `json:"-" gorm:"index"`
}

type ProfileRequest struct {
	Name     string `json:"name"`
	Age      int    `json:"age"`
	Gender   string `json:"gender"`
	Likes    string `json:"likes"`
	Dislikes string `json:"dislikes"`
}

// DeviceInfo stores device-specific details linked by device UUID.
// Separate from Profile (user info) so device metadata can be collected
// independently of user identity.
type DeviceInfo struct {
	ID           uint             `json:"id" gorm:"primaryKey"`
	DeviceID     string           `json:"device_id" gorm:"uniqueIndex;not null"`
	OSVersion    string           `json:"os_version" gorm:"default:''"`
	AppVersion   string           `json:"app_version" gorm:"default:''"`
	DeviceModel  string           `json:"device_model" gorm:"default:''"`
	Manufacturer string           `json:"manufacturer" gorm:"default:''"`
	ScreenSize   string           `json:"screen_size" gorm:"default:''"`
	Language     string           `json:"language" gorm:"default:''"`
	Timezone     string           `json:"timezone" gorm:"default:''"`
	Meta         json.RawMessage  `json:"meta" gorm:"type:jsonb;default:'{}'"`
	CreatedAt    time.Time        `json:"created_at"`
	UpdatedAt    time.Time        `json:"updated_at"`
}

type DeviceInfoRequest struct {
	OSVersion    string `json:"os_version"`
	AppVersion   string `json:"app_version"`
	DeviceModel  string `json:"device_model"`
	Manufacturer string `json:"manufacturer"`
	ScreenSize   string `json:"screen_size"`
	Language     string `json:"language"`
	Timezone     string `json:"timezone"`
}
