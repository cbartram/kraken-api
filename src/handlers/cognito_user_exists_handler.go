package handlers

import (
	"context"
	"github.com/gin-gonic/gin"
	"kraken-api/src/client"
	"net/http"
)

type CognitoUserExistsHandler struct{}

// HandleRequest Checks if the user exists and is enabled.
func (h *CognitoUserExistsHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	discordID := c.Query("discordId")
	if discordID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "discordId query parameter is required",
		})
		return
	}

	authManager := client.MakeCognitoAuthManager()
	userExists, userEnabled := authManager.DoesUserExist(ctx, discordID)

	c.JSON(http.StatusOK, gin.H{
		"userExists":  userExists,
		"userEnabled": userEnabled,
	})
}
