package handlers

import (
	"fmt"
	"math"
	"net/http"
	"strconv"
	"strings"

	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
	"github.com/gin-gonic/gin"
)

const defaultPageSize = 100
const maxPageSize = 200

// CategoryTableInfo holds data for a single category table used in UNION ALL queries.
type CategoryTableInfo struct {
	ID    uint
	Table string
	Name  string
}

// categoryTablesCache caches the list of category tables to avoid DB query on every request.
var categoryTablesCache []CategoryTableInfo

// loadCategoryTables loads or returns cached category table info.
func loadCategoryTables() []CategoryTableInfo {
	if categoryTablesCache != nil {
		return categoryTablesCache
	}

	var cats []models.Category
	database.DB.Where("content_table_id > 0").Order("id ASC").Find(&cats)

	result := make([]CategoryTableInfo, 0, len(cats))
	for _, cat := range cats {
		tableName := fmt.Sprintf("contents_%d", cat.ContentTableID)
		safeName := strings.ReplaceAll(cat.Name, "'", "''")
		result = append(result, CategoryTableInfo{ID: cat.ID, Table: tableName, Name: safeName})
	}

	categoryTablesCache = result
	return result
}

// buildFeedUnionQuery builds a UNION ALL query across all category tables.
// IDs in the tables are already globally unique (categoryID * 10M + localID).
func buildFeedUnionQuery(catTables []CategoryTableInfo, categoryFilter string) (fromClause, whereClause string) {
	var parts []string

	// Filter tables if a specific category is requested
	filtered := catTables
	if categoryFilter != "" {
		var f []CategoryTableInfo
		for _, ct := range catTables {
			if fmt.Sprintf("%d", ct.ID) == categoryFilter {
				f = append(f, ct)
				break
			}
		}
		filtered = f
	}

	for _, ct := range filtered {
		parts = append(parts, fmt.Sprintf(
			"SELECT id, %d AS category_id, '%s' AS category_name, title, body, description, poet, source, source_url, read_time_secs, tags, likes, created_at FROM %s",
			ct.ID, ct.Name, ct.Table,
		))
	}

	fromClause = "(" + strings.Join(parts, " UNION ALL ") + ") AS all_content"

	if categoryFilter != "" {
		whereClause = fmt.Sprintf("WHERE category_id = %s", categoryFilter)
	}

	return fromClause, whereClause
}

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

	// Fetch all category tables (with caching)
	catTables := loadCategoryTables()
	if len(catTables) == 0 {
		jsonResponse(c, http.StatusOK, models.FeedResponse{Content: []models.Content{}, Page: 1, PageSize: pageSize, Total: 0, HasMore: false})
		return
	}

	fromClause, whereClause := buildFeedUnionQuery(catTables, categoryID)

	// Count total
	var total int64
	countSQL := "SELECT COUNT(*) FROM " + fromClause
	if whereClause != "" {
		countSQL += " " + whereClause
	}
	database.DB.Raw(countSQL).Scan(&total)

	// Fetch content
	var content []models.Content
	orderClause := "ORDER BY likes DESC, created_at DESC"
	if useRandom {
		orderClause = "ORDER BY RANDOM()"
	}

	dataSQL := "SELECT * FROM " + fromClause
	if whereClause != "" {
		dataSQL += " " + whereClause
	}
	dataSQL += " " + orderClause + " LIMIT ? OFFSET ?"

	result := database.DB.Raw(dataSQL, pageSize, offset).Scan(&content)
	if result.Error != nil {
		jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to fetch feed"})
		return
	}

	// No encoding needed — IDs in the table are already globally unique
	// CategoryName is already in the UNION ALL query result

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
