package handlers

import (
	"net/http"

	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
	"github.com/gin-gonic/gin"
)

func GetProfile(c *gin.Context) {
	deviceID := c.Query("device_id")
	if deviceID == "" {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "device_id is required"})
		return
	}

	var profile models.Profile
	result := database.DB.Where("device_id = ?", deviceID).First(&profile)
	if result.Error != nil {
		// Return empty profile rather than 404 — client can distinguish by device_id == ""
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
		// Create new profile
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
		// Update existing profile
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

	// Re-read to return full profile
	database.DB.Where("device_id = ?", deviceID).First(&profile)
	jsonResponse(c, http.StatusOK, profile)
}
