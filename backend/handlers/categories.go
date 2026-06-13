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
		jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to fetch categories"})
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

	jsonResponse(c, http.StatusOK, gin.H{
		"categories": categoriesWithCount,
		"total":      len(categoriesWithCount),
	})
}

type L1Group struct {
	Name        string              `json:"name"`
	Icon        string              `json:"icon"`
	ColorHex    string              `json:"color_hex"`
	Categories  []models.Category   `json:"categories"`
	NovelCount  int64               `json:"novel_count"`
}

func GetL1Categories(c *gin.Context) {
	var categories []models.Category
	if err := database.DB.Order("priority ASC").Find(&categories).Error; err != nil {
		jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to fetch categories"})
		return
	}

	// Group categories by l1_category
	groupMap := make(map[string][]models.Category)
	groupOrder := []string{"Facts", "Poems", "Short Stories", "Puzzles", "Novels"}
	for _, cat := range categories {
		l1 := cat.L1Category
		if l1 == "" {
			l1 = "Facts"
		}
		groupMap[l1] = append(groupMap[l1], cat)
	}

	// Define icon and color for each L1 group
	groupMeta := map[string]struct{ icon, color string }{
		"Facts":         {"menu_book", "#00f4fe"},
		"Poems":         {"auto_stories", "#f472b6"},
		"Short Stories": {"article", "#06b6d4"},
		"Puzzles":       {"extension", "#f97316"},
		"Novels":        {"auto_stories", "#8b5cf6"},
	}

	// Count novels for the Novels group
	var novelCount int64
	database.DB.Model(&models.Novel{}).Count(&novelCount)

	var groups []L1Group
	for _, name := range groupOrder {
		if cats, ok := groupMap[name]; ok {
			meta := groupMeta[name]
			nc := int64(0)
			if name == "Novels" {
				nc = novelCount
			}
			groups = append(groups, L1Group{
				Name:       name,
				Icon:       meta.icon,
				ColorHex:   meta.color,
				Categories: cats,
				NovelCount: nc,
			})
		}
	}

	jsonResponse(c, http.StatusOK, gin.H{
		"groups": groups,
		"total":  len(groups),
	})
}
