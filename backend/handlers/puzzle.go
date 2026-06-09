package handlers

import (
	"net/http"
	"strconv"
	"strings"

	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

func GetPuzzles(c *gin.Context) {
	puzzleType := c.Query("type")
	categoryID := c.Query("category_id")
	limitStr := c.DefaultQuery("limit", "20")

	limit, _ := strconv.Atoi(limitStr)
	if limit < 1 || limit > 50 {
		limit = 20
	}

	query := database.DB.Model(&models.Puzzle{})
	if puzzleType != "" {
		query = query.Where("puzzle_type = ?", puzzleType)
	}
	if categoryID != "" {
		query = query.Where("category_id = ?", categoryID)
	}

	var total int64
	query.Count(&total)

	var puzzles []models.Puzzle
	result := query.Order("RANDOM()").Limit(limit).Find(&puzzles)
	if result.Error != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to fetch puzzles"})
		return
	}

	// Strip answers from response — only reveal on validation
	type PuzzlePublic struct {
		ID          uint   `json:"id"`
		PuzzleType  string `json:"puzzle_type"`
		CategoryID  uint   `json:"category_id"`
		Title       string `json:"title"`
		Question    string `json:"question"`
		AnswerType  string `json:"answer_type"`
		Options     string `json:"options,omitempty"`
		Hint        string `json:"hint"`
		Explanation string `json:"explanation"`
		Difficulty  int    `json:"difficulty"`
		Likes       int    `json:"likes"`
	}

	publicList := make([]PuzzlePublic, len(puzzles))
	for i, p := range puzzles {
		publicList[i] = PuzzlePublic{
			ID:          p.ID,
			PuzzleType:  p.PuzzleType,
			CategoryID:  p.CategoryID,
			Title:       p.Title,
			Question:    p.Question,
			AnswerType:  p.AnswerType,
			Options:     p.Options,
			Hint:        p.Hint,
			Explanation: p.Explanation,
			Difficulty:  p.Difficulty,
			Likes:       p.Likes,
		}
	}

	c.JSON(http.StatusOK, gin.H{
		"puzzles": publicList,
		"total":   total,
	})
}

type ValidateRequest struct {
	Answer string `json:"answer" binding:"required"`
}

func ValidatePuzzle(c *gin.Context) {
	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid puzzle ID"})
		return
	}

	var req ValidateRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Answer is required"})
		return
	}

	var puzzle models.Puzzle
	if err := database.DB.First(&puzzle, id).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Puzzle not found"})
		return
	}

	// Normalize and compare
	userAnswer := strings.TrimSpace(strings.ToLower(req.Answer))
	correctAnswer := strings.TrimSpace(strings.ToLower(puzzle.Answer))
	isCorrect := userAnswer == correctAnswer

	// For numeric answers, try parsing as float for tolerance
	if !isCorrect && puzzle.AnswerType == "number" {
		userNum, userErr := strconv.ParseFloat(userAnswer, 64)
		correctNum, correctErr := strconv.ParseFloat(correctAnswer, 64)
		if userErr == nil && correctErr == nil {
			diff := userNum - correctNum
			if diff < 0 {
				diff = -diff
			}
			isCorrect = diff < 0.01
		}
	}

	c.JSON(http.StatusOK, gin.H{
		"correct":     isCorrect,
		"explanation": puzzle.Explanation,
	})
}

func LikePuzzle(c *gin.Context) {
	idStr := c.Param("id")
	id, err := strconv.Atoi(idStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid puzzle ID"})
		return
	}

	result := database.DB.Model(&models.Puzzle{}).Where("id = ?", id).
		UpdateColumn("likes", gorm.Expr("likes + 1"))

	if result.Error != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to like puzzle"})
		return
	}
	if result.RowsAffected == 0 {
		c.JSON(http.StatusNotFound, gin.H{"error": "Puzzle not found"})
		return
	}

	var likes int
	database.DB.Model(&models.Puzzle{}).Select("likes").Where("id = ?", id).Scan(&likes)

	c.JSON(http.StatusOK, gin.H{"likes": likes})
}
