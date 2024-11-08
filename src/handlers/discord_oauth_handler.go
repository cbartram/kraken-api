package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/client"
	"kraken-api/src/model"
	"net/http"
)

type DiscordRequestHandler struct{}

// HandleRequest Handles the /api/v1/discord-oauth route which the client calls to trade a code for an OAuth
// access token.
func (h *DiscordRequestHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	discClient, exists := c.Get("discord-client")
	if !exists {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "failed to get discord client from request context",
		})
		return
	}

	if discordClient, ok := discClient.(*client.DiscordClient); ok {
		bodyRaw, err := io.ReadAll(c.Request.Body)
		if err != nil {
			log.Errorf("could not read body from request: %s", err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
			return
		}

		var reqBody model.DiscordOAuthRequest
		if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
			return
		}

		if reqBody.Code == "" {
			c.JSON(http.StatusBadRequest, gin.H{
				"error": "access code: 'code' is required",
			})
			return
		}

		log.Infof("exchanging code: %s for oauth access token", reqBody.Code)
		token, err := discordClient.ExchangeCodeForToken(reqBody.Code)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{
				"error": fmt.Sprintf("Failed to exchange code: %v", err),
			})
			return
		}

		c.JSON(http.StatusOK, model.DiscordTokenResponse{
			AccessToken:  token.AccessToken,
			TokenType:    token.TokenType,
			ExpiresIn:    token.ExpiresIn,
			RefreshToken: token.RefreshToken,
			Scope:        token.Scope,
		})
	} else {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "invalid type cast to discord client",
		})
	}
}
