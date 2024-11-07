package handlers

import (
	"context"
	"encoding/json"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/client"
	"kraken-api/src/model"
	"net/http"
)

type CognitoAuthHandler struct{}

func (h *CognitoAuthHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, model.ErrorResponse{Message: "could not read body from request: " + err.Error(), Status: "error"})
		return
	}

	var reqBody model.CognitoUser
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, model.ErrorResponse{Message: "invalid request body: " + err.Error(), Status: "error"})
		return
	}

	authManager := client.MakeCognitoAuthManager()
	isAuth, cognitoUser := authManager.AuthUser(ctx, &reqBody.Credentials.RefreshToken, &reqBody.DiscordID)

	// Note: This also has checked that the user account in cognito is enabled.
	if isAuth {
		c.JSON(http.StatusOK, cognitoUser)
	} else {
		c.JSON(http.StatusUnauthorized, model.ErrorResponse{
			Message: "user unauthorized",
			Status:  "error",
		})
	}
}
