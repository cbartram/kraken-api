package cognito

import (
	"context"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
)

type RefreshSessionHandler struct{}

// HandleRequest Authenticates that a refresh token is valid for a given user id. This returns the entire
// user object with a refreshed access token.
func (h *RefreshSessionHandler) HandleRequest(c *gin.Context, ctx context.Context, w *service.Wrapper) {
	tmp, exists := c.Get("user")
	if !exists {
		log.Errorf("user not found in context")
		c.JSON(http.StatusInternalServerError, gin.H{"error": "user not found in context"})
		return
	}

	user := tmp.(*model.User)

	log.Infof("authenticating user with discord id: %s", user.DiscordID)
	creds, err := w.CognitoService.RefreshSession(ctx, user.Credentials.RefreshToken)

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "error: failed to refresh user session: " + err.Error(),
		})
	}

	log.Infof("user auth ok")
	c.JSON(http.StatusOK, creds)
}
