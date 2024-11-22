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
)

type PluginValidateLicenseHandler struct{}

// HandleRequest Handles the /api/v1/plugin/validate-license route which validates a users license. Validating a license takes place in 3 parts:
// 1. Validate what they sent = what is on their account
// 2. Validate (once again) that the expiration timestamp for the plugin is ok
// 3. Validate that the hardware id matches what is on the users account
func (p *PluginValidateLicenseHandler) HandleRequest(c *gin.Context, ctx context.Context) {
	bodyRaw, err := io.ReadAll(c.Request.Body)
	if err != nil {
		log.Errorf("could not read body from request: %s", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "could not read body from request: " + err.Error()})
		return
	}

	var reqBody model.LicenseKeyRequest
	if err := json.Unmarshal(bodyRaw, &reqBody); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request body: " + err.Error()})
		return
	}

	// TODO Validate that the license key matches a regex.

	authManger := service.MakeCognitoService()
	log.Infof("fetching user attributes with access token")
	attr, err := authManger.GetUserAttributes(ctx, &reqBody.Credentials.AccessToken)

	licenseKeys := util.GetUserAttribute(attr, LICENSE_KEY)
	expirationTimestamps := util.GetUserAttribute(attr, EXPIRATION_TIMESTAMP_KEY)
	hardwareIds := util.GetUserAttribute(attr, HARDWARE_ID_KEY)
	pluginNames := util.GetUserAttribute(attr, PURCHASED_PLUGINS_KEY)

	plugins := map[string]string{}

	for i, name := range pluginNames {
		// Get the license key for this plugin name that was provided in the request. If this plugin name doesn't
		// exist its value will be "" which will not match the valid value anyway.
		providedLicenseKey := reqBody.Plugins[name]
		validLicenseKey := licenseKeys[i]

		// If HWID doesn't match don't bother validating the license's they are playing on the wrong computer.
		if hardwareIds[0] != reqBody.HardwareID {
			log.Infof("hardware id passed: %s does not match plugin HWID: %s", reqBody.HardwareID, hardwareIds[0])
			plugins[name] = ""
			continue
		}

		expired, err := util.IsPluginExpired(expirationTimestamps[i])
		if err != nil {
			log.Errorf("error: failed to parse plugin expiration timestamp: %s to RFC3339 format. error: %s", expirationTimestamps[i], err.Error())
			plugins[name] = ""
			continue
		}

		if expired {
			log.Infof("current time is after plugin expiration time, license key is expired.")
			plugins[name] = ""
			continue
		}

		if providedLicenseKey != validLicenseKey {
			log.Infof("provided license key: %s does not match valid license key: %s for plugin: %s", providedLicenseKey, validLicenseKey, name)
			plugins[name] = ""
			continue
		}

		plugins[name] = expirationTimestamps[i]
	}

	c.JSON(http.StatusOK, plugins)
}
