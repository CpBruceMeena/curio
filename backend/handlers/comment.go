package handlers

import (
	"encoding/json"
	"fmt"
	"math/rand"
	"net/http"
	"strconv"
	"time"

	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
	"github.com/gin-gonic/gin"
)

// ── Request / Response Types ───────────────────────────────────

type AddCommentRequest struct {
	Text  string `json:"text" binding:"required"`
	Email string `json:"email,omitempty"`
}

type CommentEntry struct {
	ID        string `json:"id"`
	Text      string `json:"text"`
	DeviceID  string `json:"device_id"`
	Email     string `json:"email,omitempty"`
	CreatedAt string `json:"created_at"`
}

type CommentsResponse struct {
	ContentID uint           `json:"content_id"`
	Comments  []CommentEntry `json:"comments"`
	Total     int            `json:"total"`
}

// ── Handlers ──────────────────────────────────────────────────

// GetComments returns all comments for a content item.
func GetComments(c *gin.Context) {
	contentID, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid content ID"})
		return
	}

	var cc models.ContentComment
	result := database.DB.Where("content_id = ?", contentID).First(&cc)

	if result.Error != nil {
		// No comments yet — return empty
		jsonResponse(c, http.StatusOK, CommentsResponse{
			ContentID: uint(contentID),
			Comments:  []CommentEntry{},
			Total:     0,
		})
		return
	}

	entries := parseComments(cc.Comments)
	jsonResponse(c, http.StatusOK, CommentsResponse{
		ContentID: uint(contentID),
		Comments:  entries,
		Total:     len(entries),
	})
}

// AddComment appends a new comment to the content's comment thread.
func AddComment(c *gin.Context) {
	contentID, err := strconv.Atoi(c.Param("id"))
	if err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid content ID"})
		return
	}

	var req AddCommentRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Comment text is required"})
		return
	}

	// Get device_id from header or query
	deviceID := c.GetHeader("X-Device-ID")
	if deviceID == "" {
		deviceID = c.Query("device_id")
	}
	if deviceID == "" {
		deviceID = "anonymous"
	}

	now := time.Now().UTC()
	entry := CommentEntry{
		ID:        fmt.Sprintf("%d-%d", now.UnixNano(), rand.Intn(99999)),
		Text:      req.Text,
		DeviceID:  deviceID,
		Email:     req.Email,
		CreatedAt: now.Format(time.RFC3339),
	}

	// Upsert: find existing or create new
	var cc models.ContentComment
	result := database.DB.Where("content_id = ?", contentID).First(&cc)

	if result.Error != nil {
		// First comment — create new record
		entries := []CommentEntry{entry}
		raw, _ := json.Marshal(entries)
		cc = models.ContentComment{
			ContentID: uint(contentID),
			Comments:  raw,
		}
		if err := database.DB.Create(&cc).Error; err != nil {
			jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to add comment"})
			return
		}
	} else {
		// Append to existing comments
		entries := parseComments(cc.Comments)
		entries = append(entries, entry)
		raw, _ := json.Marshal(entries)
		if err := database.DB.Model(&cc).Update("comments", raw).Error; err != nil {
			jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to add comment"})
			return
		}
	}

	jsonResponse(c, http.StatusOK, gin.H{
		"success": true,
		"comment": entry,
	})
}

// ── Helpers ────────────────────────────────────────────────────

func parseComments(raw json.RawMessage) []CommentEntry {
	var entries []CommentEntry
	if len(raw) == 0 {
		return entries
	}
	if err := json.Unmarshal(raw, &entries); err != nil {
		// If it's a string, try to unmarshal that
		var str string
		if json.Unmarshal(raw, &str) == nil {
			json.Unmarshal([]byte(str), &entries)
		}
	}
	if entries == nil {
		entries = []CommentEntry{}
	}
	return entries
}
