package src

import (
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
)

func LogrusMiddleware(logger *log.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		logger.WithFields(log.Fields{
			"user-agent": c.Request.UserAgent(),
			"error":      c.Errors.ByType(gin.ErrorTypePrivate).String(),
		}).Infof("[%s] %s: ", c.Request.Method, c.Request.URL.Path)
		c.Next()
	}
}
