package src

import (
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"kraken-api/src/client"
	"net/http"
)

func DiscordMiddleware(c *gin.Context) {
	discordClient, err := client.MakeDiscordClient()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "failed to create discord client: " + err.Error(),
		})
	}

	log.Info("created discord client")
	c.Set("discord-client", discordClient)
	c.Next()
}

func LogrusMiddleware(logger *log.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Next()
		logger.WithFields(log.Fields{
			"user-agent": c.Request.UserAgent(),
			"error":      c.Errors.ByType(gin.ErrorTypePrivate).String(),
		}).Infof("[%s] %s: ", c.Request.Method, c.Request.URL.Path)
	}
}
