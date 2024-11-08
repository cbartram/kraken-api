package handlers

import (
	"context"
	"fmt"
	"github.com/gin-gonic/gin"
	"kraken-api/src/client"
	"kraken-api/src/model"
	"net/http"
)

type CognitoGetUserHandler struct{}

// HandleRequest Retrieves a user from Cognito.
func (h *CognitoGetUserHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	// TODO Parse the discord id from req query params

	authManager := client.MakeCognitoAuthManager()
	cognitoUser, err := authManager.GetUser(ctx, &reqBody.DiscordID)

	if err == nil {
		c.JSON(http.StatusOK, cognitoUser)
	} else {
		c.JSON(http.StatusNotFound, model.ErrorResponse{
			Message: fmt.Sprintf("user with id: %s does not exist", reqBody.DiscordID),
			Status:  "error",
		})
	}
}
