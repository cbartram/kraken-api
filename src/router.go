package src

import (
	"context"
	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
	"kraken-api/src/handlers"
	"kraken-api/src/handlers/cognito"
	"kraken-api/src/handlers/plugin"
	"kraken-api/src/service"
	"log"
	"net/http"
	"os"
)

func NewRouter(w *service.Wrapper) *gin.Engine {
	ctx := context.Background()
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

	r.Use(CORSMiddleware(), LogrusMiddleware(logger))

	apiGroup := r.Group("/api/v1")
	userGroup := apiGroup.Group("/user")
	pluginGroup := apiGroup.Group("/plugin", AuthMiddleware(w))

	apiGroup.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"api-version": os.Getenv("API_VERSION"),
		})
	})

	apiGroup.POST("/discord/oauth", func(c *gin.Context) {
		h := handlers.DiscordRequestHandler{}
		h.HandleRequest(c, w)
	})

	apiGroup.GET("/client-bootstrap", func(c *gin.Context) {
		h := handlers.ClientBootstrapHandler{}
		h.HandleRequest(c, ctx, w)
	})

	pluginGroup.POST("/create-presigned-url", func(c *gin.Context) {
		h := plugin.PresignedUrlHandler{}
		h.HandleRequest(c, ctx, w)
	})

	pluginGroup.POST("/purchase", func(c *gin.Context) {
		h := plugin.PurchaseHandler{}
		h.HandleRequest(c, w)
	})

	pluginGroup.POST("/validate", func(c *gin.Context) {
		h := plugin.ValidatePluginHandler{}
		h.HandleRequest(c, ctx, w)
	})

	userGroup.POST("/create", func(c *gin.Context) {
		h := cognito.CreateUserRequestHandler{}
		h.HandleRequest(c, ctx, w)
	})

	userGroup.GET("/", AuthMiddleware(w), func(c *gin.Context) {
		h := cognito.AuthHandler{}
		h.HandleRequest(c, ctx, w)
	})

	return r
}
