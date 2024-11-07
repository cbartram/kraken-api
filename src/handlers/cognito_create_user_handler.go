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

type CognitoCreateUserRequestHandler struct{}

// HandleRequest This method handles the creation of a new cognito user after the user has finished the discord
// OAuth flow. It will return a Cognito refresh token AND access token which will be used by the Kraken client to authenticate a user
// in subsequent runs. In subsequent runs a user who is attempting to authenticate must use their refresh token to gain
// an access token.
func (h *CognitoCreateUserRequestHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, model.ErrorResponse{Message: "could not read body from request: " + err.Error(), Status: "error"})
		return
	}

	var reqBody model.CognitoCreateUserRequest
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, model.ErrorResponse{Message: "invalid request body: " + err.Error(), Status: "error"})
		return
	}

	authManager := client.MakeCognitoAuthManager()
	exists := authManager.DoesUserExist(ctx, reqBody.DiscordID)

	if !exists {
		refreshToken, err := authManager.CreateCognitoUser(ctx, reqBody.DiscordID, reqBody.DiscordUsername, reqBody.DiscordEmail)
		if err != nil {
			log.Errorf("error while creating new cognito user: %s", err)
			c.JSON(http.StatusInternalServerError, model.ErrorResponse{
				Message: "error while creating new cognito user:" + err.Error(),
				Status:  "error",
			})
			return
		}

		// Note: this does not provide the cognito id. However, users are located via username (discord id) not cognito id.
		c.JSON(http.StatusOK, model.CognitoUser{
			DiscordUsername: reqBody.DiscordUsername,
			Email:           reqBody.DiscordEmail,
			DiscordID:       reqBody.DiscordID,
			AccountEnabled:  true,
			Credentials: model.CognitoCredentials{
				RefreshToken: *refreshToken.RefreshToken,
				AccessToken:  *refreshToken.AccessToken,
			},
		})
	} else {
		c.JSON(http.StatusBadRequest, model.ErrorResponse{
			Message: fmt.Sprintf("user with discord id: %s already exists", reqBody.DiscordID),
			Status:  "error",
		})
	}
}
