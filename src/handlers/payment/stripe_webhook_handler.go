package payment

import (
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"github.com/stripe/stripe-go/v81"
	"github.com/stripe/stripe-go/v81/webhook"
	"gorm.io/gorm"
	"io"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
	"os"
)

type WebhookHandler struct{}

func ConsumeMessageWithDelay(message service.Message, db *gorm.DB) {
	log.Infof("processing rabbitmq message type: %s", message.Type)

	switch message.Type {
	case "payment_intent.payment_failed":
		log.Infof("customer failed to make payment")
		return
	case "payment_intent.succeeded":
		var invoice stripe.Invoice
		err := json.Unmarshal(message.Body, &invoice)
		if err != nil {
			log.Errorf("error parsing webhook json: %v", err)
			return
		}

		var user model.User
		tx := db.Model(&model.User{}).Where("customer_id = ?", invoice.Customer.ID).First(&user)
		if tx.Error != nil {
			log.Errorf("failed to find user with customer id: %s, error: %v", invoice.Customer.ID, err)
			return
		}

		for _, lineItem := range invoice.Lines.Data {
			if lineItem.Price != nil {
				log.Infof("line item price lookup key: %s", lineItem.Price.LookupKey)
			}
		}
		// TODO Update user with additional kraken tokens

		tx = db.Save(&user)
		if tx.Error != nil {
			log.Errorf("failed to update user with stripe invoice id: %s, error: %v", invoice.ID, err)
			return
		}

		log.Infof("invoice updated for user %s, id: %s, status: %s", user.DiscordUsername, invoice.ID, invoice.Status)
	}
}

func (w *WebhookHandler) HandleRequest(c *gin.Context, wrapper *service.Wrapper) {
	payload, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	endpointSecret := os.Getenv("STRIPE_ENDPOINT_SECRET")
	signatureHeader := c.GetHeader("Stripe-Signature")
	event, err := webhook.ConstructEvent(payload, signatureHeader, endpointSecret)
	if err != nil {
		log.Errorf("webhook signature verification failed: %v", err)
		c.JSON(http.StatusBadRequest, gin.H{"error": fmt.Sprintf("Webhook signature verification failed: %v", err)})
		return
	}

	eventType := string(event.Type)

	// Note: these cases are actually ordered in the timeline that webhook events are received
	switch eventType {
	case
		"charge.failed",
		"charge.succeeded",
		"payment_intent.succeeded",
		"checkout.session.completed",
		"invoice.created",
		"payment_intent.payment_failed",
		"invoice.paid":
		log.Infof("enqueueing message with type: %s", eventType)
		err = wrapper.RabbitMqService.PublishMessage(&service.Message{
			Type: eventType,
			Body: event.Data.Raw,
		})

		if err != nil {
			log.Errorf("failed to enqueue message with type: %s, error: %v", eventType, err)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to enqueue message"})
		}
	}

	c.JSON(http.StatusOK, gin.H{"message": "OK"})
}
