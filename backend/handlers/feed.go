package handlers

import (
	"math"
	"net/http"
	"strconv"

	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
	"github.com/gin-gonic/gin"
)

const defaultPageSize = 100
const maxPageSize = 200

func GetFeed(c *gin.Context) {
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "100"))
	categoryID := c.Query("category_id")
	useRandom := c.DefaultQuery("random", "false") == "true"

	if page < 1 {
		page = 1
	}
	if pageSize < 1 || pageSize > maxPageSize {
		pageSize = defaultPageSize
	}

	offset := (page - 1) * pageSize

	// Count total
	var total int64
	query := database.DB.Model(&models.Content{})
	if categoryID != "" {
		query = query.Where("category_id = ?", categoryID)
	}
	query.Count(&total)

	// Fetch content
	var content []models.Content
	orderClause := "likes DESC, created_at DESC"
	if useRandom {
		orderClause = "RANDOM()"
	}
	dataQuery := database.DB.Order(orderClause).Offset(offset).Limit(pageSize)
	if categoryID != "" {
		dataQuery = dataQuery.Where("category_id = ?", categoryID)
	}
	result := dataQuery.Find(&content)
	if result.Error != nil {
		jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to fetch feed"})
		return
	}

	// Batch-load category names
	if len(content) > 0 {
		categoryIDs := make([]uint, 0, len(content))
		idSet := make(map[uint]bool)
		for _, item := range content {
			if !idSet[item.CategoryID] {
				categoryIDs = append(categoryIDs, item.CategoryID)
				idSet[item.CategoryID] = true
			}
		}

		var categories []models.Category
		database.DB.Where("id IN ?", categoryIDs).Find(&categories)

		catMap := make(map[uint]string)
		for _, cat := range categories {
			catMap[cat.ID] = cat.Name
		}

		for i := range content {
			content[i].CategoryName = catMap[content[i].CategoryID]
		}
	}

	totalPages := int(math.Ceil(float64(total) / float64(pageSize)))

	response := models.FeedResponse{
		Content:  content,
		Page:     page,
		PageSize: pageSize,
		Total:    total,
		HasMore:  page < totalPages,
	}

	jsonResponse(c, http.StatusOK, response)
}
