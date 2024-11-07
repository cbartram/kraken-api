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
		c.JSON(http.StatusInternalServerError, model.ErrorResponse{
			Message: "failed to get discord client from request context",
			Status:  "error",
		})
		return
	}

	if discordClient, ok := discClient.(*client.DiscordClient); ok {
		bodyRaw, err := io.ReadAll(c.Request.Body)
		if err != nil {
			log.Errorf("could not read body from request: %s", err)
			c.JSON(http.StatusInternalServerError, model.ErrorResponse{Message: "could not read body from request: " + err.Error(), Status: "error"})
			return
		}

		var reqBody model.DiscordOAuthRequest
		if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
			c.JSON(http.StatusBadRequest, model.ErrorResponse{Message: "invalid request body: " + err.Error(), Status: "error"})
			return
		}

		if reqBody.Code == "" {
			c.JSON(http.StatusBadRequest, model.ErrorResponse{
				Message: "access code: 'code' is required",
				Status:  "error",
			})
			return
		}

		log.Infof("exchanging code: %s for oauth access token", reqBody.Code)
		token, err := discordClient.ExchangeCodeForToken(reqBody.Code)
		if err != nil {
			c.JSON(http.StatusInternalServerError, model.ErrorResponse{
				Message: fmt.Sprintf("Failed to exchange code: %v", err),
				Status:  "error",
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
		c.JSON(http.StatusInternalServerError, model.ErrorResponse{
			Message: "invalid type cast to discord client",
			Status:  "error",
		})
	}
}
