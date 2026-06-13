package handlers

import (
	"fmt"
	"net/http"
	"os/exec"
	"strconv"
	"strings"
	"time"

	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

// GetNovels returns a paginated list of all novels.
func GetNovels(c *gin.Context) {
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "20"))
	categoryID := c.Query("category_id")

	if page < 1 {
		page = 1
	}
	if limit < 1 || limit > 50 {
		limit = 20
	}

	query := database.DB.Model(&models.Novel{})
	if categoryID != "" {
		query = query.Where("category_id = ?", categoryID)
	}

	var total int64
	query.Count(&total)

	var novels []models.Novel
	result := query.Order("created_at DESC").Offset((page - 1) * limit).Limit(limit).Find(&novels)
	if result.Error != nil {
		jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to fetch novels"})
		return
	}

	jsonResponse(c, http.StatusOK, models.NovelsListResponse{
		Novels: novels,
		Total:  total,
		Page:   page,
		Limit:  limit,
	})
}

// GetNovel returns novel detail with chapter list and optional user progress.
func GetNovel(c *gin.Context) {
	id, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid novel ID"})
		return
	}

	var novel models.Novel
	if err := database.DB.First(&novel, id).Error; err != nil {
		jsonResponse(c, http.StatusNotFound, gin.H{"error": "Novel not found"})
		return
	}

	// Fetch chapters
	var chapters []models.NovelChapter
	database.DB.Where("novel_id = ?", id).Order("chapter_number ASC").Find(&chapters)

	// Fetch user progress if device_id provided
	deviceID := c.Query("device_id")
	var progress *models.UserNovelProgress
	if deviceID != "" {
		var p models.UserNovelProgress
		err := database.DB.Where("device_id = ? AND novel_id = ?", deviceID, id).First(&p).Error
		if err == nil {
			progress = &p
		}
	}

	jsonResponse(c, http.StatusOK, models.NovelDetailResponse{
		Novel:    novel,
		Chapters: chapters,
		Progress: progress,
	})
}

// GetNovelChapters returns the chapter list for a novel.
func GetNovelChapters(c *gin.Context) {
	id, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid novel ID"})
		return
	}

	var chapters []models.NovelChapter
	result := database.DB.Where("novel_id = ?", id).Order("chapter_number ASC").Find(&chapters)
	if result.Error != nil {
		jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to fetch chapters"})
		return
	}

	jsonResponse(c, http.StatusOK, gin.H{
		"chapters": chapters,
		"total":    len(chapters),
	})
}

// GetNovelChapter returns a single chapter's body.
func GetNovelChapter(c *gin.Context) {
	novelID, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid novel ID"})
		return
	}

	chapterNum, err := strconv.Atoi(c.Param("chapter"))
	if err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid chapter number"})
		return
	}

	var chapter models.NovelChapter
	result := database.DB.Where("novel_id = ? AND chapter_number = ?", novelID, chapterNum).First(&chapter)
	if result.Error != nil {
		jsonResponse(c, http.StatusNotFound, gin.H{"error": "Chapter not found"})
		return
	}

	jsonResponse(c, http.StatusOK, chapter)
}

// UpdateNovelProgress saves or updates a user's reading progress on a novel.
func UpdateNovelProgress(c *gin.Context) {
	novelID, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid novel ID"})
		return
	}

	var req models.ProgressUpdateRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "device_id is required"})
		return
	}

	// Verify novel exists
	var novel models.Novel
	if err := database.DB.First(&novel, novelID).Error; err != nil {
		jsonResponse(c, http.StatusNotFound, gin.H{"error": "Novel not found"})
		return
	}

	// Upsert progress
	var progress models.UserNovelProgress
	result := database.DB.Where("device_id = ? AND novel_id = ?", req.DeviceID, novelID).First(&progress)

	updates := map[string]interface{}{
		"updated_at": time.Now(),
	}
	if req.LastChapter > 0 {
		updates["last_chapter"] = req.LastChapter
	}
	if req.LastPosition > 0 {
		updates["last_position"] = req.LastPosition
	}
	if req.Completed != nil {
		updates["completed"] = *req.Completed
	}
	if req.Bookmarked != nil {
		updates["bookmarked"] = *req.Bookmarked
	}

	if result.Error != nil {
		// Create new progress
		progress = models.UserNovelProgress{
			DeviceID:     req.DeviceID,
			NovelID:      uint(novelID),
			LastChapter:  1,
			LastPosition: 0,
			Completed:    false,
		}
		if req.LastChapter > 0 {
			progress.LastChapter = req.LastChapter
		}
		database.DB.Create(&progress)
	} else {
		database.DB.Model(&progress).Updates(updates)
	}

	// Re-fetch to return current state
	database.DB.Where("device_id = ? AND novel_id = ?", req.DeviceID, novelID).First(&progress)
	jsonResponse(c, http.StatusOK, progress)
}

// RefreshNovel re-fetches a novel from Gutenberg and replaces all chapter data.
// Used by the app's "Refresh" feature to fix incorrect chapter parsing.
func RefreshNovel(c *gin.Context) {
	id, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid novel ID"})
		return
	}

	var novel models.Novel
	if err := database.DB.First(&novel, id).Error; err != nil {
		jsonResponse(c, http.StatusNotFound, gin.H{"error": "Novel not found"})
		return
	}

	// Delete existing chapters
	if err := database.DB.Where("novel_id = ?", id).Delete(&models.NovelChapter{}).Error; err != nil {
		jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to clear existing chapters"})
		return
	}

	// Re-fetch from Gutenberg
	// Note: This runs a Python subprocess to re-download and re-parse the novel.
	// The Gutenberg ID is stored in the source_url field ("https://www.gutenberg.org/ebooks/{id}")
	gutenbergID := extractGutenbergID(novel.SourceURL)
	if gutenbergID == 0 {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Cannot determine Gutenberg ID from source URL"})
		return
	}

	// Execute the Python batch scraper for this single ID
	cmdStr := fmt.Sprintf(
		"cd scripts && source venv/bin/activate && python -m scraper --novels-batch 0 --ids %d --replace 2>&1",
		gutenbergID,
	)
	cmd := exec.Command("bash", "-c", cmdStr)
	output, err := cmd.CombinedOutput()
	if err != nil {
		jsonResponse(c, http.StatusInternalServerError, gin.H{
			"error": fmt.Sprintf("Refresh failed: %v\n%s", err, string(output)),
		})
		return
	}

	// Reload chapters from DB
	var chapters []models.NovelChapter
	database.DB.Where("novel_id = ?", id).Order("chapter_number ASC").Find(&chapters)

	jsonResponse(c, http.StatusOK, gin.H{
		"novel_id": id,
		"title":    novel.Title,
		"chapters": len(chapters),
		"message":  "Novel refreshed successfully",
	})
}

// extractGutenbergID parses a Gutenberg URL like "https://www.gutenberg.org/ebooks/1342" → 1342
func extractGutenbergID(sourceURL string) int {
	if !strings.Contains(sourceURL, "gutenberg.org/ebooks/") {
		return 0
	}
	parts := strings.Split(sourceURL, "/")
	for i, part := range parts {
		if part == "ebooks" && i+1 < len(parts) {
			if id, err := strconv.Atoi(parts[i+1]); err == nil {
				return id
			}
		}
	}
	return 0
}

// LikeNovel increments the likes count on a novel.
func LikeNovel(c *gin.Context) {
	id, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid novel ID"})
		return
	}

	var novel models.Novel
	if err := database.DB.First(&novel, id).Error; err != nil {
		jsonResponse(c, http.StatusNotFound, gin.H{"error": "Novel not found"})
		return
	}

	database.DB.Model(&novel).UpdateColumn("likes", gorm.Expr("likes + 1"))

	jsonResponse(c, http.StatusOK, gin.H{
		"likes": novel.Likes + 1,
	})
}
