package handlers

import (
	"net/http"
	"regexp"
	"strings"

	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
	"github.com/gin-gonic/gin"
)

type SubmitFeedbackRequest struct {
	Message string `json:"message"`
}

// stripHTML removes all HTML tags from a string to prevent XSS.
func stripHTML(s string) string {
	re := regexp.MustCompile(`<[^>]*>`)
	return re.ReplaceAllString(s, "")
}

func SubmitFeedback(c *gin.Context) {
	var req SubmitFeedbackRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid request body"})
		return
	}

	// Sanitize: strip HTML tags
	clean := stripHTML(req.Message)
	// Trim whitespace
	clean = strings.TrimSpace(clean)

	// Validate: non-empty
	if clean == "" {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Feedback message cannot be empty"})
		return
	}

	// Validate: max 500 characters (use rune for proper Unicode handling)
	if len([]rune(clean)) > 500 {
		clean = string([]rune(clean)[:500])
	}

	// Store in database (GORM uses parameterized queries, safe from SQL injection)
	feedback := models.Feedback{Message: clean}
	result := database.DB.Create(&feedback)
	if result.Error != nil {
		jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to save feedback"})
		return
	}

	jsonResponse(c, http.StatusOK, gin.H{
		"success": true,
		"message": "Feedback submitted. Thank you!",
		"id":      feedback.ID,
	})
}
