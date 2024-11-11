package src

import (
	"context"
	ginadapter "github.com/awslabs/aws-lambda-go-api-proxy/gin"
	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
	"kraken-api/src/handlers"
	"kraken-api/src/handlers/cognito"
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

	r := gin.New()

	gin.DefaultWriter = logger.Writer()
	gin.DefaultErrorWriter = logger.Writer()
	gin.SetMode(gin.ReleaseMode)

	r.Use(LogrusMiddleware(logger))

	api := r.Group("/api/v1")
	cog := api.Group("/cognito")
	plugin := api.Group("/plugin")

	api.POST("/discord/oauth", func(c *gin.Context) {
		handler := handlers.DiscordRequestHandler{}
		handler.HandleRequest(c, ctx)
	})

	plugin.POST("/create-presigned-url", func(c *gin.Context) {
		handler := handlers.PluginPresignedUrlHandler{}
		handler.HandleRequest(c, ctx)
	})

	plugin.POST("/validate-license", func(c *gin.Context) {
		handler := handlers.PluginValidateLicenseHandler{}
		handler.HandleRequest(c, ctx)
	})

	cog.POST("/create-user", func(c *gin.Context) {
		handler := cognito.CognitoCreateUserRequestHandler{}
		handler.HandleRequest(c, ctx)
	})

	cog.POST("/auth", func(c *gin.Context) {
		handler := cognito.CognitoAuthHandler{}
		handler.HandleRequest(c, ctx)
	})

	cog.POST("/refresh-session", func(c *gin.Context) {
		handler := cognito.CognitoRefreshSessionHandler{}
		handler.HandleRequest(c, ctx)
	})

	cog.GET("/get-user", func(c *gin.Context) {
		handler := cognito.CognitoGetUserHandler{}
		handler.HandleRequest(c, ctx)
	})

	cog.PUT("/user-status", func(c *gin.Context) {
		handler := cognito.CognitoUserStatusHandler{}
		handler.HandleRequest(c, ctx)
	})

	return ginadapter.New(r)
}
