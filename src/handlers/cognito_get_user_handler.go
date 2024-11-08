package handlers

import (
	"context"
	"fmt"
	"github.com/gin-gonic/gin"
	"kraken-api/src/client"
	"net/http"
)

type CognitoGetUserHandler struct{}

// HandleRequest Retrieves a user from Cognito.
func (h *CognitoGetUserHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	discordID := c.Query("discordId")
	if discordID == "" {
		c.JSON(400, gin.H{
			"error": "discordId query parameter is required",
		})
		return
	}

	authManager := client.MakeCognitoAuthManager()
	cognitoUser, err := authManager.GetUser(ctx, &discordID)

	if err == nil {
		c.JSON(http.StatusOK, cognitoUser)
	} else {
		c.JSON(http.StatusNotFound, gin.H{
			"error": fmt.Sprintf("user with id: %s does not exist", discordID),
		})
	}
}
