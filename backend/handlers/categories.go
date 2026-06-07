package handlers

import (
	"net/http"

	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
	"github.com/gin-gonic/gin"
)

func GetCategories(c *gin.Context) {
	var categories []models.Category
	result := database.DB.Order("priority ASC").Find(&categories)
	if result.Error != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to fetch categories"})
		return
	}

	// Attach content count per category
	type CategoryWithCount struct {
		models.Category
		ContentCount int64 `json:"content_count"`
	}

	var categoriesWithCount []CategoryWithCount
	for _, cat := range categories {
		var count int64
		database.DB.Model(&models.Content{}).Where("category_id = ?", cat.ID).Count(&count)
		categoriesWithCount = append(categoriesWithCount, CategoryWithCount{
			Category:     cat,
			ContentCount: count,
		})
	}

	c.JSON(http.StatusOK, gin.H{
		"categories": categoriesWithCount,
		"total":      len(categoriesWithCount),
	})
}
