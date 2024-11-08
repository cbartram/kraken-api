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

type CognitoUserExistsHandler struct{}

// HandleRequest Checks if the user exists and is enabled.
func (h *CognitoUserExistsHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, model.ErrorResponse{Message: "could not read body from request: " + err.Error(), Status: "error"})
		return
	}

	var reqBody map[string]string
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, model.ErrorResponse{Message: "invalid request body: " + err.Error(), Status: "error"})
		return
	}

	returnObj := map[string]bool{}
	authManager := client.MakeCognitoAuthManager()
	userExists, userEnabled := authManager.DoesUserExist(ctx, reqBody["discordId"])

	returnObj["userExists"] = userExists
	returnObj["userEnabled"] = userEnabled

	c.JSON(http.StatusOK, returnObj)
}
