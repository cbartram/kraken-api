package cognito

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
)

type CognitoCreateUserRequestHandler struct{}

// HandleRequest This method handles the creation of a new cognito user after the user has finished the discord
// OAuth flow. It will return a Cognito refresh token AND access token which will be used by the Kraken service to authenticate a user
// in subsequent runs. In subsequent runs a user who is attempting to authenticate must use their refresh token to gain
// an access token.
func (h *CognitoCreateUserRequestHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody model.CognitoCreateUserRequest
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	authManager := service.MakeCognitoService()

	// We want to assert that the user does not exist before we create it.
	user, _ := authManager.GetUser(ctx, &reqBody.DiscordID)
	if user == nil {
		creds, err := authManager.CreateCognitoUser(ctx, &reqBody)
		if err != nil {
			log.Errorf("error while creating new cognito user: %s", err)
			c.JSON(http.StatusInternalServerError, gin.H{
				"error": "error while creating new cognito user:" + err.Error(),
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
				RefreshToken:    *creds.RefreshToken,
				AccessToken:     *creds.AccessToken,
				TokenExpiration: creds.ExpiresIn,
				IdToken:         *creds.IdToken,
			},
		})
	} else {
		// User already exists.
		log.Infof("user already exists, re-enabling and refreshing session")
		authManager.EnableUser(ctx, reqBody.DiscordID)
		creds, err := authManager.RefreshSession(ctx, reqBody.DiscordID)
		if err != nil {
			log.Errorf("error: failed to refresh existing user session: " + err.Error())
			c.JSON(http.StatusBadRequest, gin.H{
				"error": fmt.Sprintf("user with discord id: %s already exists. failed to refresh session", reqBody.DiscordID),
			})
			return
		}

		c.JSON(http.StatusOK, model.CognitoUser{
			DiscordUsername: reqBody.DiscordUsername,
			Email:           reqBody.DiscordEmail,
			DiscordID:       reqBody.DiscordID,
			AccountEnabled:  true,
			Credentials:     *creds,
		})
	}
}
