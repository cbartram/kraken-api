package cognito

import (
	"github.com/gin-gonic/gin"
	"kraken-api/src/handlers"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
)

type AuthHandler struct{}

// HandleRequest Authenticates that a refresh token is valid for a given user id. This returns the entire
// user object with a refreshed access token.
func (h *AuthHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
	log := handlers.GetLoggerWithTrace(c, w.Logger)
	tmp, exists := c.Get("user")
	if !exists {
		log.Errorf("user not found in context")
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not found in context"})
		return
	}

	c.JSON(http.StatusOK, tmp.(*model.User))
}
