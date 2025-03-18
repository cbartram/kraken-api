package handlers

import (
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
)

type DiscordRequestHandler struct{}

// HandleRequest Handles the /api/v1/discord/oauth route which the service calls to trade a code for an OAuth
// access token.
func (h *DiscordRequestHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody map[string]string
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	code := reqBody["code"]
	redirectUri := reqBody["redirectUri"]

	if reqBody["code"] == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "access code: 'code' is required",
		})
		return
	}

	log.Infof("exchanging code: %s for oauth access token", code)
	token, err := w.DiscordService.ExchangeCodeForToken(code, redirectUri)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": fmt.Sprintf("failed to exchange code: %v", err),
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
}
