package handlers

import (
	"context"
	"crypto/rand"
	"encoding/base64"
	"encoding/hex"
	"fmt"
	"kraken-api/src/service"
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
)

const (
	TraceIDKey    = "trace-id"
	TraceIDHeader = "X-Trace-ID"
)

// GenerateTraceID creates a random trace ID
func GenerateTraceID() string {
	bytes := make([]byte, 4) // 32-bit trace ID
	rand.Read(bytes)
	return hex.EncodeToString(bytes)
}

// TraceIDMiddleware generates or extracts trace ID and adds it to context
func TraceIDMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		traceID := c.GetHeader(TraceIDHeader)

		if traceID == "" {
			traceID = GenerateTraceID()
		}

		c.Set(TraceIDKey, traceID)
		c.Header(TraceIDHeader, traceID)

		ctx := context.WithValue(c.Request.Context(), TraceIDKey, traceID)
		c.Request = c.Request.WithContext(ctx)

		c.Next()
	}
}

func LoggingMiddlewareWithTrace(logger *zap.SugaredLogger) gin.HandlerFunc {
	return func(c *gin.Context) {
		if c.Request.URL.Path != "/api/v1/health" {
			traceID := c.GetString(TraceIDKey)
			logger.Infow("",
				"trace-id", traceID,
				"method", c.Request.Method,
				"path", c.Request.URL.Path,
				"status-code", c.Writer.Status(),
				"user-agent", c.Request.UserAgent(),
				"client-ip", c.ClientIP(),
			)
		}
		c.Next()
	}
}

func GetLoggerWithTrace(c *gin.Context, baseLogger *zap.SugaredLogger) *zap.SugaredLogger {
	traceID := c.GetString(TraceIDKey)
	return baseLogger.With("trace-id", traceID)
}

func CORSMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Writer.Header().Set("Access-Control-Allow-Origin", "*")
		c.Writer.Header().Set("Access-Control-Allow-Credentials", "true")
		c.Writer.Header().Set("Access-Control-Allow-Headers", "Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization, accept, origin, Cache-Control, X-Requested-With")
		c.Writer.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS, GET, PUT, DELETE")

		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}

		c.Next()
	}
}

// AuthMiddleware is the custom authentication middleware that checks the Authorization header to ensure a given
// discord id belong to a given refresh token.
func AuthMiddleware(w *service.Wrapper, skipCache bool) gin.HandlerFunc {
	return func(c *gin.Context) {
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "Authorization header is required"})
			return
		}

		// Parse the Authorization header
		// Expected format: "Basic <base64-encoded discord_id:refresh_token>"
		parts := strings.Split(authHeader, " ")
		if len(parts) != 2 || parts[0] != "Basic" {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "Invalid Authorization header format"})
			return
		}

		decoded, err := base64.StdEncoding.DecodeString(parts[1])
		if err != nil {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "Failed to decode credentials"})
			return
		}

		// Split the decoded credentials into Discord ID and refresh token
		credentials := strings.Split(string(decoded), ":")
		if len(credentials) != 2 {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "Invalid credentials format"})
			return
		}

		discordID := credentials[0]
		refreshToken := credentials[1]

		w.Logger.Infof("authenticating user with discord id: %s", discordID)
		user, err := w.CognitoService.AuthUser(context.Background(), &refreshToken, &discordID, w.Database, skipCache)
		if err != nil {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": fmt.Sprintf("could not authenticate user with refresh token: %s", err)})
			return
		}

		c.Set("user", user)
		c.Next()
	}
}
