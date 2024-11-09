package handlers

import (
	"context"
	"encoding/json"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/client"
	"kraken-api/src/model"
	"net/http"
	"strings"
)

type PluginPresignedUrlHandler struct{}

// HandleRequest Handles the /api/v1/discord-oauth route which the client calls to trade a code for an OAuth
// access token.
func (p *PluginPresignedUrlHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	// Note: Only the access token is provided in the request body. All other values will be nil
	var reqBody model.CognitoCredentials
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	authManger := client.MakeCognitoService()
	log.Infof("fetching user attributes with access token")
	attr, err := authManger.GetUserAttributes(ctx, &reqBody.AccessToken)
	if err != nil {
		log.Errorf("error: failed to get user attributes with access token, error: %s", err.Error())
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "failed to get user attributes with access token: " + err.Error(),
		})
		return
	}

	// Purchased Plugins attribute type will be "nil" (string) if no plugins have been purchased, else it will be
	// a CSV list of purchased plugin id's.
	var purchasedPlugins = "nil"
	for _, attribute := range attr {
		switch aws.ToString(attribute.Name) {
		case "custom:purchased_plugins":
			purchasedPlugins = aws.ToString(attribute.Value)
			break
		}
	}

	log.Infof("custom:purchased_plugins=%s", purchasedPlugins)
	if purchasedPlugins == "nil" {
		c.JSON(http.StatusOK, gin.H{
			"message": "user has not purchased any plugins",
			"url":     "",
		})
		return
	}

	// TODO Gen presigned URL
	pluginIds := strings.Split(purchasedPlugins, ",")
	c.JSON(http.StatusOK, gin.H{
		"pluginIds": pluginIds,
		"url":       "",
	})
}
