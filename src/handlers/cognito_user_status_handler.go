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

type CognitoUserStatusHandler struct{}

// HandleRequest Changes a user's status to disabled or enabled.
func (h *CognitoUserStatusHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, model.ErrorResponse{Message: "could not read body from request: " + err.Error(), Status: "error"})
		return
	}

	var reqBody model.CognitoUserStatusRequest
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, model.ErrorResponse{Message: "invalid request body: " + err.Error(), Status: "error"})
		return
	}

	authManager := client.MakeCognitoAuthManager()

	if reqBody.AccountEnabled {
		ok := authManager.EnableUser(ctx, reqBody.DiscordID)
		if !ok {
			log.Errorf("failed to enable user.")
			c.JSON(http.StatusInternalServerError, model.ErrorResponse{
				Message: "failed to enable user with cognito",
				Status:  "error",
			})
			return
		}
	} else {
		ok := authManager.DisableUser(ctx, reqBody.DiscordID)
		if !ok {
			log.Errorf("failed to disable user.")
			c.JSON(http.StatusInternalServerError, model.ErrorResponse{
				Message: "failed to disable user with cognito",
				Status:  "error",
			})
			return
		}
	}

	out := map[string]bool{}
	out["accountEnabled"] = reqBody.AccountEnabled
	c.JSON(http.StatusOK, out)
}
