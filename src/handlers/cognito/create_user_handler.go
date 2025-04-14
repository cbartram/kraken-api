package cognito

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"github.com/stripe/stripe-go/v81"
	"github.com/stripe/stripe-go/v81/customer"
	"io"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
	"time"
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
		c.JSON(http.StatusBadRequest, gin.H{"error": "could not read body from request: " + err.Error()})
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

		cust, err := customer.New(&stripe.CustomerParams{
			Params: stripe.Params{},
			Email:  &reqBody.DiscordEmail,
			Metadata: map[string]string{
				"discord-id":       reqBody.DiscordID,
				"discord-username": reqBody.DiscordUsername,
			},
			Name: &reqBody.DiscordUsername,
		})

		if err != nil {
			log.Errorf("error while creating new stripe customer: %v", err)
			c.JSON(http.StatusInternalServerError, gin.H{
				"error": "error while creating new stripe user: " + err.Error(),
			})
			return
		}

		// Users signing up for the first time through the UI in chrome won't have a hardware id
		// so this creates a tmp one for them. When the user signs in via the kraken client it will
		// update the hardware id from temp.
		var hardwareId string
		if len(reqBody.HardwareID) == 0 {
			log.Infof("no hardware id provided, creating user with temp hardware id")
			hardwareId = "temp"
		} else {
			hardwareId = reqBody.HardwareID
		}

		tx := w.Database.Begin()
		newUser := model.User{
			DiscordUsername:  reqBody.DiscordUsername,
			Email:            reqBody.DiscordEmail,
			DiscordID:        reqBody.DiscordID,
			AvatarId:         reqBody.AvatarID,
			CustomerId:       cust.ID,
			JagexCharacterId: "",
			JagexSessionId:   "",
			JagexDisplayName: "",
			Credentials: model.CognitoCredentials{
				RefreshToken:    *creds.RefreshToken,
				AccessToken:     *creds.AccessToken,
				TokenExpiration: creds.ExpiresIn,
				IdToken:         *creds.IdToken,
			},
			HardwareIDs: []model.HardwareID{
				{Value: hardwareId},
			},
			Plugins: []model.Plugin{},
			// These Fields will be updated by the /free-trial endpoint
			UsedFreeTrial:      false,
			FreeTrialStartTime: time.Now(),
			FreeTrialEndTime:   time.Now(),
		}

		createUser := w.Database.Create(&newUser)
		if createUser.Error != nil {
			log.Errorf("error while creating new user: %v", tx.Error)
			c.JSON(http.StatusInternalServerError, gin.H{
				"error": "error while creating new user: " + tx.Error.Error(),
			})
			tx.Rollback()
			return
		}

		tx.Commit()
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

		// TODO This is ripe for abuse. Need a separate route for adding hardware ids in the future.
		if len(reqBody.HardwareID) != 0 {
			for i := range user.HardwareIDs {
				if user.HardwareIDs[i].Value == "temp" {
					log.Infof("found temp hardware id, updating with actual value: %s", reqBody.HardwareID)
					// Update the in-memory value
					user.HardwareIDs[i].Value = reqBody.HardwareID
					tx := w.Database.Save(&user.HardwareIDs[i])
					if tx.Error != nil {
						log.Errorf("error while saving hardware ID: %v", tx.Error)
						c.JSON(http.StatusInternalServerError, gin.H{
							"error": "error while saving hardware ID: " + tx.Error.Error(),
						})
						return
					}
				}
			}
		}

		user.Credentials = model.CognitoCredentials{
			RefreshToken:    creds.RefreshToken,
			AccessToken:     creds.AccessToken,
			IdToken:         creds.IdToken,
			TokenExpiration: creds.TokenExpiration,
		}
		tx := w.Database.Save(&user)
		if tx.Error != nil {
			log.Errorf("error while saving existing user: %v", tx.Error)
			c.JSON(http.StatusInternalServerError, gin.H{
				"error": "error while saving existing user: " + tx.Error.Error(),
			})
			return
		}

		c.JSON(http.StatusOK, user)
	}
}
