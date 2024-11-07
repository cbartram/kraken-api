package src

import (
	ginadapter "github.com/awslabs/aws-lambda-go-api-proxy/gin"
	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
	"kraken-api/src/handlers"
	"log"
	"os"
)

func MakeRouter() *ginadapter.GinLambda {
	logger := logrus.New()
	logger.SetFormatter(&logrus.TextFormatter{
		FullTimestamp: false,
	})

	logLevel, err := logrus.ParseLevel(os.Getenv("LOG_LEVEL"))
	if err != nil {
		logLevel = logrus.InfoLevel
	}

	log.SetOutput(os.Stdout)
	logrus.SetLevel(logLevel)
	logrus.Infof("log Level set to: %s", logLevel)

	r := gin.New()

	gin.DefaultWriter = logger.Writer()
	gin.DefaultErrorWriter = logger.Writer()

	r.Use(LogrusMiddleware(logger))
	r.POST("/api/v1/discord/oauth", DiscordMiddleware, func(c *gin.Context) {
		handlers.HandleDiscordOAuth(c)
	})

	return ginadapter.New(r)
}
