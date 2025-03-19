package plugin

import (
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"gorm.io/gorm"
	"io"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"kraken-api/src/util"
	"net/http"
	"strings"
	"time"
)

type PurchaseHandler struct{}

var pluginStore = NewPluginStore()

// HandleRequest Handles the /api/v1/plugin/purchase API route.
func (p *PurchaseHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody model.PurchasePluginRequest
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	price, err := pluginStore.GetPrice(reqBody.PluginName, Period(reqBody.PurchaseDuration))
	if err != nil {
		log.Errorf("could not get plugin from store: %s", err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "could not get plugin from store: " + err.Error()})
		return
	}

	purchaseDurationDays := util.PurchaseDurationToDays(reqBody.PurchaseDuration)

	tmp, exists := c.Get("user")
	if !exists {
		log.Errorf("user not found in context")
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not found in context"})
		return
	}

	user := tmp.(*model.User)
	if user.Tokens < int64(price) {
		err := fmt.Sprintf("user does not have enough tokens to purchase: %s, has tokens: %d, needs tokens: %d", reqBody.PluginName, user.Tokens, price)
		log.Error(err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "Not enough tokens", "message": err})
		return
	}

	user.Tokens -= int64(price)

	// Validate plugin name. Request body will send something like:
	// "Alchemical-Hydra". We check s3 for a file that starts with the prefix: "Alchemical-Hydra"
	// if found we return the full name without .jar i.e "Alchemical-Hydra-1.0.0-all". This way, plugin developers
	// are free to update versions without impacting how plugins are purchased.
	exists, _, err = w.S3Service.GetLatestVersion(fmt.Sprintf("plugins/%s", reqBody.PluginName))
	if err != nil || !exists {
		log.Errorf("error: failed to list plugin objects in s3 bucket or plugin does not exist: object exists: %v, error: %s,", exists, err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "failed to list plugins or plugin does not exist"})
		return
	}

	for _, ownedPlugin := range user.Plugins {
		if strings.ToLower(ownedPlugin.Name) == strings.ToLower(reqBody.PluginName) {
			log.Infof("plugin already exists: %s and expires: %s, is expired: %v", ownedPlugin.Name, ownedPlugin.ExpirationTimestamp.Format(time.RFC3339), util.IsPluginExpired(ownedPlugin.ExpirationTimestamp))
			if !util.IsPluginExpired(ownedPlugin.ExpirationTimestamp) {
				log.Infof("user: %s attempted to purchase plugin: %s, but plugin is already owned and not expired: ", user.DiscordUsername, reqBody.PluginName)
				c.JSON(http.StatusBadRequest, gin.H{"error": "user already owns plugin (not expired): " + reqBody.PluginName})
				return
			} else {
				// User is renewing the plugin
				ownedPlugin.ExpirationTimestamp = time.Now().AddDate(0, 0, purchaseDurationDays)
				ownedPlugin.UpdatedAt = time.Now()
				licenseKey, err := util.GenerateLicenseKey()
				if err != nil {
					log.Errorf("error: failed to generate license key: %v", err)
					c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to generate license key: " + err.Error()})
					return
				}
				ownedPlugin.LicenseKey = licenseKey
				log.Infof("user: %s is renewing plugin: %s, expiration time: %s, license: %s", user.DiscordUsername, reqBody.PluginName, ownedPlugin.ExpirationTimestamp, ownedPlugin.LicenseKey)
				tx := w.Database.Save(&ownedPlugin)
				if tx.Error != nil {
					log.Errorf("error: failed to save plugin to db: %v", tx.Error)
					c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to renew plugin: " + reqBody.PluginName})
					return
				}
				c.JSON(http.StatusOK, gin.H{
					"licenseKey":          licenseKey,
					"pluginName":          reqBody.PluginName,
					"expirationTimestamp": ownedPlugin.ExpirationTimestamp.Format(time.RFC3339),
				})
				return
			}
		}
	}

	// User's first time purchasing the plugin
	licenseKey, err := util.GenerateLicenseKey()
	if err != nil {
		log.Errorf("error: failed to generate license key: %s", err.Error())
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to generate license key: " + err.Error()})
		return
	}
	plugin := model.Plugin{
		UserID:              user.ID,
		Name:                reqBody.PluginName,
		ExpirationTimestamp: time.Now().AddDate(0, 0, purchaseDurationDays),
		S3JarFilePath:       fmt.Sprintf("s3://kraken-plugins/plugins/%s", reqBody.PluginName),
		LicenseKey:          licenseKey,
		CreatedAt:           time.Now(),
		UpdatedAt:           time.Now(),
		DeletedAt:           gorm.DeletedAt{},
		User:                *user,
	}

	tx := w.Database.Save(&user)
	if tx.Error != nil {
		log.Errorf("error: failed to save user tokens to db: %s", tx.Error.Error())
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to save user tokens to db: " + reqBody.PluginName})
		return
	}

	tx = w.Database.Create(&plugin)
	if tx.Error != nil {
		log.Errorf("error: failed to save plugin to db: %s", tx.Error.Error())
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to save plugin to db: " + reqBody.PluginName})
		return
	}

	log.Infof("plugin: %s purchase for user: %s was successful", plugin.Name, user.DiscordUsername)

	c.JSON(http.StatusOK, gin.H{
		"licenseKey":          licenseKey,
		"pluginName":          reqBody.PluginName,
		"expirationTimestamp": plugin.ExpirationTimestamp.Format(time.RFC3339),
	})
}
