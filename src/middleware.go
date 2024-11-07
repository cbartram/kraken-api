package src

import (
	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
	"kraken-api/src/client"
	"kraken-api/src/model"
	"net/http"
)

func DiscordMiddleware(c *gin.Context) {
	discordClient, err := client.MakeDiscordClient()
	if err != nil {
		c.JSON(http.StatusInternalServerError, model.ErrorResponse{
			Message: "failed to create discord client: " + err.Error(),
			Status:  "error",
		})
	}

	logrus.Info("created discord client")
	c.Set("discord-client", discordClient)
	c.Next()
}

func LogrusMiddleware(logger *logrus.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Next()
		logger.WithFields(logrus.Fields{
			"user-agent": c.Request.UserAgent(),
			"error":      c.Errors.ByType(gin.ErrorTypePrivate).String(),
		}).Infof("[%s] %s: ", c.Request.Method, c.Request.URL.Path)
	}
}
