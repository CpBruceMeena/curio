package handlers

import (
	"net/http"
	"strconv"

	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

// Decode a global content ID into (categoryID, localID).
// Global ID = categoryID * 10_000_000 + localID
func decodeContentID(globalID int) (categoryID uint, localID uint) {
	categoryID = uint(globalID / 10000000)
	localID = uint(globalID % 10000000)
	return
}

func GetContent(c *gin.Context) {
	globalID, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid content ID"})
		return
	}

	// Decode global ID: categoryID * 10_000_000 + localID
	// This avoids ID collisions across per-category tables in the UNION ALL VIEW
	catID, localID := decodeContentID(globalID)

	var content models.Content
	var category models.Category

	if catID > 0 {
		// Global ID — query the correct per-category table directly
		if err := database.DB.First(&category, catID).Error; err != nil {
			jsonResponse(c, http.StatusNotFound, gin.H{"error": "Category not found"})
			return
		}
		tableName := database.ContentTableName(uint(category.ContentTableID), catID)
		result := database.DB.Table(tableName).First(&content, localID)
		if result.Error != nil {
			jsonResponse(c, http.StatusNotFound, gin.H{"error": "Content not found"})
			return
		}
		content.CategoryID = catID
		content.CategoryName = category.Name
	} else {
		// Legacy ID (raw local ID) — fall back to VIEW lookup
		result := database.DB.First(&content, globalID)
		if result.Error != nil {
			jsonResponse(c, http.StatusNotFound, gin.H{"error": "Content not found"})
			return
		}
		database.DB.First(&category, content.CategoryID)
		content.CategoryName = category.Name
		localID = content.ID // raw ID from VIEW is the correct local ID for legacy lookups
	}

	// Find related content from the same per-category table
	tableName := database.ContentTableName(uint(category.ContentTableID), content.CategoryID)
	var related []models.Content
	database.DB.Table(tableName).
		Where("id != ?", localID).
		Order("likes DESC").
		Limit(3).
		Find(&related)

	// Set category names on related content
	for i := range related {
		related[i].CategoryName = category.Name
		related[i].CategoryID = content.CategoryID
		// Build global IDs for related items
		related[i].ID = uint(content.CategoryID)*10000000 + related[i].ID
	}

	detail := models.ContentDetail{
		Content:        content,
		RelatedContent: related,
	}

	jsonResponse(c, http.StatusOK, detail)
}

// LikeContent toggles the likes count on content.
// Query param: action=like (default) increments, action=unlike decrements.
func LikeContent(c *gin.Context) {
	globalID, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid content ID"})
		return
	}

	action := c.DefaultQuery("action", "like")
	if action != "like" && action != "unlike" {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Action must be 'like' or 'unlike'"})
		return
	}

	catID, localID := decodeContentID(globalID)
	if catID == 0 {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid content ID"})
		return
	}

	// Must update the per-category table directly (VIEW is UNION ALL, not updatable)
	var category models.Category
	if err := database.DB.First(&category, catID).Error; err != nil {
		jsonResponse(c, http.StatusNotFound, gin.H{"error": "Category not found"})
		return
	}
	tableName := database.ContentTableName(uint(category.ContentTableID), catID)

	var expr string
	if action == "like" {
		expr = "likes + 1"
	} else {
		// Don't let likes go below 0
		expr = "GREATEST(likes - 1, 0)"
	}

	result := database.DB.Table(tableName).
		Where("id = ?", localID).
		UpdateColumn("likes", gorm.Expr(expr))

	if result.Error != nil {
		jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to update likes"})
		return
	}
	if result.RowsAffected == 0 {
		jsonResponse(c, http.StatusNotFound, gin.H{"error": "Content not found"})
		return
	}

	// Read back the updated likes count
	type LikesResult struct {
		Likes int `json:"likes"`
	}
	var likesRes LikesResult
	database.DB.Table(tableName).Select("likes").Where("id = ?", localID).Scan(&likesRes)

	jsonResponse(c, http.StatusOK, gin.H{"likes": likesRes.Likes})
}
