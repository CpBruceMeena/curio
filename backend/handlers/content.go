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
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid content ID"})
		return
	}

	// Look up the content via the global VIEW (SELECT only)
	var content models.Content
	result := database.DB.First(&content, globalID)
	if result.Error != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Content not found"})
		return
	}

	var category models.Category
	database.DB.First(&category, content.CategoryID)
	content.CategoryName = category.Name

	// Find related content from the same per-category table
	_, localID := decodeContentID(globalID)
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

	c.JSON(http.StatusOK, detail)
}

func LikeContent(c *gin.Context) {
	globalID, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid content ID"})
		return
	}

	catID, localID := decodeContentID(globalID)
	if catID == 0 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid content ID"})
		return
	}

	// Must update the per-category table directly (VIEW is UNION ALL, not updatable)
	// ContentTableName falls back to catID when contentTableID is 0
	tableName := database.ContentTableName(0, catID)
	result := database.DB.Table(tableName).
		Where("id = ?", localID).
		UpdateColumn("likes", gorm.Expr("likes + 1"))

	if result.Error != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to like content"})
		return
	}
	if result.RowsAffected == 0 {
		c.JSON(http.StatusNotFound, gin.H{"error": "Content not found"})
		return
	}

	// Read back the updated likes count
	type LikesResult struct {
		Likes int `json:"likes"`
	}
	var likesRes LikesResult
	database.DB.Table(tableName).Select("likes").Where("id = ?", localID).Scan(&likesRes)

	c.JSON(http.StatusOK, gin.H{"likes": likesRes.Likes})
}
