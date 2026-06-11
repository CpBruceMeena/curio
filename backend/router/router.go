package router

import (
	"github.com/curio/backend/handlers"
	"github.com/curio/backend/middleware"
	"github.com/gin-gonic/gin"
)

func Setup() *gin.Engine {
	r := gin.Default()

	// CORS middleware for Android client
	r.Use(middleware.CORS())

	// Health check
	r.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{"status": "ok", "app": "curio"})
	})

	api := r.Group("/api/v1")
	{
		// Feed
		api.GET("/feed", handlers.GetFeed)

		// Content
		api.GET("/content/:id", handlers.GetContent)
		api.POST("/content/:id/like", handlers.LikeContent)

		// Categories
		api.GET("/categories", handlers.GetCategories)
		api.GET("/categories/l1", handlers.GetL1Categories)

		// Feedback
		api.POST("/feedback", handlers.SubmitFeedback)

		// Puzzles
		api.GET("/puzzles", handlers.GetPuzzles)
		api.POST("/puzzles/:id/validate", handlers.ValidatePuzzle)
		api.POST("/puzzles/:id/like", handlers.LikePuzzle)

		// Profile
		api.GET("/profile", handlers.GetProfile)
		api.POST("/profile", handlers.CreateOrUpdateProfile)
	}

	return r
}
