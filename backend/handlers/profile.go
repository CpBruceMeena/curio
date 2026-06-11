package handlers

import (
	"net/http"

	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
	"github.com/gin-gonic/gin"
)

// ── Profile (user info) ────────────────────────────────────────

func GetProfile(c *gin.Context) {
	deviceID := c.Query("device_id")
	if deviceID == "" {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "device_id is required"})
		return
	}

	var profile models.Profile
	result := database.DB.Where("device_id = ?", deviceID).First(&profile)
	if result.Error != nil {
		jsonResponse(c, http.StatusOK, models.Profile{DeviceID: deviceID})
		return
	}

	jsonResponse(c, http.StatusOK, profile)
}

func CreateOrUpdateProfile(c *gin.Context) {
	var req models.ProfileRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid request body"})
		return
	}

	deviceID := c.Query("device_id")
	if deviceID == "" {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "device_id is required"})
		return
	}

	var profile models.Profile
	result := database.DB.Where("device_id = ?", deviceID).First(&profile)

	if result.Error != nil {
		profile = models.Profile{
			DeviceID: deviceID,
			Name:     req.Name,
			Age:      req.Age,
			Gender:   req.Gender,
			Likes:    req.Likes,
			Dislikes: req.Dislikes,
		}
		if err := database.DB.Create(&profile).Error; err != nil {
			jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to create profile"})
			return
		}
	} else {
		updates := map[string]interface{}{}
		if req.Name != "" {
			updates["name"] = req.Name
		}
		if req.Age > 0 {
			updates["age"] = req.Age
		}
		if req.Gender != "" {
			updates["gender"] = req.Gender
		}
		if req.Likes != "" {
			updates["likes"] = req.Likes
		}
		if req.Dislikes != "" {
			updates["dislikes"] = req.Dislikes
		}
		if len(updates) > 0 {
			database.DB.Model(&profile).Updates(updates)
		}
	}

	database.DB.Where("device_id = ?", deviceID).First(&profile)
	jsonResponse(c, http.StatusOK, profile)
}

// ── Device Info ────────────────────────────────────────────────

func GetDeviceInfo(c *gin.Context) {
	deviceID := c.Query("device_id")
	if deviceID == "" {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "device_id is required"})
		return
	}

	var info models.DeviceInfo
	result := database.DB.Where("device_id = ?", deviceID).First(&info)
	if result.Error != nil {
		jsonResponse(c, http.StatusOK, models.DeviceInfo{DeviceID: deviceID})
		return
	}

	jsonResponse(c, http.StatusOK, info)
}

func SubmitDeviceInfo(c *gin.Context) {
	var req models.DeviceInfoRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid request body"})
		return
	}

	deviceID := c.Query("device_id")
	if deviceID == "" {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "device_id is required"})
		return
	}

	var info models.DeviceInfo
	result := database.DB.Where("device_id = ?", deviceID).First(&info)

	if result.Error != nil {
		info = models.DeviceInfo{
			DeviceID:     deviceID,
			OSVersion:    req.OSVersion,
			AppVersion:   req.AppVersion,
			DeviceModel:  req.DeviceModel,
			Manufacturer: req.Manufacturer,
			ScreenSize:   req.ScreenSize,
			Language:     req.Language,
			Timezone:     req.Timezone,
		}
		if err := database.DB.Create(&info).Error; err != nil {
			jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to save device info"})
			return
		}
	} else {
		updates := map[string]interface{}{}
		if req.OSVersion != "" {
			updates["os_version"] = req.OSVersion
		}
		if req.AppVersion != "" {
			updates["app_version"] = req.AppVersion
		}
		if req.DeviceModel != "" {
			updates["device_model"] = req.DeviceModel
		}
		if req.Manufacturer != "" {
			updates["manufacturer"] = req.Manufacturer
		}
		if req.ScreenSize != "" {
			updates["screen_size"] = req.ScreenSize
		}
		if req.Language != "" {
			updates["language"] = req.Language
		}
		if req.Timezone != "" {
			updates["timezone"] = req.Timezone
		}
		if len(updates) > 0 {
			database.DB.Model(&info).Updates(updates)
		}
	}

	database.DB.Where("device_id = ?", deviceID).First(&info)
	jsonResponse(c, http.StatusOK, info)
}
