package payment

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/stripe/stripe-go/v82"
	"github.com/stripe/stripe-go/v82/checkout/session"
	"github.com/stripe/stripe-go/v82/price"
	"kraken-api/src/handlers"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"kraken-api/src/util"
	"net/http"
)

type CheckoutSessionHandler struct{}

func (h *CheckoutSessionHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
	log := handlers.GetLoggerWithTrace(c, w.Logger)
	lookupKey, ok := c.GetQuery("key")
	var foundPrice *stripe.Price

	if !ok {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "you must provide a \"key\" in the query parameters.",
		})
		return
	}

	tmp, exists := c.Get("user")
	if !exists {
		log.Errorf("user not found in context")
		c.JSON(http.StatusInternalServerError, gin.H{"error": "user not found in context"})
		return
	}

	user := tmp.(*model.User)

	i := price.List(&stripe.PriceListParams{
		LookupKeys: stripe.StringSlice([]string{
			lookupKey,
		}),
	})

	for i.Next() {
		p := i.Price()
		foundPrice = p
	}

	if foundPrice == nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": fmt.Sprintf("no price found for key: %s", lookupKey),
		})
		return
	}

	log.Infof("found price: %v for lookup key: %s", foundPrice, lookupKey)

	host := util.GetHostname()
	params := &stripe.CheckoutSessionParams{
		PaymentIntentData: &stripe.CheckoutSessionPaymentIntentDataParams{
			Metadata: map[string]string{
				"customer_id": user.CustomerId,
				"tokens":      foundPrice.LookupKey[13:], // trims the kraken_token_ prefix from the lookup key to get just the tokens
			},
		},
		LineItems: []*stripe.CheckoutSessionLineItemParams{{
			Price:    stripe.String(foundPrice.ID),
			Quantity: stripe.Int64(1),
		},
		},
		Customer:          stripe.String(user.CustomerId), // The stripe user is created when the user is created
		ClientReferenceID: stripe.String(user.DiscordID),
		Mode:              stripe.String(string(stripe.CheckoutSessionModePayment)),
		SuccessURL:        stripe.String(host + "/processing?success=true&session_id={CHECKOUT_SESSION_ID}"),
		CancelURL:         stripe.String(host + "/processing?canceled=true"),
		AutomaticTax:      &stripe.CheckoutSessionAutomaticTaxParams{Enabled: stripe.Bool(false)},
		Metadata: map[string]string{
			"discordId": user.DiscordID,
		},
	}

	s, err := session.New(params)

	if err != nil {
		log.Errorf("failed to create new stripe_handlers session: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": fmt.Sprintf("failed to create new checkout session: %v", err),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"url": s.URL,
	})
}
