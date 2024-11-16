package cognito

import (
	"context"
	"encoding/json"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
)

type CognitoRefreshSessionHandler struct{}

// HandleRequest Authenticates that a refresh token is valid for a given user id. This returns the entire
// user object with a refreshed access token.
func (h *CognitoRefreshSessionHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody model.CognitoAuthRequest
	if err = json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	if reqBody.DiscordID == "" {
		log.Errorf("error: discord id '%s' missing from request body: ", reqBody.DiscordID)
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: discordId or refreshToken missing."})
		return
	}

	authManager := service.MakeCognitoService()
	log.Infof("authenticating user with discord id: %s", reqBody.DiscordID)
	creds, err := authManager.RefreshSession(ctx, reqBody.RefreshToken)

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "error: failed to refresh user session: " + err.Error(),
		})
	}

	log.Infof("user auth ok")
	c.JSON(http.StatusOK, creds)
}
