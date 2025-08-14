package plugin

import (
	"fmt"
	"kraken-api/src/handlers"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"kraken-api/src/util"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
)

type FreeTrialHandler struct{}

func (f *FreeTrialHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
	log := handlers.GetLoggerWithTrace(c, w.Logger)
	tmp, exists := c.Get("user")
	if !exists {
		log.Errorf("user not found in context")
		c.JSON(http.StatusUnauthorized, gin.H{"message": "user not found in context"})
		return
	}

	user := tmp.(*model.User)

	if user.UsedFreeTrial {
		log.Infof("user: %s has already consumed free trial", user.DiscordUsername)
		c.JSON(http.StatusUnauthorized, gin.H{"message": "user has already consumed free trial"})
		return
	}

	startTime := time.Now()
	endTime := time.Now().Add(168 * time.Hour)
	plugins := w.PluginStore.GetPlugins()

	tx := w.Database.Begin()
	if tx.Error != nil {
		log.Errorf("failed to begin transaction: %v", tx.Error)
		c.JSON(http.StatusInternalServerError, gin.H{"message": "database error creating transaction"})
		return
	}

	user.FreeTrialStartTime = startTime
	user.FreeTrialEndTime = endTime
	user.UsedFreeTrial = true

	if err := tx.Save(&user).Error; err != nil {
		tx.Rollback()
		log.Errorf("failed to update user free trial status: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"message": "failed to update user free trial status"})
		return
	}

	// Get user's existing plugins to avoid duplicates
	var existingPlugins []model.Plugin
	if err := tx.Where("user_id = ?", user.ID).Find(&existingPlugins).Error; err != nil {
		tx.Rollback()
		log.Errorf("failed to fetch existing plugins: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"message": "database error while finding users plugins"})
		return
	}

	// Create a map for quick lookup of existing plugins
	existingPluginMap := make(map[string]bool)
	for _, p := range existingPlugins {
		existingPluginMap[p.Name] = true
	}

	var newPlugins []model.Plugin
	for _, plugin := range plugins {
		// Skip if user already has purchased this plugin
		if existingPluginMap[plugin.Name] {
			log.Infof("[Free Trial] Skipping trial plugin %s for user %s, already owned", plugin.Name, user.DiscordUsername)
			continue
		}

		key, err := util.GenerateLicenseKey()
		if err != nil {
			log.Errorf("failed to generate license key for free trial plugin: %v", err)
			continue
		}

		newPlugins = append(newPlugins, model.Plugin{
			UserID:              user.ID,
			Name:                plugin.Name,
			ExpirationTimestamp: endTime,
			TrialPlugin:         true,
			LicenseKey:          key,
			CreatedAt:           time.Now(),
			User:                *user,
		})
		log.Infof("[Free Trial] Adding plugin %s for user %s, already owned", plugin.Name, user.DiscordUsername)
	}

	// Batch insert all new plugins in a single database call
	log.Infof("Adding %v new trial plugins for user %s", len(newPlugins), user.DiscordUsername)
	if len(newPlugins) > 0 {
		if err := tx.CreateInBatches(newPlugins, 100).Error; err != nil {
			tx.Rollback()
			log.Errorf("failed to add trial plugins: %v", err)
			c.JSON(http.StatusInternalServerError, gin.H{"message": "failed to add trial plugins to batch"})
			return
		}
	}

	if err := tx.Commit().Error; err != nil {
		log.Errorf("failed to commit transaction for free trial plugins: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"message": "database error initiating free trial"})
		return
	}

	err := w.Cache.Invalidate(fmt.Sprintf("user:discord:%s", user.DiscordID))
	if err != nil {
		log.Errorf("failed to invalidate cache for user %s: %v", user.DiscordUsername, err)
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "Free trial activated successfully",
		"expires": endTime,
	})
}
