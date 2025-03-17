package plugin

import (
	"context"
	"encoding/json"
	"github.com/gin-gonic/gin"
	log "github.com/sirupsen/logrus"
	"io"
	"kraken-api/src/model"
	"kraken-api/src/service"
	"kraken-api/src/util"
	"net/http"
	"time"
)

type ValidatePluginHandler struct{}

type ValidatePluginRequest struct {
	Plugins    map[string]string `json:"plugins"`
	HardwareID string            `json:"hardwareId"`
}

// HandleRequest Validates that the plugin license keys's derived from the licenseKey field on a plugin's configuration
// match what was generated when the plugin was purchased.
func (v *ValidatePluginHandler) HandleRequest(c *gin.Context, ctx context.Context, w *service.Wrapper) {
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

	log.Infof("req body plugins: %v", reqBody.Plugins)

	var validPlugins map[string]string
	for _, plugin := range user.Plugins {
		log.Infof("validating plugin: %s", plugin.Name)
		licenseKeyToValidate, exists := reqBody.Plugins[plugin.Name]
		if !exists {
			log.Errorf("plugin %s owned by user but not found in request", plugin.Name)
			continue
		}

		if plugin.LicenseKey != licenseKeyToValidate {
			log.Errorf("plugin %s has an invalid license key: %s, valid license key is: %s", plugin.Name, licenseKeyToValidate, plugin.LicenseKey)
			continue
		}

		validPlugins[plugin.Name] = plugin.ExpirationTimestamp.Format(time.RFC3339)
	}

	log.Infof("%d/%d of the plugins for user: %s are valid", len(validPlugins), len(reqBody.Plugins), user.DiscordUsername)
	c.JSON(http.StatusOK, validPlugins)
}
