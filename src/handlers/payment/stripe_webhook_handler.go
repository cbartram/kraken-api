package payment

import (
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/stripe/stripe-go/v82"
	"github.com/stripe/stripe-go/v82/webhook"
	"go.uber.org/zap"
	"gorm.io/gorm"
	"io"
	"kraken-api/src/cache"
	"kraken-api/src/handlers"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

type WebhookHandler struct{}

func ConsumeMessageWithDelay(message service.Message, db *gorm.DB, cache *cache.RedisCache, log *zap.SugaredLogger) {
	log.Infof("processing rabbitmq message type: %s", message.Type)

	switch message.Type {
	case "payment_intent.payment_failed":
		log.Infof("%s: customer failed to make payment", message.Type)
		return
	case "payment_intent.succeeded":
		log.Infof("%s: customer payment success", message.Type)
		if err := handlePaymentIntentSucceeded(message, db, cache, log); err != nil {
			log.Errorf("failed to handle payment intent succeeded: %v", err)
		}
	default:
		log.Infof("unhandled message type: %s", message.Type)
	}
}

func handlePaymentIntentSucceeded(message service.Message, db *gorm.DB, cache *cache.RedisCache, log *zap.SugaredLogger) error {
	var payment stripe.PaymentIntent
	if err := json.Unmarshal(message.Body, &payment); err != nil {
		return fmt.Errorf("error parsing webhook json: %w", err)
	}

	customerId := payment.Metadata["customer_id"]
	tokens := payment.Metadata["tokens"]

	if customerId == "" {
		return fmt.Errorf("customer_id not found in payment metadata")
	}
	if tokens == "" {
		return fmt.Errorf("tokens not found in payment metadata")
	}

	log.Infof("payment intent - customer id: %s, tokens: %s", customerId, tokens)

	// Find user by customer ID
	var user model.User
	if err := db.Model(&model.User{}).Where("customer_id = ?", customerId).First(&user).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return fmt.Errorf("user not found with customer id: %s", customerId)
		}
		return fmt.Errorf("failed to find user with customer id %s: %w", customerId, err)
	}

	// Parse tokens
	validTokens, err := strconv.Atoi(tokens)
	if err != nil {
		return fmt.Errorf("error parsing tokens '%s': %w", tokens, err)
	}

	if validTokens <= 0 {
		return fmt.Errorf("invalid token amount: %d", validTokens)
	}

	log.Infof("user: %s, current tokens: %d, purchased tokens: %d, new tokens: %d", user.DiscordUsername, user.Tokens, validTokens, user.Tokens+int64(validTokens))

	// Update user tokens
	user.Tokens = user.Tokens + int64(validTokens)

	if err := db.Save(&user).Error; err != nil {
		return fmt.Errorf("failed to update user with stripe payment id %s: %w", payment.ID, err)
	}

	err = cache.Invalidate(fmt.Sprintf("user:discord:%s", user.DiscordID))
	if err != nil {
		log.Errorf("failed to invalidate cache for user %s: %v", user.DiscordUsername, err)
	}

	log.Infof("payment updated for user %s, id: %s, status: %s, tokens added: %d",
		user.DiscordUsername, payment.ID, payment.Status, validTokens)

	return nil
}

func (w *WebhookHandler) HandleRequest(c *gin.Context, wrapper *service.Wrapper) {
	log := handlers.GetLoggerWithTrace(c, wrapper.Logger)

	// Check if RabbitMQ service is healthy
	if !wrapper.RabbitMqService.IsHealthy() {
		log.Error("RabbitMQ service is not healthy")
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": "message queue service unavailable"})
		return
	}

	payload, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "could not read request body"})
		return
	}

	endpointSecret := strings.TrimSpace(os.Getenv("STRIPE_ENDPOINT_SECRET"))
	if endpointSecret == "" {
		log.Error("STRIPE_ENDPOINT_SECRET environment variable not set")
		c.JSON(http.StatusInternalServerError, gin.H{"error": "server configuration error"})
		return
	}

	signatureHeader := c.GetHeader("Stripe-Signature")
	if signatureHeader == "" {
		log.Error("missing Stripe-Signature header")
		c.JSON(http.StatusBadRequest, gin.H{"error": "missing signature header"})
		return
	}

	event, err := webhook.ConstructEvent(payload, signatureHeader, endpointSecret)
	if err != nil {
		log.Errorf("webhook signature verification failed: %v", err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "signature verification failed"})
		return
	}

	eventType := string(event.Type)
	log.Infof("received webhook event: %s", eventType)

	// Handle supported events
	supportedEvents := map[string]bool{
		"payment_intent.succeeded":      true,
		"payment_intent.payment_failed": true,
	}

	if !supportedEvents[eventType] {
		log.Infof("ignoring unsupported event type: %s", eventType)
		c.JSON(http.StatusOK, gin.H{"message": "event ignored"})
		return
	}

	// Attempt to enqueue message with retry
	message := &service.Message{
		Type: eventType,
		Body: event.Data.Raw,
	}

	maxRetries := 3
	var lastErr error

	for attempt := 1; attempt <= maxRetries; attempt++ {
		log.Infof("enqueueing message with type: %s (attempt %d/%d)", eventType, attempt, maxRetries)

		err = wrapper.RabbitMqService.PublishMessage(message)
		if err == nil {
			log.Infof("successfully enqueued message with type: %s", eventType)
			c.JSON(http.StatusOK, gin.H{"message": "webhook processed successfully"})
			return
		}

		lastErr = err
		log.Errorf("failed to enqueue message with type: %s (attempt %d/%d), error: %v",
			eventType, attempt, maxRetries, err)

		if attempt < maxRetries {
			// Wait before retry with exponential backoff
			waitTime := time.Duration(attempt) * time.Second
			time.Sleep(waitTime)
		}
	}

	// All retries failed
	log.Errorf("failed to enqueue message with type: %s after %d attempts, last error: %v",
		eventType, maxRetries, lastErr)
	c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to process webhook"})
}
