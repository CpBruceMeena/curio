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

		// Novels
		api.GET("/novels", handlers.GetNovels)
		api.GET("/novels/:id", handlers.GetNovel)
		api.GET("/novels/:id/chapters", handlers.GetNovelChapters)
		api.GET("/novels/:id/chapters/:chapter", handlers.GetNovelChapter)
		api.POST("/novels/:id/progress", handlers.UpdateNovelProgress)
		api.POST("/novels/:id/like", handlers.LikeNovel)

		// Comments (stored as JSON array per content_id)
		api.GET("/content/:id/comments", handlers.GetComments)
		api.POST("/content/:id/comments", handlers.AddComment)

		// Categories
		api.GET("/categories", handlers.GetCategories)
		api.GET("/categories/l1", handlers.GetL1Categories)

		// Feedback
		api.POST("/feedback", handlers.SubmitFeedback)

		// Puzzles
		api.GET("/puzzles", handlers.GetPuzzles)
		api.POST("/puzzles/:id/validate", handlers.ValidatePuzzle)
		api.POST("/puzzles/:id/like", handlers.LikePuzzle)

		// TTS — Text-to-Speech via LocalTTS Docker container
		api.POST("/tts", handlers.GenerateTTS)

		// Profile (device-based, no auth — UUID identifies the device)
		api.GET("/profile", handlers.GetProfile)
		api.POST("/profile", handlers.CreateOrUpdateProfile)

		// Device Info (device-specific metadata, separate from user profile)
		api.GET("/device-info", handlers.GetDeviceInfo)
		api.POST("/device-info", handlers.SubmitDeviceInfo)
	}

	return r
}
