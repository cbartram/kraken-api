package cognito

import (
	"encoding/json"
	"io"
	"kraken-api/src/handlers"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
)

type JagexLinkHandler struct{}

type JagexLink struct {
	JagexCharacterId string `json:"jagexCharacterId"`
	JagexSessionId   string `json:"jagexSessionId"`
	JagexDisplayName string `json:"jagexDisplayName"`
	Ip               string `json:"ip"`
}

func (j *JagexLinkHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
	log := handlers.GetLoggerWithTrace(c, w.Logger)
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody JagexLink
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	user, exists := c.Get("user")
	if !exists {
		log.Errorf("user not found in context")
		c.JSON(http.StatusInternalServerError, gin.H{"error": "user not found in context"})
		return
	}

	userModel, ok := user.(*model.User)
	if !ok {
		log.Errorf("failed to cast user to User model")
		c.JSON(http.StatusInternalServerError, gin.H{"error": "internal server error"})
		return
	}

	userModel.JagexCharacterId = reqBody.JagexCharacterId
	userModel.JagexSessionId = reqBody.JagexSessionId
	userModel.JagexDisplayName = reqBody.JagexDisplayName
	userModel.Ip = reqBody.Ip
	userModel.LastClientLoginTime = time.Now()

	if err := w.Database.Save(userModel).Error; err != nil {
		log.Errorf("failed to update user during jagex acct link: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to update user: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "ok",
	})
}
