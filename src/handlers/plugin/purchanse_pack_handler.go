package plugin

import (
	"encoding/json"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"net/http"
	"time"
)

type PurchasePluginPackHandler struct{}

func (plugin *PurchasePluginPackHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
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

	// PluginName in this case is the Plugin Pack Name
	plugins, err := w.PluginStore.GetPluginsInPack(reqBody.PluginName)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Something went wrong", "message": "could not find plugins in pack: " + err.Error()})
	}

	tx := w.Database.Begin()
	if tx.Error != nil {
		log.Errorf("failed to begin transaction: %s", tx.Error.Error())
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to begin transaction"})
		return
	}

	purchaseCtx := NewPurchaseContext(user, tx, w)
	results := make([]gin.H, 0, len(plugins))
	hasErrors := false

	for _, plugin := range plugins {
		p, status, err := purchaseCtx.PurchasePlugin(&model.PurchasePluginRequest{
			PluginName:       plugin.Name,
			PurchaseDuration: reqBody.PurchaseDuration,
		})

		result := gin.H{
			"pluginName": plugin.Name,
			"success":    err == nil,
		}

		if err != nil {
			hasErrors = true
			result["error"] = err.Error()
			result["status"] = status
		} else {
			result["licenseKey"] = p.LicenseKey
			result["expirationTimestamp"] = p.ExpirationTimestamp.Format(time.RFC3339)
		}

		results = append(results, result)
	}

	if hasErrors {
		tx.Rollback()
		log.Errorf("batch purchase failed, transaction rolled back")
		c.JSON(http.StatusBadRequest, gin.H{
			"error":   "One or more plugin purchases failed",
			"results": results,
		})
		return
	}

	// Commit the transaction
	if err := tx.Commit().Error; err != nil {
		log.Errorf("failed to commit transaction: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to complete purchases", "message": err.Error()})
		return
	}

	log.Infof("plugin pack purchase successful for user: %s", user.DiscordUsername)
	c.JSON(http.StatusOK, gin.H{
		"results": results,
	})
}
