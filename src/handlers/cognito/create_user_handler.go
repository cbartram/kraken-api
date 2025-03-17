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

type CreateUserRequestHandler struct{}

// HandleRequest This method handles the creation of a new cognito user after the user has finished the discord
// OAuth flow. It will return a Cognito refresh token AND access token which will be used by the Kraken service to authenticate a user
// in subsequent runs. In subsequent runs a user who is attempting to authenticate must use their refresh token to gain
// an access token.
func (h *CreateUserRequestHandler) HandleRequest(c *gin.Context, ctx context.Context, w *service.Wrapper) {
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

	// We want to assert that the user does not exist before we create it.
	user, err := model.GetUser(reqBody.DiscordID, w.Database)

	if err != nil {
		log.Infof("no user found with id: %s, creating user", reqBody.DiscordID)
		creds, err := w.CognitoService.CreateCognitoUser(ctx, &reqBody)
		if err != nil {
			log.Errorf("error while creating new cognito user: %s", err)
			c.JSON(http.StatusInternalServerError, gin.H{
				"error": "error while creating new cognito user:" + err.Error(),
			})
			return
		}

		newUser := model.User{
			DiscordUsername: reqBody.DiscordUsername,
			Email:           reqBody.DiscordEmail,
			DiscordID:       reqBody.DiscordID,
			Credentials: model.CognitoCredentials{
				RefreshToken:    *creds.RefreshToken,
				AccessToken:     *creds.AccessToken,
				TokenExpiration: creds.ExpiresIn,
				IdToken:         *creds.IdToken,
			},
			HardwareIDs: []model.HardwareID{
				{Value: reqBody.HardwareID},
			},
			Plugins: []model.Plugin{},
		}

		tx := w.Database.Create(&newUser)
		if tx.Error != nil {
			log.Errorf("error while creating new user: %v", tx.Error)
			c.JSON(http.StatusInternalServerError, gin.H{
				"error": "error while creating new user: " + tx.Error.Error(),
			})
			return
		}

		c.JSON(http.StatusOK, newUser)
	} else {
		log.Infof("user already exists, refreshing session")
		creds, err := w.CognitoService.RefreshSession(ctx, reqBody.DiscordID)
		if err != nil {
			log.Errorf("error: failed to refresh existing user session: " + err.Error())
			c.JSON(http.StatusBadRequest, gin.H{
				"error": fmt.Sprintf("user with discord id: %s already exists. failed to refresh session", reqBody.DiscordID),
			})
			return
		}

		user.Credentials = model.CognitoCredentials{
			RefreshToken:    creds.RefreshToken,
			AccessToken:     creds.AccessToken,
			IdToken:         creds.IdToken,
			TokenExpiration: creds.TokenExpiration,
		}

		c.JSON(http.StatusOK, user)
	}
}
