package src

import (
	"context"
	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
	"kraken-api/src/handlers"
	"kraken-api/src/handlers/cognito"
	"kraken-api/src/handlers/payment"
	"kraken-api/src/handlers/plugin"
	"kraken-api/src/handlers/sale"
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
	saleGroup := apiGroup.Group("/sale")
	userGroup := apiGroup.Group("/user")
	pluginGroup := apiGroup.Group("/plugin")
	stripeGroup := apiGroup.Group("/stripe")

	apiGroup.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"api-version": os.Getenv("API_VERSION"),
		})
	})

	saleGroup.POST("/create", AuthMiddleware(w), func(c *gin.Context) {
		h := sale.CreateSaleHandler{}
		h.HandleRequest(c, w)
	})

	saleGroup.GET("/", func(c *gin.Context) {
		h := sale.GetSaleHandler{}
		h.HandleRequest(c, w)
	})

	apiGroup.POST("/discord/oauth", func(c *gin.Context) {
		h := handlers.DiscordRequestHandler{}
		h.HandleRequest(c, w)
	})

	apiGroup.GET("/client-bootstrap", func(c *gin.Context) {
		h := handlers.ClientBootstrapHandler{}
		h.HandleRequest(c, ctx, w)
	})

	apiGroup.POST("/support/send-message", AuthMiddleware(w), func(c *gin.Context) {
		h := handlers.EmailHandler{}
		h.HandleRequest(c)
	})

	stripeGroup.GET("/checkout-session", AuthMiddleware(w), func(c *gin.Context) {
		h := payment.CheckoutSessionHandler{}
		h.HandleRequest(c)
	})

	stripeGroup.POST("/webhook", func(c *gin.Context) {
		h := payment.WebhookHandler{}
		h.HandleRequest(c, w)
	})

	pluginGroup.GET("/", func(c *gin.Context) {
		h := plugin.PluginMetadataHandler{}
		h.HandleRequest(c, w)
	})

	pluginGroup.GET("/pack", func(c *gin.Context) {
		h := plugin.PluginPackHandler{}
		h.HandleRequest(c, w)
	})

	pluginGroup.POST("/free-trial", AuthMiddleware(w), func(c *gin.Context) {
		h := plugin.FreeTrialHandler{}
		h.HandleRequest(c, w)
	})

	pluginGroup.POST("/create-presigned-url", AuthMiddleware(w), func(c *gin.Context) {
		h := plugin.PresignedUrlHandler{}
		h.HandleRequest(c, ctx, w)
	})

	pluginGroup.POST("/purchase", AuthMiddleware(w), func(c *gin.Context) {
		h := plugin.PurchaseHandler{}
		h.HandleRequest(c, w)
	})

	pluginGroup.POST("/purchase-pack", AuthMiddleware(w), func(c *gin.Context) {
		h := plugin.PurchasePluginPackHandler{}
		h.HandleRequest(c, w)
	})

	pluginGroup.POST("/validate", AuthMiddleware(w), func(c *gin.Context) {
		h := plugin.ValidatePluginHandler{}
		h.HandleRequest(c)
	})

	userGroup.POST("/create", func(c *gin.Context) {
		h := cognito.CreateUserRequestHandler{}
		h.HandleRequest(c, ctx, w)
	})

	userGroup.GET("/", AuthMiddleware(w), func(c *gin.Context) {
		h := cognito.AuthHandler{}
		h.HandleRequest(c)
	})

	userGroup.POST("/jagex/link", AuthMiddleware(w), func(c *gin.Context) {
		h := cognito.JagexLinkHandler{}
		h.HandleRequest(c, w)
	})

	return r
}
