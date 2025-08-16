package plugin

import (
	"encoding/json"
	"io"
	"kraken-api/src/handlers"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"kraken-api/src/util"
	"net/http"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
)

type ValidatePluginHandler struct{}

type ValidatePluginRequest struct {
	Plugins    map[string]string `json:"plugins"`
	HardwareID string            `json:"hardwareId"`
}

// HandleRequest Validates that the plugin license keys's derived from the licenseKey field on a plugin's configuration
// match what was generated when the plugin was purchased.
func (v *ValidatePluginHandler) HandleRequest(c *gin.Context, w *service.Wrapper) {
	log := handlers.GetLoggerWithTrace(c, w.Logger)
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody ValidatePluginRequest
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

	if !util.IsValidHardwareID(reqBody.HardwareID, user.HardwareIDs) {
		log.Errorf("invalid hardware id specified: %s", reqBody.HardwareID)
		c.JSON(http.StatusUnauthorized, gin.H{"error": "invalid hardware id"})
	}

	validPlugins := make(map[string]string)

	// Special case: Socket comes pre-packaged with socket-sotetseg plugin and requires no license key to validate
	_, ok := reqBody.Plugins["Socket"]
	if ok {
		log.Infof("validation request contains: Socket plugin, auto validating")
		validPlugins["Socket"] = time.Now().AddDate(15, 0, 0).Format(time.RFC3339)
	}

	// Collect expired trial plugins for batch deletion (async)
	var expiredTrialPlugins []*model.Plugin

	for _, plugin := range user.Plugins {
		log.Infof("validating plugin: %s, user: %s, in trial period: %v, trial plugin: %v, plugin active: %v", plugin.Name, user.DiscordUsername, user.InFreeTrialPeriod(), plugin.TrialPlugin, !util.IsPluginExpired(plugin.ExpirationTimestamp))

		// Handle active trial plugins
		if user.InFreeTrialPeriod() && plugin.TrialPlugin && !util.IsPluginExpired(plugin.ExpirationTimestamp) {
			log.Infof("user: %s has a plugin: %s in free trial period.", user.DiscordUsername, plugin.Name)
			validPlugins[plugin.Name] = user.FreeTrialEndTime.Format(time.RFC3339)
			continue
		}

		// Collect expired trial plugins for cleanup but don't delete immediately
		if !user.InFreeTrialPeriod() && plugin.TrialPlugin && util.IsPluginExpired(plugin.ExpirationTimestamp) {
			log.Infof("user: %s has a trial plugin: %s that has expired, marking for cleanup", user.DiscordUsername, plugin.Name)
			expiredTrialPlugins = append(expiredTrialPlugins, &plugin)
			continue
		}

		// Validate license key for active plugins
		licenseKeyToValidate, exists := reqBody.Plugins[plugin.Name]
		if !exists {
			log.Infof("plugin %s found for user, but may be expired or mismatch between plugin name and db entry: %s", plugin.Name, plugin.ExpirationTimestamp.Format(time.RFC3339))
			continue
		}

		if plugin.LicenseKey != strings.TrimSpace(licenseKeyToValidate) {
			log.Infof("plugin %s has an invalid license key: %s, valid license key is: %s, user in free trial period: %v", plugin.Name, licenseKeyToValidate, plugin.LicenseKey, user.InFreeTrialPeriod())
			continue
		}

		validPlugins[plugin.Name] = plugin.ExpirationTimestamp.Format(time.RFC3339)
	}

	// Perform async cleanup of expired trial plugins if any exist
	if len(expiredTrialPlugins) > 0 {
		go func() {
			v.cleanupExpiredTrialPlugins(w, expiredTrialPlugins, user.DiscordUsername)
		}()
	}

	log.Infof("%d/%d of the plugins for user: %s are valid", len(validPlugins), len(reqBody.Plugins), user.DiscordUsername)
	c.JSON(http.StatusOK, validPlugins)
}

// cleanupExpiredTrialPlugins performs batch deletion of expired trial plugins asynchronously
func (v *ValidatePluginHandler) cleanupExpiredTrialPlugins(w *service.Wrapper, expiredPlugins []*model.Plugin, username string) {
	log := w.Logger
	var failedDeletions []string
	var pluginNames []string
	for _, plugin := range expiredPlugins {
		pluginNames = append(pluginNames, plugin.Name)
		if err := w.Database.Unscoped().Delete(plugin); err.Error != nil {
			log.Errorf("failed to delete expired trial plugin: %s for user: %s, error: %v", plugin.Name, username, err.Error)
			failedDeletions = append(failedDeletions, plugin.Name)
		}
	}

	if len(failedDeletions) > 0 {
		log.Errorf("failed to delete %d/%d expired trial plugins for user: %s, failed plugins: %v",
			len(failedDeletions), len(expiredPlugins), username, failedDeletions)
	} else {
		log.Infof("cleaned up %d expired trial plugins for user: %s, %v", len(expiredPlugins), username, pluginNames)
	}
}
