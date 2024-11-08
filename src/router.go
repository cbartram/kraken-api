package src

import (
	"context"
	ginadapter "github.com/awslabs/aws-lambda-go-api-proxy/gin"
	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
	"kraken-api/src/handlers"
	"log"
	"os"
)

func MakeRouter(ctx context.Context) *ginadapter.GinLambda {
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
		handler := handlers.DiscordRequestHandler{}
		handler.HandleRequest(c, ctx)
	})

	r.POST("/api/v1/cognito/create-user", func(c *gin.Context) {
		handler := handlers.CognitoCreateUserRequestHandler{}
		handler.HandleRequest(c, ctx)
	})

	r.POST("/api/v1/cognito/auth", func(c *gin.Context) {
		handler := handlers.CognitoAuthHandler{}
		handler.HandleRequest(c, ctx)
	})

	r.GET("/api/v1/cognito/get-user", func(c *gin.Context) {
		handler := handlers.CognitoGetUserHandler{}
		handler.HandleRequest(c, ctx)
	})

	r.GET("/api/v1/cognito/user-exists", func(c *gin.Context) {
		handler := handlers.CognitoUserExistsHandler{}
		handler.HandleRequest(c, ctx)
	})

	r.PUT("/api/v1/cognito/user-status", func(c *gin.Context) {
		handler := handlers.CognitoUserStatusHandler{}
		handler.HandleRequest(c, ctx)
	})

	return ginadapter.New(r)
}
