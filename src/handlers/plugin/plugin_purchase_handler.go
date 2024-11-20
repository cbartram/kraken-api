package plugin

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider/types"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"kraken-api/src/util"
	"net/http"
	"slices"
	"strings"
	"time"
)

type PurchaseHandler struct{}

const (
	PURCHASED_PLUGINS_KEY    = "custom:purchased_plugins"
	EXPIRATION_TIMESTAMP_KEY = "custom:expiration_timestamp"
	PURCHASE_TIMESTAMP_KEY   = "custom:purchase_timestamp"
	LICENSE_KEY              = "custom:license_key"
	HARDWARE_ID_KEY          = "custom:hardware_id"
)

// HandleRequest Handles the /api/v1/plugin/purchase API route.
func (p *PurchaseHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody model.PurchasePluginRequest
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	// Validate plugin name. Request body will send something like:
	// "Alchemical-Hydra". We check s3 for a file that starts with the prefix: "Alchemical-Hydra"
	// if found we return the full name without .jar i.e "Alchemical-Hydra-1.0.0-all". This way plugin developers
	// are free to update versions without impacting how plugins are purchased.
	// When presigned URL's are generated the plugin name fetched from cognito user attributes will be "Alchemical-Hydra" it follows the
	// same process of finding the true object name from S3 to generate the signed URL for.
	s3, err := service.MakeS3Service("kraken-plugins")
	if err != nil {
		log.Errorf("error: failed to create service: %s", err.Error())
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to create service: " + err.Error()})
		return
	}

	exists, objectName, err := s3.GetLatestVersion(fmt.Sprintf("plugins/%s", reqBody.PluginName))
	if err != nil || !exists {
		log.Errorf("error: failed to list objects in s3 bucket or object does not exist: object exists: %v, error: %s,", exists, err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "failed to list objects in s3 bucket or object does not exist: " + err.Error()})
		return
	}

	// Check if the user has previously purchased this plugin.
	cognitoService := service.MakeCognitoService()
	attributes, err := cognitoService.GetUserAttributes(ctx, &reqBody.Credentials.AccessToken)
	if err != nil {
		log.Errorf("error: failed to create cognito service: %s", err.Error())
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to create service: " + err.Error()})
		return
	}

	writableAttributes := make([]types.AttributeType, 0)

	pluginKeys := util.GetUserAttribute(attributes, PURCHASED_PLUGINS_KEY)
	expirationTimestamps := util.GetUserAttribute(attributes, EXPIRATION_TIMESTAMP_KEY)
	purchaseDates := util.GetUserAttribute(attributes, PURCHASE_TIMESTAMP_KEY)
	licenseKeys := util.GetUserAttribute(attributes, LICENSE_KEY)

	log.Infof("User attributes: custom:purchased_plugins=%s, custom:expiration_timestamp=%s, custom:purchase_timestamp=%s", pluginKeys, expirationTimestamps, purchaseDates)

	purchaseTime := time.Now()
	expirationTime := purchaseTime.AddDate(0, 0, reqBody.PurchaseDurationDays).Format(time.RFC3339)
	licenseKey, err := util.GenerateLicenseKey()
	if err != nil {
		log.Errorf("error: failed to generate license key: %s", err.Error())
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to generate license key: " + err.Error()})
		return
	}

	// Purchased plugin properties are initially set to: "nil". This value tracks if we can just remove the "nil"
	// and replace it with the purchased plugin. i.e. this is the users first plugin purchase
	if pluginKeys[0] == "nil" {
		log.Infof("list of plugins is nil: first ever purchase, overwriting plugin list with: %s", objectName)

		// Note: we store the plugin name: "Alchemical-Hydra" NOT the jar file name: "Alchemical-Hydra-1.0.0-all.jar"
		updatedPlugins := util.MakeAttribute(PURCHASED_PLUGINS_KEY, reqBody.PluginName)
		updatedExpiration := util.MakeAttribute(EXPIRATION_TIMESTAMP_KEY, expirationTime)
		updatedPurchaseDate := util.MakeAttribute(PURCHASE_TIMESTAMP_KEY, purchaseTime.Format(time.RFC3339))
		updatedLicenseKey := util.MakeAttribute(LICENSE_KEY, licenseKey)

		writableAttributes = append(writableAttributes, updatedPlugins, updatedExpiration, updatedPurchaseDate, updatedLicenseKey)
		err = cognitoService.UpdateUserAttributes(ctx, &reqBody.Credentials.AccessToken, writableAttributes)
		if err != nil {
			log.Errorf("error: failed to update user attributes for first time plugin purchase: %s", err.Error())
			c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to update user attributes: " + err.Error()})
			return
		}
		c.JSON(http.StatusOK, gin.H{
			"licenseKey":          licenseKey,
			"pluginName":          reqBody.PluginName,
			"expirationTimestamp": expirationTime,
		})
		return
	}

	// Tracks if we should extend the plugin duration i.e they have purchased this plugin before
	// or if we should just add the plugin to their list of purchased plugins
	if slices.Contains(pluginKeys, reqBody.PluginName) {
		log.Infof("user is renewing plugin: %s", reqBody.PluginName)
		for i, pluginKey := range pluginKeys {
			if pluginKey == reqBody.PluginName {
				// The user previously purchased this plugin. We only need to update the expiration timestamp at index i.
				expirationTimestamps[i] = time.Now().AddDate(0, 0, reqBody.PurchaseDurationDays).Format(time.RFC3339)
				purchaseDates[i] = time.Now().Format(time.RFC3339)
				licenseKeys[i] = licenseKey
			}
		}

		// Join the expiration and purchase date back into csv strings and make them into cognito Attributes
		updatedExpiration := util.MakeAttribute(EXPIRATION_TIMESTAMP_KEY, strings.Join(expirationTimestamps, ","))
		updatedPurchase := util.MakeAttribute(PURCHASE_TIMESTAMP_KEY, strings.Join(purchaseDates, ","))
		updatedLicense := util.MakeAttribute(LICENSE_KEY, strings.Join(licenseKeys, ","))
		writableAttributes = append(writableAttributes, updatedExpiration, updatedPurchase, updatedLicense)

		// TODO This code can be Dry'd up substantially
		err = cognitoService.UpdateUserAttributes(ctx, &reqBody.Credentials.AccessToken, writableAttributes)
		if err != nil {
			log.Errorf("error: failed to update user attributes for first time plugin purchase: %s", err.Error())
			c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to update user attributes: " + err.Error()})
			return
		}
		c.JSON(http.StatusOK, gin.H{
			"licenseKey":          licenseKey,
			"pluginName":          reqBody.PluginName,
			"expirationTimestamp": expirationTime,
		})
		return
	}

	// Final scenario: user has purchased previous plugins but this is a new one for them
	log.Infof("user has purchased other plugins but first time for: %s", reqBody.PluginName)
	pluginKeys = append(pluginKeys, reqBody.PluginName)
	expirationTimestamps = append(expirationTimestamps, expirationTime)
	purchaseDates = append(purchaseDates, purchaseTime.Format(time.RFC3339))
	licenseKeys = append(licenseKeys, licenseKey)

	updatedLicenseKeys := util.MakeAttribute(LICENSE_KEY, strings.Join(licenseKeys, ","))
	updatedPluginKeys := util.MakeAttribute(PURCHASED_PLUGINS_KEY, strings.Join(pluginKeys, ","))
	updatedExpirationTimestamps := util.MakeAttribute(EXPIRATION_TIMESTAMP_KEY, strings.Join(expirationTimestamps, ","))
	updatedPurchaseDates := util.MakeAttribute(PURCHASE_TIMESTAMP_KEY, strings.Join(purchaseDates, ","))

	writableAttributes = append(writableAttributes, updatedLicenseKeys, updatedPurchaseDates, updatedPluginKeys, updatedExpirationTimestamps)

	err = cognitoService.UpdateUserAttributes(ctx, &reqBody.Credentials.AccessToken, writableAttributes)
	if err != nil {
		log.Errorf("error: failed to update user attributes for first time plugin purchase: %s", err.Error())
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to update user attributes: " + err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{
		"licenseKey":          licenseKey,
		"pluginName":          reqBody.PluginName,
		"expirationTimestamp": expirationTime,
	})

}
