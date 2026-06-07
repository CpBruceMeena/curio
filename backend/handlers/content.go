package handlers

import (
	"net/http"
	"strconv"

	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

func GetContent(c *gin.Context) {
	id, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid content ID"})
		return
	}

	var content models.Content
	result := database.DB.First(&content, id)
	if result.Error != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Content not found"})
		return
	}

	var category models.Category
	database.DB.First(&category, content.CategoryID)
	content.CategoryName = category.Name

	// Find related content from same category
	var related []models.Content
	database.DB.Where("category_id = ? AND id != ?", content.CategoryID, content.ID).
		Order("likes DESC").
		Limit(3).
		Find(&related)

	detail := models.ContentDetail{
		Content:         content,
		RelatedContent:  related,
	}

	c.JSON(http.StatusOK, detail)
}

func LikeContent(c *gin.Context) {
	id, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid content ID"})
		return
	}

	result := database.DB.Model(&models.Content{}).Where("id = ?", id).
		UpdateColumn("likes", gorm.Expr("likes + 1"))
	if result.Error != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to like content"})
		return
	}
	if result.RowsAffected == 0 {
		c.JSON(http.StatusNotFound, gin.H{"error": "Content not found"})
		return
	}

	var content models.Content
	database.DB.First(&content, id)
	c.JSON(http.StatusOK, gin.H{"likes": content.Likes})
}
