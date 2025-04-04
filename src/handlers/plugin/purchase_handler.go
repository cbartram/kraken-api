package plugin

import (
	"encoding/json"
	"errors"
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

	tmp, exists := c.Get("user")
	if !exists {
		log.Errorf("user not found in context")
		c.JSON(http.StatusUnauthorized, gin.H{"error": "user not found in context"})
		return
	}

	user := tmp.(*model.User)

	tx := w.Database.Begin()
	if tx.Error != nil {
		log.Errorf("failed to begin transaction: %s", tx.Error.Error())
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to begin db transaction"})
		return
	}

	purchaseCtx := NewPurchaseContext(user, tx, w)
	plugin, status, err := purchaseCtx.PurchasePlugin(&reqBody)

	if err != nil {
		tx.Rollback()
		c.JSON(status, gin.H{"error": "Something went wrong purchasing: " + reqBody.PluginName, "message": err.Error()})
		return
	}

	if err := tx.Commit().Error; err != nil {
		log.Errorf("failed to commit transaction: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to complete purchase", "message": err.Error()})
		return
	}

	log.Infof("plugin: %s purchase for user: %s was successful", plugin.Name, user.DiscordUsername)

	c.JSON(http.StatusOK, gin.H{
		"licenseKey":          plugin.LicenseKey,
		"pluginName":          plugin.Name,
		"expirationTimestamp": plugin.ExpirationTimestamp.Format(time.RFC3339),
	})
}

// PurchaseContext holds the state for a plugin purchase operation
type PurchaseContext struct {
	User    *model.User
	DB      *gorm.DB
	Wrapper *service.Wrapper
}

// NewPurchaseContext creates a new purchase context
func NewPurchaseContext(user *model.User, tx *gorm.DB, w *service.Wrapper) *PurchaseContext {
	return &PurchaseContext{
		User:    user,
		DB:      tx,
		Wrapper: w,
	}
}

// PurchasePlugin purchases a single plugin
func (ctx *PurchaseContext) PurchasePlugin(purchaseReq *model.PurchasePluginRequest) (*model.Plugin, int, error) {
	price, err := ctx.Wrapper.PluginStore.GetPrice(purchaseReq.PluginName, service.Period(purchaseReq.PurchaseDuration))
	if err != nil {
		log.Errorf("could not get plugin from store: %s", err)
		return nil, http.StatusBadRequest, err
	}

	purchaseDurationDays := util.PurchaseDurationToDays(purchaseReq.PurchaseDuration)

	if ctx.User.Tokens < int64(price) {
		err := fmt.Sprintf("user does not have enough tokens to purchase: %s, has tokens: %d, needs tokens: %d",
			purchaseReq.PluginName, ctx.User.Tokens, price)
		log.Error(err)
		return nil, http.StatusBadRequest, errors.New(err)
	}

	if ctx.User.InFreeTrialPeriod() {
		msg := fmt.Sprintf("user: %s cannot purchase plugins during their free trial period", ctx.User.DiscordUsername)
		log.Errorf(msg)
		return nil, http.StatusBadRequest, errors.New(msg)
	}

	// Validate plugin name
	exists, _, err := ctx.Wrapper.S3Service.GetLatestVersion(fmt.Sprintf("plugins/%s", purchaseReq.PluginName))
	if err != nil || !exists {
		msg := fmt.Sprintf("failed to list plugin objects in s3 bucket or plugin does not exist: object exists: %v, error: %s",
			exists, err)
		log.Errorf(msg)
		return nil, http.StatusBadRequest, errors.New(msg)
	}

	// Reduce user tokens
	ctx.User.Tokens -= int64(price)

	// Save user tokens to DB within the transaction
	if err := ctx.DB.Save(ctx.User).Error; err != nil {
		log.Errorf("failed to save user tokens to db: %s", err)
		return nil, http.StatusInternalServerError, errors.New("failed to save user tokens to db")
	}

	// Check if user already owns this plugin
	for i, ownedPlugin := range ctx.User.Plugins {
		if strings.ToLower(ctx.User.Plugins[i].Name) == strings.ToLower(purchaseReq.PluginName) {
			log.Infof("plugin already exists: %s and expires: %s, is expired: %v",
				ctx.User.Plugins[i].Name,
				ctx.User.Plugins[i].ExpirationTimestamp.Format(time.RFC3339),
				util.IsPluginExpired(ctx.User.Plugins[i].ExpirationTimestamp))

			if !util.IsPluginExpired(ctx.User.Plugins[i].ExpirationTimestamp) {
				msg := fmt.Sprintf("user: %s attempted to purchase plugin: %s, but plugin is already owned and not expired",
					ctx.User.DiscordUsername, purchaseReq.PluginName)
				log.Errorf(msg)
				return nil, http.StatusBadRequest, errors.New("user already owns non-expired plugin: " + purchaseReq.PluginName)
			} else {
				// User is renewing the plugin. We do not generate a new license key for a plugin renewal. This may change in the future
				// but for now it ensures users don't have to change anything in their plugin configuration when they renew.
				ctx.User.Plugins[i].ExpirationTimestamp = time.Now().AddDate(0, 0, purchaseDurationDays)
				ctx.User.Plugins[i].UpdatedAt = time.Now()
				log.Infof("user: %s is renewing plugin: %s, expiration time: %s, license (existing): %s",
					ctx.User.DiscordUsername,
					purchaseReq.PluginName,
					ctx.User.Plugins[i].ExpirationTimestamp,
					ctx.User.Plugins[i].LicenseKey)

				if err := ctx.DB.Save(&ctx.User.Plugins[i]).Error; err != nil {
					log.Errorf("failed to save plugin to db: %v", err)
					return nil, http.StatusInternalServerError, errors.Join(errors.New("failed to save plugin to db"), err)
				}

				return &ownedPlugin, http.StatusOK, nil
			}
		}
	}

	// User's first time purchasing the plugin
	licenseKey, err := util.GenerateLicenseKey()
	if err != nil {
		log.Errorf("failed to generate license key: %v", err)
		return nil, http.StatusInternalServerError, errors.Join(errors.New("failed to generate a license key"), err)
	}

	expirationTime := time.Now().AddDate(0, 0, purchaseDurationDays)
	log.Infof("plugin has been purchased for: %d days and will expire at: %s",
		purchaseDurationDays, expirationTime.Format(time.RFC3339))

	plugin := model.Plugin{
		UserID:              ctx.User.ID,
		Name:                purchaseReq.PluginName,
		ExpirationTimestamp: expirationTime,
		S3JarFilePath:       fmt.Sprintf("s3://kraken-plugins/plugins/%s", purchaseReq.PluginName),
		LicenseKey:          licenseKey,
		CreatedAt:           time.Now(),
		UpdatedAt:           time.Now(),
		DeletedAt:           gorm.DeletedAt{},
		TrialPlugin:         false,
		User:                *ctx.User,
	}

	if err := ctx.DB.Create(&plugin).Error; err != nil {
		log.Errorf("error: failed to save plugin to db: %s", err)
		return nil, http.StatusInternalServerError, errors.New("failed to save plugin to db")
	}

	return &plugin, http.StatusOK, nil
}
