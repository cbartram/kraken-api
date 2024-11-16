package cognito

import (
	"context"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"kraken-api/src/service"

	"net/http"
)

type CognitoGetUserHandler struct{}

// HandleRequest Retrieves a user from Cognito.
func (h *CognitoGetUserHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	discordID := c.Query("discordId")
	if discordID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "discordId query parameter is required",
		})
		return
	}

	authManager := service.MakeCognitoService()
	log.Infof("retrieving user with id: %s from cognito", discordID)

	// Note: This method does not return credentials with the user
	cognitoUser, err := authManager.GetUser(ctx, &discordID)

	if err == nil {
		c.JSON(http.StatusOK, cognitoUser)
	} else {
		c.JSON(http.StatusNotFound, gin.H{
			"error": fmt.Sprintf("user with id: %s does not exist", discordID),
		})
	}
}
