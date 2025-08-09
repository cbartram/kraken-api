package src

import (
	"context"
	"kraken-api/src/handlers"
	"kraken-api/src/handlers/cognito"
	"kraken-api/src/handlers/discord"
	"kraken-api/src/handlers/payment"
	"kraken-api/src/handlers/plugin"
	"kraken-api/src/handlers/sale"
	"kraken-api/src/service"
	"net/http"
	"os"

	"github.com/gin-gonic/gin"
)

func NewRouter(w *service.Wrapper) *gin.Engine {
	ctx := context.Background()

	r := gin.New()
	gin.SetMode(gin.ReleaseMode)

	r.Use(handlers.CORSMiddleware(), handlers.TraceIDMiddleware(), handlers.LoggingMiddlewareWithTrace(w.Logger))

	apiGroup := r.Group("/api/v1")
	saleGroup := apiGroup.Group("/sale")
	userGroup := apiGroup.Group("/user")
	pluginGroup := apiGroup.Group("/plugin")
	stripeGroup := apiGroup.Group("/stripe")
	discordGroup := apiGroup.Group("/discord")

	apiGroup.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"api-version": os.Getenv("API_VERSION"),
		})
	})

	saleGroup.POST("/create", handlers.AuthMiddleware(w, false), func(c *gin.Context) {
		h := sale.CreateSaleHandler{}
		h.HandleRequest(c, w)
	})

	saleGroup.GET("/", func(c *gin.Context) {
		h := sale.GetSaleHandler{}
		h.HandleRequest(c, w)
	})

	discordGroup.POST("/oauth", func(c *gin.Context) {
		h := discord.DiscordRequestHandler{}
		h.HandleRequest(c, w)
	})

	discordGroup.POST("/create-ticket", handlers.AuthMiddleware(w, false), func(c *gin.Context) {
		log := handlers.GetLoggerWithTrace(c, w.Logger)
		h := discord.TicketHandler{
			Log:   log,
			Token: os.Getenv("DISCORD_BOT_TOKEN"),
		}
		h.HandleRequest(c)
	})

	discordGroup.DELETE("/close-ticket", handlers.AuthMiddleware(w, false), func(c *gin.Context) {
		log := handlers.GetLoggerWithTrace(c, w.Logger)
		h := discord.TicketHandler{
			Log:   log,
			Token: os.Getenv("DISCORD_BOT_TOKEN"),
		}
		h.HandleCloseTicket(c, w)
	})

	apiGroup.GET("/client-bootstrap", func(c *gin.Context) {
		h := handlers.ClientBootstrapHandler{}
		h.HandleRequest(c, ctx, w)
	})

	apiGroup.POST("/support/send-message", handlers.AuthMiddleware(w, false), func(c *gin.Context) {
		h := handlers.EmailHandler{}
		h.HandleRequest(c, w)
	})

	stripeGroup.GET("/checkout-session", handlers.AuthMiddleware(w, false), func(c *gin.Context) {
		h := payment.CheckoutSessionHandler{}
		h.HandleRequest(c, w)
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

	pluginGroup.POST("/free-trial", handlers.AuthMiddleware(w, true), func(c *gin.Context) {
		h := plugin.FreeTrialHandler{}
		h.HandleRequest(c, w)
	})

	pluginGroup.POST("/create-presigned-url", handlers.AuthMiddleware(w, true), func(c *gin.Context) {
		h := plugin.PresignedUrlHandler{}
		h.HandleRequest(c, ctx, w)
	})

	pluginGroup.POST("/purchase", handlers.AuthMiddleware(w, false), func(c *gin.Context) {
		h := plugin.PurchaseHandler{}
		h.HandleRequest(c, w)
	})

	pluginGroup.POST("/purchase-pack", handlers.AuthMiddleware(w, false), func(c *gin.Context) {
		h := plugin.PurchasePluginPackHandler{}
		h.HandleRequest(c, w)
	})

	pluginGroup.POST("/validate", handlers.AuthMiddleware(w, true), func(c *gin.Context) {
		h := plugin.ValidatePluginHandler{}
		h.HandleRequest(c, w)
	})

	userGroup.POST("/create", func(c *gin.Context) {
		h := cognito.CreateUserRequestHandler{}
		h.HandleRequest(c, ctx, w)
	})

	userGroup.GET("/", handlers.AuthMiddleware(w, false), func(c *gin.Context) {
		h := cognito.AuthHandler{}
		h.HandleRequest(c, w)
	})

	userGroup.POST("/jagex/link", handlers.AuthMiddleware(w, false), func(c *gin.Context) {
		h := cognito.JagexLinkHandler{}
		h.HandleRequest(c, w)
	})

	return r
}
