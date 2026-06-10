package handlers

import (
	"encoding/json"
	"github.com/gin-gonic/gin"
)

// jsonResponse renders JSON without escaping Unicode characters.
// Go's default json.Marshal escapes non-ASCII characters as \uXXXX,
// which breaks non-English text (Hindi, Urdu, etc.) on the client.
func jsonResponse(c *gin.Context, status int, v interface{}) {
	c.Writer.Header().Set("Content-Type", "application/json; charset=utf-8")
	c.Writer.WriteHeader(status)
	enc := json.NewEncoder(c.Writer)
	enc.SetEscapeHTML(false)
	enc.Encode(v)
}
