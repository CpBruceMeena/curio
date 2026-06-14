package handlers

import (
	"bytes"
	"encoding/json"
	"io"
	"net/http"
	"strconv"
	"time"

	"github.com/curio/backend/database"
	"github.com/curio/backend/models"
	"github.com/gin-gonic/gin"
)

// TTSRequest is the JSON payload from the Android client.
type TTSRequest struct {
	ContentID   uint   `json:"content_id"`
	Text        string `json:"text"`
	Voice       string `json:"voice"`
}

// EdgeTTSRequest is the format expected by the LocalTTS (travisvn/openai-edge-tts) container.
type EdgeTTSRequest struct {
	Model string `json:"model"`
	Input string `json:"input"`
	Voice string `json:"voice"`
}

var defaultVoice = "en-US-AriaNeural"

// voiceByCategory maps content category IDs to the most appropriate Edge TTS voice.
// Voices are selected per category to match content type, language, and mood:
//   - Facts/educational → warm, clear narrators (Aria)
//   - Poetry → expressive, melodic voices (Jenny, Sonia, Swara, Salman)
//   - Stories → engaging storytellers (Aria, Jenny, Guy, Sonia)
//   - Puzzles → precise, calm voices (Davis)
var voiceByCategory = map[uint]string{
	// ── Facts (educational, warm, clear) ────────────────────────
	1:  "en-US-AriaNeural",   // Science
	2:  "en-US-AriaNeural",   // Space
	3:  "en-US-AriaNeural",   // History
	4:  "en-US-AriaNeural",   // Biology
	5:  "en-US-AriaNeural",   // Psychology
	6:  "en-US-AriaNeural",   // Philosophy
	7:  "en-US-AriaNeural",   // Physics
	8:  "en-US-AriaNeural",   // Startups
	9:  "en-US-AriaNeural",   // AI
	10: "en-US-AriaNeural",   // Economics
	11: "en-US-AriaNeural",   // Nature
	12: "en-US-AriaNeural",   // Technology
	14: "en-US-AriaNeural",   // Movies
	15: "en-US-AriaNeural",   // Neuroscience
	16: "en-US-AriaNeural",   // Literature
	17: "en-US-AriaNeural",   // Geography
	18: "en-US-AriaNeural",   // Music
	19: "en-US-AriaNeural",   // Sports
	20: "en-US-AriaNeural",   // Food

	// ── Poetry & Literary (expressive, melodic) ─────────────────
	13: "en-US-JennyNeural",  // English Poems — expressive, conversational
	21: "ur-IN-SalmanNeural", // Shayari — Urdu male voice, traditional feel
	24: "hi-IN-SwaraNeural",  // Hindi Poems — Hindi female, warm and clear
	25: "en-GB-SoniaNeural",  // Classics — British English, fits classic literature
	26: "en-US-JennyNeural",  // Modern — expressive, contemporary

	// ── Short Stories (narrative, engaging) ─────────────────────
	23: "en-US-AriaNeural",   // Short Stories — warm storyteller
	31: "en-GB-SoniaNeural",  // Classic Fiction — British, perfect for classic lit
	32: "en-US-JennyNeural",  // Micro Stories — engaging for short reads
	33: "en-US-GuyNeural",    // Serialized Stories — authoritative, keeps you hooked
	105: "hi-IN-SwaraNeural", // Hindi Stories — Hindi female voice, warm and clear

	// ── Puzzles (calm, precise, clear) ─────────────────────────
	22: "en-US-DavisNeural",  // Mixed Puzzles — calm, balanced
	27: "en-US-DavisNeural",  // Sudoku — precise, clear
	28: "en-US-DavisNeural",  // Math Puzzles — clear enunciation
	29: "en-US-DavisNeural",  // Logic Puzzles — articulate
	30: "en-US-JennyNeural",  // Word Puzzles — conversational, word-play friendly
}

// GenerateTTS handles POST /api/v1/tts
// Accepts either:
//   { "text": "some text", "voice": "en-US-JennyNeural" }
//   { "content_id": 12345, "voice": "en-US-JennyNeural" }
//
// Proxies to the LocalTTS container at http://localhost:5050/v1/audio/speech
// and streams the MP3 response back to the client.
func GenerateTTS(c *gin.Context) {
	var req TTSRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "Invalid request: " + err.Error()})
		return
	}

	voice := req.Voice

	// Determine the text to synthesize
	text := req.Text
	if text == "" && req.ContentID > 0 {
		// Global ID = categoryID * 10_000_000 + localID
		// IDs in per-category tables are already stored as global IDs
		catID := req.ContentID / 10000000

		var content models.Content

		if catID > 0 {
			// Query the correct per-category table using the global ID directly
			var category models.Category
			if err := database.DB.First(&category, catID).Error; err != nil {
				jsonResponse(c, http.StatusNotFound, gin.H{"error": "Category not found"})
				return
			}
			tableName := database.ContentTableName(uint(category.ContentTableID), catID)
			result := database.DB.Table(tableName).First(&content, req.ContentID)
			if result.Error != nil {
				jsonResponse(c, http.StatusNotFound, gin.H{"error": "Content not found"})
				return
			}
			content.CategoryID = catID
		}

		// Auto-select voice based on content category
		if voice == "" {
			if mapped, ok := voiceByCategory[content.CategoryID]; ok {
				voice = mapped
			}
		}

		// Combine title + body for a richer audio experience
		text = content.Title + ". " + content.Body
	}

	if voice == "" {
		voice = defaultVoice
	}

	if text == "" {
		jsonResponse(c, http.StatusBadRequest, gin.H{"error": "No text or content_id provided"})
		return
	}

	// Prepare request for LocalTTS container
	edgeReq := EdgeTTSRequest{
		Model: "edge-tts",
		Input: text,
		Voice: voice,
	}

	bodyBytes, err := json.Marshal(edgeReq)
	if err != nil {
		jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to marshal request"})
		return
	}

	// Call the LocalTTS Docker container with a timeout
	client := &http.Client{Timeout: 30 * time.Second}
	resp, err := client.Post(
		"http://localhost:5050/v1/audio/speech",
		"application/json",
		bytes.NewReader(bodyBytes),
	)
	if err != nil {
		jsonResponse(c, http.StatusInternalServerError, gin.H{"error": "Failed to connect to TTS service: " + err.Error()})
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		jsonResponse(c, resp.StatusCode, gin.H{"error": "TTS service error: " + string(body)})
		return
	}

	// Stream the MP3 audio back to the client
	c.Writer.Header().Set("Content-Type", "audio/mpeg")
	c.Writer.Header().Set("Content-Length", strconv.FormatInt(resp.ContentLength, 10))
	c.Writer.WriteHeader(http.StatusOK)
	io.Copy(c.Writer, resp.Body)
}
