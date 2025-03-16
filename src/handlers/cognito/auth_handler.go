package cognito

import (
	"context"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
)

type AuthHandler struct{}

// HandleRequest Authenticates that a refresh token is valid for a given user id. This returns the entire
// user object with a refreshed access token.
func (h *AuthHandler) HandleRequest(c *gin.Context, ctx context.Context, wrapper *service.Wrapper) {
	tmp, exists := c.Get("user")
	if !exists {
		log.Errorf("user not found in context")
		c.JSON(http.StatusInternalServerError, gin.H{"error": "user not found in context"})
		return
	}

	user := tmp.(*model.User)

	log.Infof("authenticating user with discord id: %s", user.DiscordID)
	user, err := wrapper.CognitoService.AuthUser(ctx, &user.Credentials.RefreshToken, &user.DiscordID, wrapper.Database)
	if err != nil {
		log.Errorf("user is unauthorized: %v", err)
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "user unauthorized",
		})
		return
	}

	c.JSON(http.StatusOK, user)
}
