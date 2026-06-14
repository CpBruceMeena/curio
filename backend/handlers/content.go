package handlers

import (
	"net/http"
	"strconv"

	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

// contentTableForCategory returns the content table name for a given category ID.
// Uses the stable content_table_id from the categories table.
func contentTableForCategory(catID uint) (string, error) {
	var category models.Category
	if err := database.DB.First(&category, catID).Error; err != nil {
		return "", err
	}
	return database.ContentTableName(uint(category.ContentTableID), catID), nil
}

func GetContent(c *gin.Context) {
	globalID, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid content ID"})
		return
	}

	// Global ID = categoryID * 10_000_000 + localID
	// IDs in the per-category tables are already stored as global IDs
	catID := uint(globalID) / 10000000
	if catID == 0 {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid content ID"})
		return
	}

	tableName, err := contentTableForCategory(catID)
	if err != nil {
		jsonResponse(c, http.StatusNotFound, gin.H{"error": "Category not found"})
		return
	}

	// Query the table using the global ID directly
	var content models.Content
	result := database.DB.Table(tableName).First(&content, globalID)
	if result.Error != nil {
		jsonResponse(c, http.StatusNotFound, gin.H{"error": "Content not found"})
		return
	}
	content.CategoryID = catID

	// Look up category name
	var category models.Category
	database.DB.First(&category, catID)
	content.CategoryName = category.Name

	// Find related content from the same per-category table
	var related []models.Content
	database.DB.Table(tableName).
		Where("id != ?", globalID).
		Order("likes DESC").
		Limit(3).
		Find(&related)

	for i := range related {
		related[i].CategoryName = category.Name
		related[i].CategoryID = catID
		// IDs from the table are already global — no encoding needed
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

	catID := uint(globalID) / 10000000
	if catID == 0 {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid content ID"})
		return
	}

	tableName, err := contentTableForCategory(catID)
	if err != nil {
		jsonResponse(c, http.StatusNotFound, gin.H{"error": "Category not found"})
		return
	}

	var expr string
	if action == "like" {
		expr = "likes + 1"
	} else {
		expr = "GREATEST(likes - 1, 0)"
	}

	result := database.DB.Table(tableName).
		Where("id = ?", globalID).
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
	database.DB.Table(tableName).Select("likes").Where("id = ?", globalID).Scan(&likesRes)

	jsonResponse(c, http.StatusOK, gin.H{"likes": likesRes.Likes})
}
